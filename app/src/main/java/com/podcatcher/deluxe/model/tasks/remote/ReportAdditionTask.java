/**
 * Copyright 2012-2015 Kevin Hausmann
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

package com.podcatcher.deluxe.model.tasks.remote;

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.model.types.Podcast;

import java.net.URL;

/**
 * Report podcast additions to the Podcatcher Deluxe website.
 */
public class ReportAdditionTask extends LoadRemoteFileTask<Podcast, Void> {

    /**
     * The path to get from podcatcher-deluxe.com
     */
    public static final String HOST_PATH = "http://www.podcatcher-deluxe.com/create-suggestion?url=";

    @Override
    protected Void doInBackground(Podcast... podcasts) {
        if (!BuildConfig.DEBUG && !BuildConfig.FIXED_BUNDLE)
            for (Podcast podcast : podcasts)
                try {
                    // All we need to do is trigger the page load and discard the result
                    loadFile(new URL(HOST_PATH + podcast.getUrl()));
                } catch (Throwable e) {
                    // pass
                }

        return null;
    }
}
