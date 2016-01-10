/**
 * Copyright 2012-2016 Kevin Hausmann
 *
 * This file is part of Podcatcher Deluxe.
 *
 * Podcatcher Deluxe is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Podcatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Podcatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package com.podcatcher.deluxe;

import com.podcatcher.deluxe.listeners.OnChangeEpisodeStateListener;
import com.podcatcher.deluxe.listeners.OnChangePlaylistListener;
import com.podcatcher.deluxe.listeners.OnDownloadEpisodeListener;
import com.podcatcher.deluxe.listeners.OnEpisodeInformationChangedListener;
import com.podcatcher.deluxe.listeners.OnPlayEpisodeFromPositionListener;
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

import android.annotation.TargetApi;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.widget.SeekBar;

import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.podcatcher.deluxe.view.fragments.DeleteDownloadsConfirmationFragment.TAG;

/**
 * Show episode activity. This is thought of as an abstract activity for an app
 * only consisting of an episode view and the player. Sub-classes could extend
 * or simply show this layout.
 */
public abstract class EpisodeActivity extends BaseActivity implements
        PlayerListener, PlayServiceListener, OnSelectEpisodeListener, OnDownloadEpisodeListener,
        OnEpisodeInformationChangedListener, OnChangePlaylistListener, OnChangeEpisodeStateListener,
        OnPlayEpisodeFromPositionListener {

    /**
     * Key used to store episode URL in intent or bundle
     */
    public static final String EPISODE_URL_KEY = "episode_url_key";
    /**
     * Permission request code used for downloads
     */
    public static final int REQUEST_CODE_DOWNLOAD = 42;
    /**
     * Permission request code used for playback
     */
    public static final int REQUEST_CODE_PLAYBACK = 43;

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
     * Player update handler, runs every 1000ms when playing
     */
    private Handler playerUpdateHandler = new Handler(Looper.getMainLooper());
    /**
     * Flag for visibility, coordinating update handler
     */
    private boolean visible = false;
    /**
     * Player update runnable used by the handler
     */
    private Runnable playerUpdater = new Runnable() {

        @Override
        public void run() {
            updatePlayerUi();

            playerUpdateHandler.postDelayed(this, TimeUnit.SECONDS.toMillis(1));
        }
    };

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
        // Make sure play service is started, this will not have any effect
        // if the service is already running
        startService(new Intent(this, PlayEpisodeService.class));
        // Attach to play service, this will register the play service listener
        // once the service is up
        Intent intent = new Intent(this, PlayEpisodeService.class);
        bindService(intent, connection, 0); // no flags needed

        // We have to do this here instead of onCreate since we can only react
        // on the call-backs properly once we have our fragment
        episodeManager.addInformationChangedListener(this);
        episodeManager.addDownloadListener(this);
        episodeManager.addPlaylistListener(this);
        episodeManager.addStateChangedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.visible = true;

        // This is safe since it actually only starts the updater if it is actually needed
        startPlayerUpdater();
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateActionBar();
        updatePlayerUi();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.visible = false;

        stopPlayerUpdater();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Disconnect from episode manager
        episodeManager.removeInformationChangedListener(this);
        episodeManager.removeDownloadListener(this);
        episodeManager.removePlaylistListener(this);
        episodeManager.removeStateChangedListener(this);

        // Detach from play service
        if (service != null) {
            service.removePlayServiceListener(this);
            unbindService(connection);
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
                // Find, and if not already done create, episode fragment
                if (episodeFragment == null)
                    episodeFragment = new EpisodeFragment();

                // Add the fragment to the UI, replacing the list fragment if it
                // is not already there
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.right, episodeFragment,
                            getString(R.string.episode_fragment_tag));
                    transaction.addToBackStack(null);
                    transaction.commit();
                }

                // Set the episode
                episodeFragment.setEpisode(selectedEpisode);

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
    public void onDurationChanged(Episode episode) {
        updateEpisodeMetadataUi();
    }

    @Override
    public void onMediaFileSizeChanged(Episode episode) {
        updateEpisodeMetadataUi();
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
        updateEpisodeMetadataUi();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onToggleDownload() {
        if (selection.isEpisodeSet()) {
            // Make sure we have permission to store/remove files
            if (!((Podcatcher) getApplication()).canWriteExternalStorage())
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_DOWNLOAD);
            else {
                // Check for action to perform
                boolean download = !episodeManager.isDownloadingOrDownloaded(selection.getEpisode());

                // Kick off the appropriate action
                if (download) {
                    showToast(getString(R.string.download_started, selection.getEpisode().getName()));

                    episodeManager.download(selection.getEpisode());
                    updateDownloadUi();
                } else {
                    // For deletion, we show a confirmation dialog first
                    final DeleteDownloadsConfirmationFragment confirmationDialog =
                            new DeleteDownloadsConfirmationFragment();
                    confirmationDialog.setListener(new OnDeleteDownloadsConfirmationListener() {

                        @Override
                        public void onConfirmDeletion() {
                            episodeManager.deleteDownload(selection.getEpisode());

                            // Prevent immediate re-download by marking episode 'old'
                            if (preferences.getBoolean(SettingsActivity.KEY_AUTO_DOWNLOAD, false))
                                episodeManager.setState(selection.getEpisode(), true);
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            switch (requestCode) {
                case REQUEST_CODE_DOWNLOAD:
                    onToggleDownload();
                    break;
                case REQUEST_CODE_PLAYBACK:
                    onToggleLoad();
                    break;
            }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onToggleLoad() {
        stopPlayerUpdater();

        if (service.isLoadedEpisode(selection.getEpisode()))
            service.stop();
        else if (selection.isEpisodeSet()) {
            // Make sure we have permission to play downloaded files if needed
            if (!((Podcatcher) getApplication()).canWriteExternalStorage() &&
                    episodeManager.isDownloaded(selection.getEpisode()))
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PLAYBACK);
            else
                service.playEpisode(selection.getEpisode());
        }

        // Update UI
        updatePlayerUi();
        playerFragment.updateSeekBarSecondaryProgress(0);
    }

    @Override
    public void onTogglePlay() {
        // Player is playing
        if (service.isPlaying()) {
            service.pause();
            stopPlayerUpdater();
        } // Player in pause
        else {
            service.resume();
            startPlayerUpdater();
        }

        updatePlayerUi();
    }

    @Override
    public void onPlayFromPosition(Episode episode, int millis) {
        if (episode != null && millis >= 0) {
            // This will make the service jump to the desired position once playback starts
            episodeManager.setResumeAt(episode, millis);

            // Three cases here:
            // 1. Episode is already loaded: we need to seek (and resume playback, if paused)
            // 2. Episode is loaded but not yet prepared: do nothing, will start at position set above
            // 3. Episode not loaded: make service switch to it and hit play
            if (service.isLoadedEpisode(episode) && service.isPrepared()) {
                service.seekTo(millis);
                service.resume();
                startPlayerUpdater();
            } else if (!service.isLoadedEpisode(episode))
                service.playEpisode(episode);

            updatePlayerUi();
        }
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
        startPlayerUpdater();
    }

    @Override
    public void onPlaybackStateChanged() {
        updatePlayerUi();

        if (service != null && service.isPlaying())
            startPlayerUpdater();
        else
            stopPlayerUpdater();
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
        stopPlayerUpdater();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        startPlayerUpdater();
    }

    @Override
    public void onStopForBuffering() {
        stopPlayerUpdater();
        updatePlayerUi();
    }

    @Override
    public void onResumeFromBuffering() {
        startPlayerUpdater();
    }

    @Override
    public void onBufferUpdate(int millis) {
        playerFragment.updateSeekBarSecondaryProgress(millis);
    }

    @Override
    public void onPlaybackComplete() {
        if (episodeManager.isPlaylistEmpty()) {
            stopPlayerUpdater();
            updatePlayerUi();
        }
    }

    @Override
    public void onError() {
        stopPlayerUpdater();
        service.stop();

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
     * Update all UI related to the episode metadata.
     * Sub-classes might want to extend this.
     */
    protected void updateEpisodeMetadataUi() {
        // The episode fragment might be popped out if we are in small landscape
        // view mode and the episode list is currently visible
        if (episodeFragment != null && selection.isEpisodeSet())
            episodeFragment.updateEpisodeMetadata();
    }

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

            episodeFragment.setDownloadMenuItemVisibility(selection.isEpisodeSet() &&
                    !selection.getEpisode().isLive(), !(downloading || downloaded));
            episodeFragment.setDownloadIconVisibility(downloading || downloaded, downloaded,
                    episodeManager.isDownloadedToSdCard(selection.getEpisode()));
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
     * Update the player fragment UI to reflect current state of play.
     */
    protected void updatePlayerUi() {
        // Even though all the fragments should be present, the service might
        // not have been connected to yet.
        if (service != null) {
            final boolean currentEpisodeIsShowing = service.isLoadedEpisode(selection.getEpisode());

            // Show/hide menu item
            playerFragment.setLoadMenuItemVisibility(selection.isEpisodeSet(),
                    !currentEpisodeIsShowing, !currentEpisodeIsShowing &&
                            episodeManager.getResumeAt(selection.getEpisode()) > 0);

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
                playerFragment.setShowShortPosition(view.isSmall());
                // Enable/disable next button
                playerFragment.setNextButtonVisibility(!view.isSmall() &&
                        !episodeManager.isPlaylistEmptyBesides(service.getCurrentEpisode()));

                // Update UI to reflect service status
                playerFragment.updatePlayerTitle(service.getCurrentEpisode());
                playerFragment.updateSeekBar(service.canSeek() && !service.isPreparing(),
                        service.getDuration(), service.getCurrentPosition());
                playerFragment.updateButton(service.isBuffering(), service.isPlaying(),
                        service.canSeek(), service.getDuration(), service.getCurrentPosition());
            }
        }
    }

    private void startPlayerUpdater() {
        // Do not start the task if there is no progress to monitor and we are
        // visible (this fixes the case of stacked activities running the handler)
        if (visible && service != null && service.isPlaying()) {
            // Remove existing runnables to make sure only one runs at any given time
            playerUpdateHandler.removeCallbacks(playerUpdater);
            playerUpdateHandler.post(playerUpdater);
        }
    }

    private void stopPlayerUpdater() {
        // Stop the player update handler
        playerUpdateHandler.removeCallbacks(playerUpdater);
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

            // Restart play progress timer task if service is playing
            if (service.isPlaying())
                startPlayerUpdater();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Nothing to do here
        }
    };
}
