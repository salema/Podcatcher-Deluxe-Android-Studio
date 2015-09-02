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

package com.podcatcher.deluxe;

import com.podcatcher.deluxe.preferences.DownloadFolderPreference;
import com.podcatcher.deluxe.view.fragments.SettingsFragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Update settings activity.
 */
public class SettingsActivity extends BaseActivity {

    /**
     * The flag for the first run dialog
     */
    public static final String KEY_FIRST_RUN = "first_run";

    /**
     * The select all podcast on start-up preference key
     */
    public static final String KEY_SELECT_ALL_ON_START = "select_all_on_startup";
    /**
     * The preference key for the auto download flag
     */
    public static final String KEY_AUTO_DOWNLOAD = "auto_download";
    /**
     * The preference key for the auto delete flag
     */
    public static final String KEY_AUTO_DELETE = "auto_delete";
    /**
     * The key for the download folder preference
     */
    public static final String KEY_DOWNLOAD_FOLDER = "download_folder";

    /**
     * Setting key for the sync receive field
     */
    public static final String KEY_SYNC_RECEIVE = "receive_controller";
    /**
     * Setting key for the sync field
     */
    public static final String KEY_SYNC_ACTIVE = "enabled_sync_controllers";
    /**
     * Setting key for the last sync field
     */
    public static final String KEY_LAST_SYNC = "last_full_sync";

    /**
     * Tag to find the add suggestion dialog fragment under
     */
    private static final String SETTINGS_DIALOG_TAG = "settings_dialog";
    /**
     * Permission request code
     */
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 43;

    /**
     * The settings fragment we display
     */
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.preferences);

        // Create and show suggestion fragment
        if (savedInstanceState == null) {
            // Create the fragment to show
            this.settingsFragment = new SettingsFragment();
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, settingsFragment, SETTINGS_DIALOG_TAG)
                    .commit();
        } else
            this.settingsFragment = (SettingsFragment)
                    getFragmentManager().findFragmentByTag(SETTINGS_DIALOG_TAG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        // This is used to fetch the result from the select folder dialog. The
        // result is forwarded to the preference object via the fragment.
        if (resultCode == RESULT_OK && requestCode == DownloadFolderPreference.REQUEST_CODE)
            try {
                final File downloadFolder = new File(result.getData().getPath());
                final DownloadFolderPreference folderPreference = (DownloadFolderPreference)
                        settingsFragment.findPreference(KEY_DOWNLOAD_FOLDER);

                // Make sure we can actually write to this folder
                // noinspection ResultOfMethodCallIgnored
                downloadFolder.mkdirs();
                // noinspection ResultOfMethodCallIgnored
                File.createTempFile("test", "tmp", downloadFolder).delete();

                // Update the preference
                folderPreference.update(downloadFolder);
            } catch (IOException e) {
                showToast(getString(R.string.file_select_access_denied));
            } catch (NullPointerException npe) {
                // pass, this should not happen
            }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);

        // Make sure we have the correct permissions to fulfill the auto tasks, since
        // these are disabled when the permission is taken away, we can count on this
        // to only run if one of the auto tasks is enabled and not on disable.
        if (!Podcatcher.canWriteExternalStorage() &&
                (KEY_AUTO_DELETE.equals(key) || KEY_AUTO_DOWNLOAD.equals(key)))
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case STORAGE_PERMISSION_REQUEST_CODE:
                if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // Reset preferences to default state (off)
                    preferences.edit()
                            .putBoolean(SettingsActivity.KEY_AUTO_DELETE, false)
                            .putBoolean(SettingsActivity.KEY_AUTO_DOWNLOAD, false).apply();

                    showToast(getString(R.string.file_select_access_denied));
                }
        }
    }
}
