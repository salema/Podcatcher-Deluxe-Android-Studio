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

import com.podcatcher.deluxe.listeners.PlayServiceListener;
import com.podcatcher.deluxe.services.PlayEpisodeService;
import com.podcatcher.deluxe.services.PlayEpisodeService.PlayServiceBinder;
import com.podcatcher.deluxe.view.fragments.VideoSurfaceProvider;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.MediaController;

/**
 * Show fullscreen video activity.
 */
public class DefaultFullscreenVideoActivity extends BaseActivity implements VideoSurfaceProvider,
        PlayServiceListener, OnSystemUiVisibilityChangeListener {

    /**
     * Play service
     */
    protected PlayEpisodeService service;
    /**
     * The media controller overlay
     */
    private MediaController controller;

    /**
     * Milli-seconds to wait for system UI to be hidden
     */
    private static final int HIDE_SYSTEM_UI_DELAY = 5000;
    /**
     * The system UI flags we want to set
     */
    private int systemUiFlags;

    /**
     * Flag to indicate whether video surface is available
     */
    private boolean videoSurfaceAvailable = false;
    /**
     * Our video surface holder callback to update availability
     */
    private VideoCallback videoCallback = new VideoCallback();

    /**
     * The video view
     */
    private SurfaceView videoView;

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We only get to work if fullscreen playback is actually enabled.
        if (selection.isFullscreenEnabled()) {
            setContentView(R.layout.fullscreen_video);

            videoView = (SurfaceView) findViewById(R.id.fullscreen_video);
            videoView.getHolder().addCallback(videoCallback);

            // Needed for dimming/hiding of the system UI
            videoView.setOnSystemUiVisibilityChangeListener(this);
            // Set system UI flags depending on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                systemUiFlags = View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            else
                systemUiFlags |= View.SYSTEM_UI_FLAG_LOW_PROFILE;

            // Attach to play episode service
            Intent intent = new Intent(this, PlayEpisodeService.class);
            bindService(intent, connection, 0);
        }
        // Stop ourselves
        else
            finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        videoView.setSystemUiVisibility(systemUiFlags);
        showMediaControllerOverlay();
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        if (visibility == View.SYSTEM_UI_FLAG_VISIBLE) {
            showMediaControllerOverlay();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    videoView.setSystemUiVisibility(systemUiFlags);
                }
            }, HIDE_SYSTEM_UI_DELAY);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showMediaControllerOverlay();

        return false;
    }

    @Override
    public void finish() {
        selection.setFullscreenEnabled(false);

        super.finish();
    }

    @Override
    protected void onDestroy() {
        try {
            videoView.setOnSystemUiVisibilityChangeListener(null);
            videoView.getHolder().removeCallback(videoCallback);
        } catch (NullPointerException npe) {
            // VideoView is already gone, pass
        }

        // Detach from play service (prevents leaking)
        if (service != null) {
            // This was introduced to prevent leaking of this activity,
            // but broke proper resume of the embedded video playback.
            // It should not make any difference though, because any
            // references to this activity instance held by the play
            // service will be overwritten by the episode fragment
            // immediately after this activity went away.
            // service.setVideoSurfaceProvider(null);

            service.removePlayServiceListener(this);
            unbindService(connection);
        }

        super.onDestroy();
    }

    @Override
    public void onVideoAvailable() {
        // pass
    }

    @Override
    public void onPlaybackStarted() {
        // If we go to the next episode and that one has no video content, close
        // the activity.
        if (service != null && !service.isVideo())
            finish();
        else {
            // Update the controller
            showMediaControllerOverlay();
            attachPrevNextListeners();
        }
    }

    @Override
    public void onPlaybackStateChanged() {
        showMediaControllerOverlay();
    }

    @Override
    public void onStopForBuffering() {
        // pass
    }

    @Override
    public void onResumeFromBuffering() {
        // pass
    }

    @Override
    public void onBufferUpdate(int seconds) {
        // pass
    }

    @Override
    public void onPlaybackComplete() {
        if (episodeManager.isPlaylistEmpty())
            finish();
    }

    @Override
    public void onError() {
        // Close activity on error
        finish();
    }

    @Override
    public SurfaceHolder getVideoSurface() {
        return videoView == null ? null : videoView.getHolder();
    }

    @Override
    public boolean isVideoSurfaceAvailable() {
        return videoSurfaceAvailable;
    }

    @Override
    public void adjustToVideoSize(int width, int height) {
        LayoutParams layoutParams = videoView.getLayoutParams();

        layoutParams.height = (int) (((float) height / (float) width) *
                (float) videoView.getWidth());

        videoView.setLayoutParams(layoutParams);
    }

    private void showMediaControllerOverlay() {
        if (controller != null)
            try {
                controller.show(HIDE_SYSTEM_UI_DELAY);
            } catch (WindowManager.BadTokenException bte) {
                // This happens of the Amazon devices a lot, not sure why
                // pass
            }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder serviceBinder) {
            service = ((PlayServiceBinder) serviceBinder).getService();
            // Attach the listeners and provide our surface
            service.setVideoSurfaceProvider(DefaultFullscreenVideoActivity.this);
            service.addPlayServiceListener(DefaultFullscreenVideoActivity.this);

            // Create the media controller to show when the surface is touched
            controller = new MediaController(DefaultFullscreenVideoActivity.this, service.canSeekForward());
            controller.setMediaPlayer(service);
            controller.setAnchorView(findViewById(R.id.controller_anchor));

            attachPrevNextListeners();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Nothing to do here
        }
    };

    /**
     * The callback implementation
     */
    private final class VideoCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            videoSurfaceAvailable = true;

            showMediaControllerOverlay();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // pass
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            videoSurfaceAvailable = false;
        }
    }

    private final class NextListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (service != null)
                service.playNext();
        }
    }

    private final class PrevListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            if (service != null) {
                service.seekTo(0);

                // This will make sure the progress bar is updated
                showMediaControllerOverlay();
            }
        }
    }

    private void attachPrevNextListeners() {
        if (controller != null)
            controller.setPrevNextListeners(
                    // Only show next option if there is an episode waiting in queue
                    episodeManager.isPlaylistEmpty() ? null : new NextListener(),
                    // Only show prev option if the current media is seekable
                    service.canSeekBackward() ? new PrevListener() : null);
    }
}
