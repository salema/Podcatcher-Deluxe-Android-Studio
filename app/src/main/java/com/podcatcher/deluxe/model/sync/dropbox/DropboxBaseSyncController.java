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

package com.podcatcher.deluxe.model.sync.dropbox;

import com.podcatcher.deluxe.model.sync.PreferenceSetSyncController;

import android.content.Context;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;

import java.util.Locale;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.podcatcher.deluxe.Podcatcher.USER_AGENT_VALUE;

/**
 * A sync controller for the Dropbox service, abstract base class. Provides some
 * common fields and access to the client handle.
 */
abstract class DropboxBaseSyncController extends PreferenceSetSyncController {

    /**
     * The Dropbox access token settings key.
     */
    public static final String ACCESS_TOKEN = "ACCESS_TOKEN";

    /**
     * The Dropbox API client.
     */
    protected final DbxClientV2 client;

    /**
     * Our log tag
     */
    protected static final String TAG = "DropboxSyncController";

    /**
     * Create new sync controller for the Dropbox service.
     *
     * @param context The context to read the configuration from, in particular
     *                our {@link #ACCESS_TOKEN}. The controller will not function
     *                unless this is set correctly.
     */
    protected DropboxBaseSyncController(Context context) {
        super(context);

        final DbxRequestConfig config = new DbxRequestConfig(USER_AGENT_VALUE, Locale.getDefault().toString());
        client = new DbxClientV2(config, getDefaultSharedPreferences(context).getString(ACCESS_TOKEN, null));
    }
}
