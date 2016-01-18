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

import com.podcatcher.deluxe.model.types.Episode;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * Show fullscreen video activity. Uses the VLC player.
 */
public class VlcPlayerFullscreenVideoActivity extends BaseActivity {

    /**
     * VLC player package
     */
    public static final String VLC_PACKAGE = "org.videolan.vlc";
    /**
     * VLC player video activity class
     */
    public static final String VLC_PLAYBACK_CLASS = "org.videolan.vlc.gui.video.VideoPlayerActivity";

    /**
     * VLC player intent configuration options
     */
    private static final String INTENT_TITLE = "title";
    private static final String INTENT_POSITION = "position";
    /**
     * VLC player result extras
     */
    private static final String RESULT_POSITION = "extra_position";
    private static final String RESULT_DURATION = "extra_duration";

    /**
     * Test whether the VLC is available for video playback.
     *
     * @param context Context to test in.
     * @return <code>true</code> iff the VLC Player is available and can be used.
     */
    public static boolean isAvailable(Context context) {
        try {
            final ComponentName componentName = new ComponentName(VLC_PACKAGE, VLC_PLAYBACK_CLASS);
            context.getPackageManager().getActivityInfo(componentName, 0);

            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (selection.isFullscreenEnabled() && savedInstanceState == null) {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            final Episode episode = selection.getEpisode();

            if (episodeManager.isDownloaded(episode))
                intent.setData(Uri.parse(episodeManager.getLocalPath(episode)));
            else
                intent.setData(Uri.parse(episode.getMediaUrl()));

            intent.setClassName(VLC_PACKAGE, VLC_PLAYBACK_CLASS);
            intent.putExtra(INTENT_TITLE, episode.getName());
            if (!episode.isLive())
                intent.putExtra(INTENT_POSITION, (long) episodeManager.getResumeAt(episode));

            startActivityForResult(intent, 42);
        } else if (!selection.isFullscreenEnabled())
            finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Episode episode = selection.getEpisode();
        if (resultCode == RESULT_OK && episode != null && !episode.isLive()) {
            final long position = data.getLongExtra(RESULT_POSITION, 0);
            final long duration = data.getLongExtra(RESULT_DURATION, 0);

            if (position >= duration) {
                episodeManager.setState(episode, true);
                episodeManager.setResumeAt(episode, null);

                if (PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean(SettingsActivity.KEY_AUTO_DELETE, false))
                    episodeManager.deleteDownload(episode);
            } else
                episodeManager.setResumeAt(episode, (int) position);
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        selection.setFullscreenEnabled(false);
    }
}
