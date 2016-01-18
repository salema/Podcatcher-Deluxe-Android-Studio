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

import java.util.ArrayList;
import java.util.List;

import static com.podcatcher.deluxe.Podcatcher.AUTHORIZATION_KEY;

/**
 * Show fullscreen video activity. Uses the MX Player Pro, see
 * https://sites.google.com/site/mxvpen/api
 */
public class MxPlayerFullscreenVideoActivity extends BaseActivity {

    /**
     * MX Player Pro package
     */
    public static final String MXVP_PRO = "com.mxtech.videoplayer.pro";
    /**
     * MX Player Pro fullscreen video activity class
     */
    public static final String MXVP_PLAYBACK_CLASS = "com.mxtech.videoplayer.ActivityScreen";

    /**
     * MX Player Pro intent configuration options
     */
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_VIDEO_LIST = "video_list";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_RETURN_RESULT = "return_result";
    public static final String EXTRA_HEADERS = "headers";
    public static final String EXTRA_END_BY = "end_by";
    public static final String EXTRA_PLAYBACK_COMPLETION = "playback_completion";

    /**
     * Test whether the MX Player Pro is available for video playback.
     *
     * @param context Context to test in.
     * @return <code>true</code> iff the MX Player Pro is available and can be used.
     */
    public static boolean isAvailable(Context context) {
        try {
            final ComponentName componentName = new ComponentName(MXVP_PRO, MXVP_PLAYBACK_CLASS);
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

            intent.setClassName(MXVP_PRO, MXVP_PLAYBACK_CLASS);
            intent.putExtra(EXTRA_VIDEO_LIST, new Uri[]{intent.getData()});
            intent.putExtra(EXTRA_TITLE, episode.getName());
            intent.putExtra(EXTRA_RETURN_RESULT, true);
            intent.putExtra(EXTRA_HEADERS, createHeaderList(episode));
            if (!episode.isLive())
                intent.putExtra(EXTRA_POSITION, episodeManager.getResumeAt(episode));

            startActivityForResult(intent, 42);
        } else if (!selection.isFullscreenEnabled())
            finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final Episode episode = selection.getEpisode();
        if (resultCode == RESULT_OK && episode != null && !episode.isLive()) {
            // Depending on the version of MX Player installed,
            // there are different ways to determine if playback completed
            final boolean playbackCompleted = data.hasExtra(EXTRA_END_BY) ? // This should be present for version >= 1.7.19
                    data.getStringExtra(EXTRA_END_BY).equals(EXTRA_PLAYBACK_COMPLETION) :
                    !data.hasExtra(EXTRA_POSITION); // This is a good approximation

            if (playbackCompleted) {
                episodeManager.setState(episode, true);
                episodeManager.setResumeAt(episode, null);

                if (PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean(SettingsActivity.KEY_AUTO_DELETE, false))
                    episodeManager.deleteDownload(episode);
            } else if (data.hasExtra(EXTRA_POSITION))
                episodeManager.setResumeAt(episode, data.getIntExtra(EXTRA_POSITION, 0));
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        selection.setFullscreenEnabled(false);
    }

    private String[] createHeaderList(Episode episode) {
        // Header example from MX Player docs: String[] headers =
        // new String[]{"User-Agent", "MX Player Caller App/1.0", "Extra-Header", "911"};
        final List<String> headers = new ArrayList<>(4);

        headers.add(Podcatcher.USER_AGENT_KEY);
        headers.add(Podcatcher.USER_AGENT_VALUE);

        // Put username and password if needed
        final String auth = episode.getPodcast().getAuthorization();
        if (auth != null) {
            headers.add(AUTHORIZATION_KEY);
            headers.add(auth);
        }

        return headers.toArray(new String[headers.size()]);
    }
}
