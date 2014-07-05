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

package com.podcatcher.deluxe.model.sync.dropbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.podcatcher.deluxe.R;

import static com.podcatcher.deluxe.SettingsActivity.KEY_THEME_COLOR;

/**
 * The sync controller part for the Dropbox service that deals with the user's
 * settings.
 */
abstract class DropboxSettingsSyncController extends DropboxBaseSyncController implements
        OnSharedPreferenceChangeListener {

    /**
     * The settings table name
     */
    private static final String SETTINGS_TABLE = "settings";
    /**
     * The default theme color
     */
    private final int defaultThemeColor;
    /**
     * The settings table handle
     */
    private DbxTable settingsTable;
    /**
     * The theme color record id
     */
    private String themeColorRecordId;
    /**
     * Our shared setting handle
     */
    private SharedPreferences preferences;

    protected DropboxSettingsSyncController(Context context) {
        super(context);

        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);

        this.defaultThemeColor = context.getResources().getColor(R.color.theme_dark);

        // Since the store might not be available, we might not have a table
        // handle and need to catch NPEs in all actions below.
        if (store != null) {
            this.settingsTable = store.getTable(SETTINGS_TABLE);
            this.themeColorRecordId = toValidDataStoreId(KEY_THEME_COLOR);
        }
    }

    @Override
    public void syncSettings() {
        try {
            // Update the store with the current color setting
            if (SyncMode.SEND_ONLY.equals(mode) && preferences.contains(KEY_THEME_COLOR))
                updateThemeColorSettingInDataStore();

            // Sync our changes, this will also cover the second sync mode
            syncStore();
        } catch (DbxException | NullPointerException e) {
            Log.d(TAG, "Writing settings to local store failed", e);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_THEME_COLOR))
            try {
                updateThemeColorSettingInDataStore();

                syncStore();
            } catch (DbxException | NullPointerException e) {
                Log.d(TAG, "Theme color changed, but cannot be written to data store", e);
            }
    }

    @Override
    protected void onSyncStoreComplete() {
        super.onSyncStoreComplete();

        // Update the local color setting with remote value
        if (SyncMode.SEND_RECEIVE.equals(mode))
            try {
                final DbxRecord colorRecord = settingsTable.get(themeColorRecordId);

                if (colorRecord != null && colorRecord.hasField(KEY_THEME_COLOR))
                    preferences.edit()
                            .putInt(KEY_THEME_COLOR, (int) colorRecord.getLong(KEY_THEME_COLOR))
                            .apply();
            } catch (DbxException | NullPointerException e) {
                Log.d(TAG, "Receiving settings from data store failed", e);
            }
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();

        preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateThemeColorSettingInDataStore() throws DbxException {
        settingsTable.getOrInsert(themeColorRecordId).set(KEY_THEME_COLOR,
                preferences.getInt(KEY_THEME_COLOR, defaultThemeColor));
    }
}
