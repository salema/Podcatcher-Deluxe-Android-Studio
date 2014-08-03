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

package com.podcatcher.deluxe;

import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.SeekBar;

import com.podcatcher.deluxe.listeners.OnChangeEpisodeStateListener;
import com.podcatcher.deluxe.listeners.OnChangePlaylistListener;
import com.podcatcher.deluxe.listeners.OnDownloadEpisodeListener;
import com.podcatcher.deluxe.listeners.OnRequestFullscreenListener;
import com.podcatcher.deluxe.listeners.OnSelectEpisodeListener;
import com.podcatcher.deluxe.listeners.PlayServiceListener;
import com.podcatcher.deluxe.listeners.PlayerListener;
import com.podcatcher.deluxe.model.tasks.remote.DownloadEpisodeTask.EpisodeDownloadError;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.services.PlayEpisodeService;
import com.podcatcher.deluxe.services.PlayEpisodeService.PlayServiceBinder;
import com.podcatcher.deluxe.view.fragments.DeleteDownloadsConfirmationFragment;
import com.podcatcher.deluxe.view.fragments.DeleteDownloadsConfirmationFragment.OnDeleteDownloadsConfirmationListener;
import com.podcatcher.deluxe.view.fragments.EpisodeFragment;
import com.podcatcher.deluxe.view.fragments.PlayerFragment;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.podcatcher.deluxe.view.fragments.DeleteDownloadsConfirmationFragment.TAG;

/**
 * Show episode activity. This is thought of as an abstract activity for an app
 * only consisting of an episode view and the player. Sub-classes could extend
 * or simply show this layout.
 */
public abstract class EpisodeActivity extends BaseActivity implements
        PlayerListener, PlayServiceListener, OnSelectEpisodeListener,
        OnDownloadEpisodeListener, OnChangePlaylistListener, OnChangeEpisodeStateListener,
        OnRequestFullscreenListener {

    /**
     * Key used to store episode URL in intent or bundle
     */
    public static final String EPISODE_URL_KEY = "episode_url_key";

    /**
     * The current episode fragment
     */
    protected EpisodeFragment episodeFragment;
    /**
     * The current player fragment
     */
    protected PlayerFragment playerFragment;

    /**
     * Play service
     */
    private PlayEpisodeService service;

    /**
     * Play update timer
     */
    private Timer playUpdateTimer = new Timer();
    /**
     * Play update timer task
     */
    private TimerTask playUpdateTimerTask;
    /**
     * Flag for visibility, coordinating timer
     */
    private boolean visible = false;

    /**
     * The actual task to regularly update the UI on playback
     */
    private static class PlayProgressTask extends TimerTask {

        /**
         * Use weak reference to avoid any leaking of activities
         */
        private final WeakReference<EpisodeActivity> activityReference;

        /**
         * Create a new update task.
         *
         * @param episodeActivity Activity to call update player for.
         */
        public PlayProgressTask(EpisodeActivity episodeActivity) {
            activityReference = new WeakReference<>(episodeActivity);
        }

        @Override
        public void run() {
            final EpisodeActivity episodeActivity = activityReference.get();
            if (episodeActivity != null) {
                // Need to run on UI thread, since we want to update the player
                episodeActivity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        episodeActivity.updatePlayerUi();
                    }
                });
            }
        }
    }

    /**
     * Get the fragments needed by this activity from the fragment manager and
     * set member fields. Sub-classes should call this after setting their
     * content view or plugging in fragments. Sub-classes that use their own
     * fragments should also extend this. Members will only be set if
     * <code>null</code>. It is safe to assume the fragment members to be
     * non-null once this method completed.
     */
    protected void findFragments() {
        // The player fragment to use
        if (playerFragment == null)
            playerFragment = (PlayerFragment) findByTagId(R.string.player_fragment_tag);

        // The episode fragment to use
        if (episodeFragment == null)
            episodeFragment = (EpisodeFragment) findByTagId(R.string.episode_fragment_tag);
    }

    /**
     * Register the various listeners, that will alert our activities on model
     * or UI changes. This runs after the fragments have been established to
     * avoid the case where we are hit by a call-back and could not react to it.
     */
    protected void registerListeners() {
        // Make sure play service is started
        startService(new Intent(this, PlayEpisodeService.class));
        // Attach to play service, this will register the play service listener
        // once the service is up
        Intent intent = new Intent(this, PlayEpisodeService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        // We have to do this here instead of onCreate since we can only react
        // on the call-backs properly once we have our fragment
        episodeManager.addDownloadListener(this);
        episodeManager.addPlaylistListener(this);
        episodeManager.addStateChangedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.visible = true;

        // This is safe since it actually only starts the timer if it is
        // actually needed
        startPlayProgressTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateActionBar();
        updatePlayerUi();
        updateVideoSurface();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.visible = false;

        stopPlayProgressTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop the timer
        playUpdateTimer.cancel();

        // Disconnect from episode manager
        episodeManager.removeDownloadListener(this);
        episodeManager.removePlaylistListener(this);
        episodeManager.removeStateChangedListener(this);

        // Detach from play service (prevents leaking)
        if (service != null) {
            service.removePlayServiceListener(this);
            unbindService(connection);
        }
    }

    @Override
    public void onRequestFullscreen() {
        if (!selection.isFullscreenEnabled()) {
            selection.setFullscreenEnabled(true);

            startActivity(new Intent(this, FullscreenVideoActivity.class));
        }
    }

    @Override
    public void onEpisodeSelected(Episode selectedEpisode) {
        selection.setEpisode(selectedEpisode);

        switch (view) {
            case LARGE_PORTRAIT:
            case LARGE_LANDSCAPE:
                // Set episode in episode fragment
                episodeFragment.setEpisode(selectedEpisode);

                break;
            case SMALL_LANDSCAPE:
                // We need to create new episode fragment here each time to
                // renew the video surface
                episodeFragment = new EpisodeFragment();

                // Add the fragment to the UI, replacing the list fragment if it
                // is not already there
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.right, episodeFragment,
                            getString(R.string.episode_fragment_tag));
                    transaction.addToBackStack(null);
                    transaction.commit();

                    getFragmentManager().executePendingTransactions();
                }

                // Set the episode
                episodeFragment.setEpisode(selectedEpisode);
                episodeFragment.setShowEpisodeDate(true);

                updateVideoSurface();

                break;
            case SMALL_PORTRAIT:
                // This should be handled by sub-class
                break;
        }

        updatePlayerUi();
        updateDownloadUi();
        updateStateUi();
    }

    @Override
    public void onReturnToPlayingEpisode() {
        if (service != null && service.getCurrentEpisode() != null)
            onEpisodeSelected(service.getCurrentEpisode());
    }

    @Override
    public void onNoEpisodeSelected() {
        selection.resetEpisode();

        updatePlayerUi();
        updateDownloadUi();
    }

    @Override
    public void onDownloadProgress(Episode episode, int percent) {
        // pass
    }

    @Override
    public void onDownloadSuccess(Episode episode) {
        updateDownloadUi();
    }

    @Override
    public void onDownloadDeleted(Episode episode) {
        updateDownloadUi();
    }

    @Override
    public void onDownloadFailed(Episode episode, EpisodeDownloadError error) {
        updateDownloadUi();
    }

    @Override
    public void onPlaylistChanged() {
        updatePlaylistUi();
        updatePlayerUi();
    }

    @Override
    public void onStateChanged(Episode episode, boolean newState) {
        updateStateUi();
    }

    @Override
    public void onResumeAtChanged(Episode episode, Integer millis) {
        updateStateUi();
    }

    @Override
    public void onToggleDownload() {
        if (selection.isEpisodeSet()) {
            // Check for action to perform
            boolean download = !episodeManager.isDownloadingOrDownloaded(selection.getEpisode());

            // Kick off the appropriate action
            if (download) {
                episodeManager.download(selection.getEpisode());

                showToast(getString(R.string.download_started, selection.getEpisode().getName()));
                updateDownloadUi();
            } else {
                // For deletion, we show a confirmation dialog first
                final DeleteDownloadsConfirmationFragment confirmationDialog =
                        new DeleteDownloadsConfirmationFragment();
                confirmationDialog.setListener(new OnDeleteDownloadsConfirmationListener() {

                    @Override
                    public void onConfirmDeletion() {
                        episodeManager.deleteDownload(selection.getEpisode());
                    }

                    @Override
                    public void onCancelDeletion() {
                        // Nothing to do here...
                    }
                });

                confirmationDialog.show(getFragmentManager(), TAG);
            }
        }
    }

    @Override
    public void onToggleLoad() {
        // Stop timer task
        stopPlayProgressTimer();

        // Stop called: unload episode
        if (service.isLoadedEpisode(selection.getEpisode()))
            service.reset();
            // Play called on unloaded episode
        else if (selection.isEpisodeSet())
            service.playEpisode(selection.getEpisode());

        // Update UI
        updatePlayerUi();
        playerFragment.updateSeekBarSecondaryProgress(0);
    }

    @Override
    public void onTogglePlay() {
        // Player is playing
        if (service.isPlaying()) {
            service.pause();
            stopPlayProgressTimer();
        } // Player in pause
        else {
            service.resume();
            startPlayProgressTimer();
        }

        updatePlayerUi();
    }

    @Override
    public void onNext() {
        service.playNext();
        updatePlayerUi();
    }

    @Override
    public void onRewind() {
        if (service != null && service.isPrepared()) {
            service.rewind();

            updatePlayerUi();
        }
    }

    @Override
    public void onFastForward() {
        if (service != null && service.isPrepared()) {
            service.fastForward();

            updatePlayerUi();
        }
    }

    @Override
    public void onPlaybackStarted() {
        startPlayProgressTimer();
    }

    @Override
    public void onVideoAvailable() {
        if (episodeFragment != null)
            episodeFragment.setShowVideoView(true,
                    view.isSmallLandscape() || view.isLargePortrait());
    }

    @Override
    public void onPlaybackStateChanged() {
        updatePlayerUi();

        if (service != null && service.isPlaying())
            startPlayProgressTimer();
        else
            stopPlayProgressTimer();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            service.seekTo(progress);
            updatePlayerUi();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        stopPlayProgressTimer();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        startPlayProgressTimer();
    }

    @Override
    public void onStopForBuffering() {
        stopPlayProgressTimer();
        updatePlayerUi();
    }

    @Override
    public void onResumeFromBuffering() {
        startPlayProgressTimer();
    }

    @Override
    public void onBufferUpdate(int seconds) {
        playerFragment.updateSeekBarSecondaryProgress(seconds);
    }

    @Override
    public void onPlaybackComplete() {
        if (episodeManager.isPlaylistEmpty()) {
            stopPlayProgressTimer();
            updatePlayerUi();

            if (episodeFragment != null)
                episodeFragment.setShowVideoView(false, false);
        }
    }

    @Override
    public void onError() {
        stopPlayProgressTimer();
        service.reset();

        if (episodeFragment != null)
            episodeFragment.setShowVideoView(false, false);

        updatePlayerUi();
        playerFragment.setPlayerVisibility(true);
        playerFragment.setErrorViewVisibility(true);
    }

    /**
     * Update the action bar to reflect current selection and loading state.
     * Sub-classes need to overwrite.
     */
    protected abstract void updateActionBar();

    /**
     * Update all UI related to the download state of the current selection.
     * Sub-classes might want to extend this.
     */
    protected void updateDownloadUi() {
        // The episode fragment might be popped out if we are in small landscape
        // view mode and the episode list is currently visible
        if (episodeFragment != null) {
            final boolean downloading = episodeManager.isDownloading(selection.getEpisode());
            final boolean downloaded = episodeManager.isDownloaded(selection.getEpisode());

            episodeFragment.setDownloadMenuItemVisibility(selection.isEpisodeSet(),
                    !(downloading || downloaded));
            episodeFragment.setDownloadIconVisibility(downloading || downloaded, downloaded);
        }
    }

    /**
     * Update all UI related to the playlist. Sub-classes might want to extend
     * this.
     */
    protected void updatePlaylistUi() {
        // Nothing to do here
    }

    /**
     * Update all UI related to the old/new state of the current selection.
     * Sub-classes might want to extend this.
     */
    protected void updateStateUi() {
        // The episode fragment might be popped out if we are in small landscape
        // view mode and the episode list is currently visible
        if (episodeFragment != null)
            episodeFragment.setNewIconVisibility(!episodeManager.getState(selection.getEpisode()));
    }

    /**
     * Broadcast the video surface to the episode playback service.
     */
    protected void updateVideoSurface() {
        if (service != null && episodeFragment != null && !selection.isFullscreenEnabled())
            service.setVideoSurfaceProvider(episodeFragment);
    }

    /**
     * Update the player fragment UI to reflect current state of play.
     */
    protected void updatePlayerUi() {
        // Even though all the fragments should be present, the service might
        // not have been connected to yet.
        if (service != null) {
            final boolean currentEpisodeIsShowing = service.isLoadedEpisode(selection.getEpisode());

            // Show/hide menu item
            playerFragment.setLoadMenuItemVisibility(selection.isEpisodeSet(),
                    !currentEpisodeIsShowing, !currentEpisodeIsShowing
                            && episodeManager.getResumeAt(selection.getEpisode()) > 0
            );

            // Show/hide video output
            final boolean showVideo = service.isPrepared() && service.isVideo()
                    && currentEpisodeIsShowing;
            if (episodeFragment != null)
                episodeFragment.setShowVideoView(showVideo, showVideo
                        && (view.isSmallLandscape() || view.isLargePortrait()));

            // Make sure player is shown if and as needed (update the details
            // only if they are actually visible)
            final boolean showPlayer = service.isPreparing() || service.isPrepared();
            playerFragment.setPlayerVisibility(showPlayer);
            if (showPlayer) {
                // Make sure error view is hidden
                playerFragment.setErrorViewVisibility(false);
                // Show(hide episode title and seek bar
                playerFragment.setPlayerTitleVisibility(
                        !view.isSmallLandscape() && !currentEpisodeIsShowing);
                playerFragment.setPlayerSeekbarVisibility(!view.isSmallLandscape());
                // Enable/disable next button
                final boolean showNext =
                        !episodeManager.isPlaylistEmptyBesides(service.getCurrentEpisode());
                playerFragment.setShowShortPosition(!view.isLargePortrait() &&
                        (showNext || service.getDuration() >= TimeUnit.HOURS.toMillis(1)));
                playerFragment.setNextButtonVisibility(showNext);

                // Update UI to reflect service status
                playerFragment.updatePlayerTitle(service.getCurrentEpisode());
                playerFragment.updateSeekBar(!service.isPreparing(), service.getDuration(),
                        service.getCurrentPosition());
                playerFragment.updateButton(service.isBuffering(), service.isPlaying(),
                        service.getDuration(), service.getCurrentPosition());
            }
        }
    }

    private void startPlayProgressTimer() {
        // Do not start the task if there is no progress to monitor and we are
        // visible (this fixes the case of stacked activities running the timer)
        if (visible && service != null && service.isPlaying()) {
            // Only start task if it isn't already running and
            // there is actually some progress to monitor
            if (playUpdateTimerTask == null) {
                PlayProgressTask task = new PlayProgressTask(this);

                try {
                    playUpdateTimer.schedule(task, 0, 1000);
                    playUpdateTimerTask = task;
                } catch (IllegalStateException e) {
                    // In rare cases, the timer might be canceled (the activity
                    // is going down) while schedule() is called, skip...
                }
            }
        }
    }

    private void stopPlayProgressTimer() {
        if (playUpdateTimerTask != null) {
            playUpdateTimerTask.cancel();
            playUpdateTimerTask = null;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            service = ((PlayServiceBinder) serviceBinder).getService();

            // Register listener
            service.addPlayServiceListener(EpisodeActivity.this);

            // Update player UI
            updatePlayerUi();
            updateVideoSurface();

            // Restart play progress timer task if service is playing
            if (service.isPlaying())
                startPlayProgressTimer();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Nothing to do here
        }
    };
}
