/** Copyright 2012-2015 Kevin Hausmann
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

import android.content.Intent;
import android.net.Uri;

/**
 * Non-UI activity to import podcasts. This just aligns the data and forwards it
 * to the {@link PodcastActivity}.
 */
public class ImportPodcastActivity extends BaseActivity {

    /*
     * These flags will make sure the PodcastActivity goes into the desired
     * state before handling the intent.
     */
    private static final int LAUNCHER_FLAGS = Intent.FLAG_ACTIVITY_CLEAR_TOP |
            Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP;

    /**
     * Scheme to use as a replacement for the specific ones
     */
    private static final String HTTP = "http";

    @Override
    protected void onStart() {
        super.onStart();

        // This is an external call to add a new podcast that matched one of our intent filters
        if (getIntent().getData() != null) {
            // We need to replace the itpc:// etc. schemes if present
            String uri = getIntent().getDataString()
                    .replaceFirst("^(itpc|pcast|feed|rss|pcd)", HTTP);

            // Catch cases like "pcast:example.org/feed.xml" and convert to proper URL
            if (uri.startsWith(HTTP + ":") && uri.charAt(5) != '/')
                uri = uri.replaceFirst(":", "://");

            // Catch calls from "Subscribe on Android" and convert to proper URL
            uri = uri.replaceFirst("(subscribeonandroid.com/|www.subscribeonandroid.com/)", "");

            // Finally, we might now have "http://" two times in a row, fix that
            if (uri.matches("^http://https?://.*"))
                uri = uri.replaceFirst(HTTP + "://", "");

            // Start (or reset display for) main activity
            final Intent intent = new Intent(this, PodcastActivity.class);
            intent.setData(Uri.parse(uri));
            intent.addFlags(LAUNCHER_FLAGS);

            startActivity(intent);
        }

        // Make sure we stop here
        finish();
    }
}
