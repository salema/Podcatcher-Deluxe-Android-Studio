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

    private static final String HTTP = "http";
    private static final String ITPC = "itpc";
    private static final String PCAST = "pcast";
    private static final String FEED = "feed";
    private static final String RSS = "rss";

    @Override
    protected void onStart() {
        super.onStart();

        // This is an external call to add a new podcast that matched one of our
        // intent filters
        if (getIntent().getData() != null) {
            Intent intent = new Intent(this, PodcastActivity.class);

            // We need to replace the itpc:// scheme if present
            String uri = getIntent().getDataString();
            if (uri.startsWith(ITPC))
                uri = uri.replaceFirst(ITPC, HTTP);
            else if (uri.startsWith(PCAST))
                uri = uri.replaceFirst(PCAST, HTTP);
            else if (uri.startsWith(FEED))
                uri = uri.replaceFirst(FEED, HTTP);
            else if (uri.startsWith(RSS))
                uri = uri.replaceFirst(RSS, HTTP);

            // Make the new intent work as intended
            intent.setData(Uri.parse(uri));
            intent.addFlags(LAUNCHER_FLAGS);

            startActivity(intent);
        }

        // Make sure we stop here
        finish();
    }
}
