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

package com.podcatcher.deluxe.model.sync.gpodder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.dragontek.mygpoclient.api.MygPodderClient;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.sync.SyncController;

import java.util.Locale;

/**
 * A sync controller for the gpodder.net service, abstract base class.
 */
abstract class GpodderBaseSyncController extends SyncController {

    /**
     * The gpodder.net user name setting key
     */
    public static final String USERNAME_KEY = "gpodder_username";
    /**
     * The gpodder.net password setting key
     */
    public static final String PASSWORD_KEY = "gpodder_password";
    /**
     * The gpodder.net device id setting key
     */
    public static final String DEVICE_ID_KEY = "gpodder_device_id";
    /**
     * Our log tag
     */
    protected static final String TAG = "GpodderSyncController";
    /**
     * Our preferences handle
     */
    protected final SharedPreferences preferences;
    /**
     * Our client instance
     */
    protected final MygPodderClient client;
    /**
     * Our device id
     */
    protected final String deviceId;

    /**
     * Create new sync controller for the gpodder.net service.
     *
     * @param context The context to read the configuration from, in particular
     *                {@link #USERNAME_KEY}, {@link #PASSWORD_KEY}, and
     *                {@link #DEVICE_ID_KEY}.
     */
    protected GpodderBaseSyncController(Context context) {
        super();

        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String user = preferences.getString(USERNAME_KEY, "");
        final String password = preferences.getString(PASSWORD_KEY, "");

        client = new MygPodderClient(user, password);
        deviceId = preferences.getString(DEVICE_ID_KEY, getDefaultDeviceId(context));
    }

    /**
     * Create a gpodder.net device id for the device this controller is running
     * on.
     *
     * @param context The context of the controller.
     * @return A device id to use for gpodder.net.
     */
    public static String getDefaultDeviceId(Context context) {
        return (context.getResources().getString(R.string.app_name) + "-" + Build.MODEL)
                .toLowerCase(Locale.US).replace(" ", "");
    }
}
