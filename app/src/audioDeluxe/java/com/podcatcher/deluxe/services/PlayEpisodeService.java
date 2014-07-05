/** Copyright 2012-2014 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package com.podcatcher.deluxe.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.podcatcher.deluxe.Podcatcher;
import com.podcatcher.deluxe.SettingsActivity;
import com.podcatcher.deluxe.listeners.OnChangePlaylistListener;
import com.podcatcher.deluxe.listeners.PlayServiceListener;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.types.Episode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.RemoteControlClient.PLAYSTATE_BUFFERING;
import static android.media.RemoteControlClient.PLAYSTATE_ERROR;
import static android.media.RemoteControlClient.PLAYSTATE_PAUSED;
import static android.media.RemoteControlClient.PLAYSTATE_PLAYING;
import static android.media.RemoteControlClient.PLAYSTATE_STOPPED;
import static com.podcatcher.deluxe.Podcatcher.AUTHORIZATION_KEY;

/**
 * Play an episode service, wraps media player. This class implements an Android
 * service. It can be used to play back podcast episodes and tries to hide away
 * the complexity of the media player support in Android. All methods should
 * fail gracefully. Connect (bind) to the service from your activity/fragment
 * and/or send intent actions to use it. For even more interaction, implement
 * {@link PlayServiceListener}.
 */
public class PlayEpisodeService extends Service implements OnPreparedListener,
        OnCompletionListener, OnErrorListener, OnBufferingUpdateListener,
        OnInfoListener, OnAudioFocusChangeListener, OnChangePlaylistListener {

    /**
     * Action to send to service to toggle play/pause
     */
    public static final String ACTION_TOGGLE = "com.podcatcher.deluxe.action.TOGGLE";
    /**
     * Action to send to service to play (resume) episode
     */
    public static final String ACTION_PLAY = "com.podcatcher.deluxe.action.PLAY";
    /**
     * Action to send to service to pause episode
     */
    public static final String ACTION_PAUSE = "com.podcatcher.deluxe.action.PAUSE";
    /**
     * Action to send to service to restart the current episode
     */
    public static final String ACTION_PREVIOUS = "com.podcatcher.deluxe.action.PREVIOUS";
    /**
     * Action to send to service to skip to next episode
     */
    public static final String ACTION_SKIP = "com.podcatcher.deluxe.action.SKIP";
    /**
     * Action to send to service to rewind the current episode
     */
    public static final String ACTION_REWIND = "com.podcatcher.deluxe.action.REWIND";
    /**
     * Action to send to service to fast forward the current episode
     */
    public static final String ACTION_FORWARD = "com.podcatcher.deluxe.action.FORWARD";
    /**
     * Action to send to service to stop episode
     */
    public static final String ACTION_STOP = "com.podcatcher.deluxe.action.STOP";

    /**
     * The episode manager handle
     */
    private EpisodeManager episodeManager;
    /**
     * Current episode
     */
    private Episode currentEpisode;
    /**
     * Our MediaPlayer handle
     */
    private MediaPlayer player;
    /**
     * Is the player prepared ?
     */
    private boolean prepared = false;
    /**
     * Is the player currently buffering ?
     */
    private boolean buffering = false;
    /**
     * Are we bound to any activity ?
     */
    private boolean bound = false;

    /**
     * Our audio manager handle
     */
    private AudioManager audioManager;
    /**
     * Our becoming noisy broadcast receiver
     */
    private ComponentName noisyReceiver;
    /**
     * Our media button broadcast receiver
     */
    private ComponentName mediaButtonReceiver;
    /**
     * Our remote control client
     */
    private PodcatcherRCClient remoteControlClient;
    /**
     * Our wifi lock
     */
    private WifiLock wifiLock;
    /**
     * Our notification helper
     */
    private PlayEpisodeNotification notification;

    /**
     * Play update timer for notification
     */
    private Timer playUpdateTimer = new Timer();
    /**
     * Play update timer task for notification
     */
    private TimerTask playUpdateTimerTask;

    /**
     * Our notification id (does not really matter)
     */
    private static final int NOTIFICATION_ID = 123;
    /**
     * The amount of seconds used for any forward or rewind event
     */
    private static final int SKIP_AMOUNT = 10 * 1000;
    /**
     * The volume we duck playback to
     */
    private static final float DUCK_VOLUME = 0.1f;
    /**
     * Our log tag
     */
    private static final String TAG = "PlayEpisodeService";

    /**
     * The call-back set for the play service listeners
     */
    private Set<PlayServiceListener> listeners = new HashSet<>();
    /**
     * Binder given to clients
     */
    private final IBinder binder = new PlayServiceBinder();

    /**
     * The binder to return to client.
     */
    public class PlayServiceBinder extends Binder {

        /**
         * @return The service binder.
         */
        public PlayEpisodeService getService() {
            // Return this instance of this service, so clients can call public
            // methods
            return PlayEpisodeService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Get and enable broadcast receivers
        noisyReceiver = new ComponentName(this, BecomingNoisyReceiver.class);
        enableReceiver(noisyReceiver);
        mediaButtonReceiver = new ComponentName(this, MediaButtonReceiver.class);
        enableReceiver(mediaButtonReceiver);

        // Get the audio manager handle
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Create the wifi lock (not acquired yet)
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        // Get our episode manager handle
        episodeManager = EpisodeManager.getInstance();
        // We need to listen to playlist updates to update the notification
        episodeManager.addPlaylistListener(this);
        // Our notification helper
        notification = PlayEpisodeNotification.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We might have received an action to perform
        if (intent != null && intent.getAction() != null && prepared) {
            // Retrieve the action
            String action = intent.getAction();
            // Go handle the action
            if (action.equals(ACTION_TOGGLE)) {
                if (isPlaying())
                    pause();
                else
                    resume();
            } else if (action.equals(ACTION_PLAY))
                resume();
            else if (action.equals(ACTION_PAUSE))
                pause();
            else if (action.equals(ACTION_PREVIOUS)) {
                // Store the resume at value because we want to handle the case
                // where the user invokes this action accidentally.
                storeResumeAt();

                seekTo(0);
            } else if (action.equals(ACTION_SKIP)) {
                // "Skip" can mean two things here: Move ahead in the current
                // episode to the stored "resume at" value, or (if that is not
                // available or actually "behind" us) go to the next item in the
                // playlist.
                final int resumeAt = episodeManager.getResumeAt(currentEpisode);

                if (resumeAt > getCurrentPosition())
                    player.seekTo(resumeAt);
                else
                    playNext();
            } else if (action.equals(ACTION_REWIND)) {
                rewind();
            } else if (action.equals(ACTION_FORWARD)) {
                fastForward();
            } else if (action.equals(ACTION_STOP))
                reset();

            // Alert listeners so the UI can adjust
            for (PlayServiceListener listener : listeners)
                listener.onPlaybackStateChanged();
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.bound = true;

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.bound = false;

        // Since this is a started service (which is also bound to in addition)
        // we need to take care of stopping ourself. But we do not want to go
        // away if there is still some playback. Therefore we check whether
        // there is any episode loaded and only stop ourselves if there is none.
        stopSelfIfUnboundAndIdle();

        return false;
    }

    @Override
    public void onDestroy() {
        reset();

        // Unregister listener
        episodeManager.removePlaylistListener(this);
        // Stop the timer
        playUpdateTimer.cancel();

        // Disable broadcast receivers
        disableReceiver(noisyReceiver);
        disableReceiver(mediaButtonReceiver);
    }

    /**
     * Register a play service listener.
     *
     * @param listener Listener to add.
     */
    public void addPlayServiceListener(PlayServiceListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister a play service listener.
     *
     * @param listener Listener to remove.
     */
    public void removePlayServiceListener(PlayServiceListener listener) {
        listeners.remove(listener);
    }

    /**
     * Load and start playback for given episode. Will end any current playback.
     *
     * @param episode Episode to play (not <code>null</code>).
     */
    public void playEpisode(Episode episode) {
        if (episode != null) {
            // Stop and release the current player and reset variables
            reset();

            // Make the new episode our current source
            this.currentEpisode = episode;

            // Start playback for new episode
            try {
                initPlayer();

                // Play local file
                if (episodeManager.isDownloaded(episode))
                    player.setDataSource(episodeManager.getLocalPath(episode));
                    // Need to resort to remote file
                else {
                    // We add some request headers to overwrite the default user
                    // agent because this is blocked by some servers
                    final HashMap<String, String> headers = new HashMap<>(2);
                    headers.put(Podcatcher.USER_AGENT_KEY, Podcatcher.USER_AGENT_VALUE);

                    // Also set the authorization header data if needed
                    final String auth = episode.getPodcast().getAuthorization();
                    if (auth != null)
                        headers.put(AUTHORIZATION_KEY, auth);

                    // Actually set the remote source for the playback
                    player.setDataSource(this, Uri.parse(currentEpisode.getMediaUrl()), headers);

                    // We are streaming, so make wifi stay alive
                    wifiLock.acquire();
                }

                player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                player.prepareAsync(); // might take long! (for buffering, etc)
            } catch (Exception e) {
                Log.d(TAG, "Prepare/Play failed for episode: " + episode, e);
            }
        }
    }

    /**
     * Play the next episode in the playlist. Does nothing if there is none. If
     * this is called while the current episode is still set, this episode will
     * be removed from the playlist. The current episode (if set) also
     * influences the selection of the next episode: if in the playlist (e.g. as
     * item number #4) the next episode is selected relative to the current one
     * (#5). That said, playback will jump to item #1 if the current episode is
     * either not in the playlist or is at the end of the playlist.
     */
    public void playNext() {
        final List<Episode> playlist = episodeManager.getPlaylist();
        final int currentEpisodePosition = playlist.indexOf(currentEpisode);

        // Pop the episode off the playlist
        episodeManager.removeFromPlaylist(currentEpisode);
        playlist.remove(currentEpisode);

        if (!playlist.isEmpty()) {
            Episode next = playlist.get(0);

            if (currentEpisodePosition > 0 && currentEpisodePosition < playlist.size())
                next = playlist.get(currentEpisodePosition);

            playEpisode(next);
        }
    }

    @Override
    public void onPlaylistChanged() {
        // Update status bar notification
        rebuildNotification();

        // Update rc if any (e.g. lock screen)
        if (currentEpisode != null && remoteControlClient != null)
            remoteControlClient.showNext(!episodeManager.isPlaylistEmptyBesides(currentEpisode));
    }

    /**
     * Pause current playback.
     */
    public void pause() {
        if (prepared && isPlaying()) {
            player.pause();
            storeResumeAt();

            stopPlayProgressTimer();
            updateRemoteControlPlaystate(PLAYSTATE_PAUSED);
            rebuildNotification();
        }
    }

    /**
     * Resume to play current episode.
     */
    public void resume() {
        if (prepared && !isPlaying()) {
            player.start();

            startPlayProgressTimer();
            updateRemoteControlPlaystate(PLAYSTATE_PLAYING);
            rebuildNotification();
        }
    }

    /**
     * Seek player to given location in media file.
     *
     * @param msecs Milli seconds from the start to seek to.
     */
    public void seekTo(int msecs) {
        if (prepared && msecs >= 0 && msecs <= getDuration()) {
            player.seekTo(msecs);

            startForeground(NOTIFICATION_ID,
                    notification.updateProgress(getCurrentPosition(), getDuration()));
        }
    }

    /**
     * Rewind the playback 10 secs.
     */
    public void rewind() {
        final int newPosition = getCurrentPosition() - SKIP_AMOUNT;
        seekTo(newPosition <= 0 ? 0 : newPosition);
    }

    /**
     * Fast forward 10 secs.
     */
    public void fastForward() {
        final int newPosition = getCurrentPosition() + SKIP_AMOUNT;

        if (newPosition < getDuration())
            seekTo(newPosition);
    }

    /**
     * @return Whether the player is currently playing.
     */
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    /**
     * @return Whether the service is currently preparing, i.e. buffering data
     * and will start playing asap.
     */
    public boolean isPreparing() {
        return currentEpisode != null && !prepared;
    }

    /**
     * @return Whether the service is prepared, i.e. any episode is loaded.
     */
    public boolean isPrepared() {
        return prepared;
    }

    /**
     * @return Whether the service is currently buffering data.
     */
    public boolean isBuffering() {
        return buffering || isPreparing();
    }

    /**
     * Checks whether the currently loaded episode is equal to the one given.
     * The check we be true regardless of whether the episode has been actually
     * prepared or not.
     *
     * @param episode Episode to check for.
     * @return true iff given episode is loaded (or loading), false otherwise.
     */
    public boolean isLoadedEpisode(Episode episode) {
        return currentEpisode != null && currentEpisode.equals(episode);
    }

    /**
     * @return The episode currently loaded.
     */
    public Episode getCurrentEpisode() {
        return currentEpisode;
    }

    /**
     * @return Current position of playback in milli-seconds from media start.
     * Does not throw any exception but returns at least zero.
     */
    public int getCurrentPosition() {
        if (player == null || !prepared)
            return 0;
        else
            return player.getCurrentPosition();
    }

    /**
     * @return Duration of media element in milli-seconds. Does not throw any
     * exception but returns at least zero.
     */
    public int getDuration() {
        if (player == null || !prepared)
            return 0;
        else
            return player.getDuration();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        this.prepared = true;

        // Try to get audio focus
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        // Only start playback if focus is granted
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // So we have audio focus and we tell the audio manager all the
            // details about our playback and that it should route media buttons
            // to us
            updateAudioManager();
            updateRemoteControlPlaystate(PLAYSTATE_PLAYING);

            // Go start and show the notification
            player.seekTo(episodeManager.getResumeAt(currentEpisode));
            player.start();
            startForeground(NOTIFICATION_ID, notification.build(currentEpisode));
            startPlayProgressTimer();

            // Alert the listeners
            for (PlayServiceListener listener : listeners)
                listener.onPlaybackStarted();
        } else
            onError(mediaPlayer, 0, 0);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        // Send buffer information to listeners
        for (PlayServiceListener listener : listeners)
            listener.onBufferUpdate(getDuration() * percent / 100);

        // This will fix the case where the media player does not send a
        // "BUFFERING_END" event via onInfo(), we will simply create our own:
        if (buffering && getDuration() > 0 &&
                percent > getCurrentPosition() / (float) getDuration() * 100)
            onInfo(player, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                buffering = true;
                updateRemoteControlPlaystate(PLAYSTATE_BUFFERING);

                for (PlayServiceListener listener : listeners)
                    listener.onStopForBuffering();

                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                buffering = false;
                updateRemoteControlPlaystate(isPlaying() ? PLAYSTATE_PLAYING : PLAYSTATE_PAUSED);

                for (PlayServiceListener listener : listeners)
                    listener.onResumeFromBuffering();

                break;
        }

        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        updateRemoteControlPlaystate(PLAYSTATE_STOPPED);

        // Mark the episode old (needs to be done before resetting the service!)
        episodeManager.setState(currentEpisode, true);
        // Delete download if auto delete is enabled
        if (shouldAutoDeleteCompletedEpisode(currentEpisode))
            episodeManager.deleteDownload(currentEpisode);

        // If there is another episode on the playlist, play it.
        if (!episodeManager.isPlaylistEmptyBesides(currentEpisode))
            playNext();
            // If not, stop
        else {
            // Pop the episode off the playlist
            episodeManager.removeFromPlaylist(currentEpisode);

            reset();
            stopSelfIfUnboundAndIdle();
        }

        // Alert listeners
        if (listeners.size() > 0)
            for (PlayServiceListener listener : listeners)
                listener.onPlaybackComplete();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        updateRemoteControlPlaystate(PLAYSTATE_ERROR);

        // If there is another downloaded episode in the playlist, play it.
        final SortedMap<Integer, Episode> playlist = episodeManager.getDownloadedPlaylist();
        if (!(playlist.isEmpty() || (playlist.size() == 1 && playlist.values().contains(
                currentEpisode)))) {
            // Find the current episode's position in the complete playlist and
            // remove it from the playlist of downloaded episodes item since we
            // will not play that one in any case and we know there is at least
            // one other episode more.
            final int currentEpisodePosition = episodeManager.getPlaylistPosition(currentEpisode);
            playlist.remove(currentEpisodePosition);

            // Play the episode with the lowest key or the one preceding the
            // current episode's position
            Episode next = playlist.get(playlist.firstKey());

            if (currentEpisodePosition > 0 && currentEpisodePosition < playlist.lastKey()) {
                SortedMap<Integer, Episode> tail = playlist.tailMap(currentEpisodePosition);
                next = tail.get(tail.firstKey());
            }

            playEpisode(next);
        }
        // If there is anybody listening, alert and let them decide what to do
        // next, if not we reset and possibly stop ourselves
        else if (listeners.size() > 0)
            for (PlayServiceListener listener : listeners)
                listener.onError();
        else {
            reset();
            stopSelfIfUnboundAndIdle();

            Log.d(TAG, "Media player send error: " + what + "/" + extra);
        }

        return true;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                player.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and
                // release media player
                reset();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                player.setVolume(DUCK_VOLUME, DUCK_VOLUME);
                break;
        }
    }

    /**
     * Reset the service to creation state.
     */
    public void reset() {
        // Store resume at time
        storeResumeAt();

        // Stop current playback if any
        if (isPlaying())
            player.stop();

        // Remove notification
        stopForeground(true);
        stopPlayProgressTimer();

        // Reset variables
        this.currentEpisode = null;
        this.prepared = false;
        this.buffering = false;

        // Release resources
        audioManager.abandonAudioFocus(this);
        audioManager.unregisterRemoteControlClient(remoteControlClient);
        audioManager.unregisterMediaButtonEventReceiver(mediaButtonReceiver);
        if (wifiLock.isHeld())
            wifiLock.release();

        // Release player
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void storeResumeAt() {
        if (currentEpisode != null && player != null) {
            final int position = player.getCurrentPosition();
            final int duration = player.getDuration();

            // Only set resume at time if it is actually interesting, i.e. not
            // at the beginning or very close to the end (position == duration
            // might not be true even after player called onCompletion)
            episodeManager.setResumeAt(currentEpisode,
                    position == 0 || position / (float) duration > 0.99 ? null : position);
        }
    }

    private void enableReceiver(final ComponentName receiver) {
        // This writes to disk, so go off the main thread
        new Thread() {
            @Override
            public void run() {
                getPackageManager().setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }.start();
    }

    private void disableReceiver(final ComponentName receiver) {
        // This writes to disk, so go off the main thread
        new Thread() {
            @Override
            public void run() {
                getPackageManager().setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            }
        }.start();
    }

    private void initPlayer() {
        player = new MediaPlayer();

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnInfoListener(this);
        player.setOnBufferingUpdateListener(this);
    }

    private void startPlayProgressTimer() {
        // Only start task if it isn't already running
        if (playUpdateTimerTask == null) {
            final TimerTask task = new TimerTask() {

                @Override
                public void run() {
                    startForeground(NOTIFICATION_ID,
                            notification.updateProgress(getCurrentPosition(), getDuration()));
                }
            };

            playUpdateTimer.schedule(task, 1000, 1000);
            playUpdateTimerTask = task;
        }
    }

    private void rebuildNotification() {
        if (isPrepared() && currentEpisode != null)
            startForeground(NOTIFICATION_ID,
                    notification.build(currentEpisode, !isPlaying(), getCurrentPosition(),
                            getDuration())
            );
    }

    private void stopPlayProgressTimer() {
        if (playUpdateTimerTask != null) {
            playUpdateTimerTask.cancel();
            playUpdateTimerTask = null;
        }
    }

    private void stopSelfIfUnboundAndIdle() {
        if (!bound && currentEpisode == null)
            stopSelf();
    }

    private void updateAudioManager() {
        // Register our media button receiver
        audioManager.registerMediaButtonEventReceiver(mediaButtonReceiver);

        // Build the PendingIntent for the remote control client
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(mediaButtonReceiver);
        PendingIntent mediaPendingIntent =
                PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);

        // Create and register the remote control client
        remoteControlClient = new PodcatcherRCClient(mediaPendingIntent, this, currentEpisode);
        audioManager.registerRemoteControlClient(remoteControlClient);
    }

    private void updateRemoteControlPlaystate(int state) {
        if (remoteControlClient != null)
            remoteControlClient.setPlaybackState(state);
    }

    private boolean shouldAutoDeleteCompletedEpisode(Episode episode) {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean(SettingsActivity.KEY_AUTO_DELETE, false);
    }
}
