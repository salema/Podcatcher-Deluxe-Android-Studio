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

package com.podcatcher.deluxe.model.sync.podcare;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.sync.PreferenceSetSyncController;
import com.podcatcher.labs.sync.podcare.PodcareClient;

import android.content.Context;

import static com.podcatcher.deluxe.BuildConfig.DEBUG;
import static com.podcatcher.deluxe.Podcatcher.userAgentValue;

/**
 * A sync controller for the Podcare service, abstract base class.
 */
abstract class PodcareBaseSyncController extends PreferenceSetSyncController {

    /**
     * The Podcare connect id setting key
     */
    public static final String CONNECT_ID_KEY = "podcare_connect_id";
    /**
     * Our log tag
     */
    protected static final String TAG = "PodcareSyncController";
    /**
     * Our client instance
     */
    protected final PodcareClient podcare;
    /**
     * Our connect id
     */
    protected final String connectId;

    /**
     * Create new sync controller for the gpodder.net service.
     *
     * @param context The context to read the configuration from,
     *                in particular {@link #CONNECT_ID_KEY}.
     */
    protected PodcareBaseSyncController(Context context) {
        super(context);

        podcare = new PodcareClient(context.getString(R.string.podcare_api_key), userAgentValue, DEBUG);
        connectId = preferences.getString(CONNECT_ID_KEY, null);
    }
}
