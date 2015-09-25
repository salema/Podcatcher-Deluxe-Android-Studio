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

package com.podcatcher.deluxe.model.sync;

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.sync.dropbox.DropboxSyncController;
import com.podcatcher.deluxe.model.sync.gpodder.GpodderSyncController;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;

/**
 * The list of current {@link SyncController} implementations. This enum also
 * allows access to some meta and state information that should be available
 * without having any actual instances of a {@link SyncController} around, such
 * as {@link #isAvailable(Context)} and {@link #isLinked(Context)}.
 */
public enum ControllerImpl {
    /**
     * Sync to the <a href="https://www.dropbox.com/developers/datastore">
     * Dropbox Datastore API</a>
     */
    DROPBOX,

    /**
     * Sync to the <a href="http://www.gpodder.net">gpodder.net</a> service
     */
    GPODDER;

    /**
     * Create a {@link SyncController} object for the service given. This is for
     * convenience only, you could also call the constructors directly.
     *
     * @param context Context the {@link SyncController} operates in (not
     *                <code>null</code>).
     * @param impl    The service the controller syncs to.
     * @return The new controller instance or <code>null</code> for a
     * <code>null</code> implementation reference.
     */
    public static SyncController create(Context context, ControllerImpl impl) {
        switch (impl) {
            case GPODDER:
                return new GpodderSyncController(context);
            case DROPBOX:
                return new DropboxSyncController(context);
            default:
                return null;
        }
    }

    /**
     * @return An UI-label for this controller, generally this would be the name
     * so there should be no need for translation.
     */
    public CharSequence getLabel() {
        switch (this) {
            case GPODDER:
                return "gpodder.net";
            case DROPBOX:
                return "Dropbox";
            default:
                return "Sync Controller X";
        }
    }

    /**
     * @return The resource id of the logo drawable matching this
     * {@link SyncController}.
     */
    public int getLogoResourceId() {
        switch (this) {
            case GPODDER:
                return R.drawable.ic_gpodder;
            case DROPBOX:
                return R.drawable.ic_dropbox;
            default:
                return 0;
        }
    }

    /**
     * Find out whether the {@link SyncController} represented could run in the
     * given environment.
     *
     * @param context The context the controller should run in.
     * @return Will be <code>true</code> if the sync controller is able to
     * operate in the given context, <code>false</code> otherwise. This
     * does not look at the linked state (see {@link #isLinked(Context)}
     * of the controller, but at device compatibility. (E.g. the Dropbox
     * controller runs on arm architectures only.)
     */
    public boolean isAvailable(Context context) {
        switch (this) {
            case DROPBOX:
                final String arch = System.getProperty("os.arch");
                return "arm".equals(arch.substring(0, 3).toLowerCase(Locale.US)) &&
                        // Disable Dropbox sync for fixed bundle versions
                        !BuildConfig.FIXED_BUNDLE;
            default:
                return true;
        }
    }

    /**
     * Find out whether the {@link SyncController} represented could be enabled.
     *
     * @param context The context the controller should run in.
     * @return The linked state. Will be <code>true</code> if the sync
     * controller is ready to go, <code>false</code> otherwise. Please
     * note that this only checks for things like login credentials,
     * <em>not</em> for network state etc.
     */
    public boolean isLinked(Context context) {
        switch (this) {
            case GPODDER:
                final SharedPreferences preferences =
                        PreferenceManager.getDefaultSharedPreferences(context);

                return preferences.contains(GpodderSyncController.USERNAME_KEY) &&
                        preferences.contains(GpodderSyncController.PASSWORD_KEY) &&
                        preferences.contains(GpodderSyncController.DEVICE_ID_KEY);
            case DROPBOX:
                return DropboxSyncController.getAccountManager(context).hasLinkedAccount();
            default:
                return false;
        }
    }
}
