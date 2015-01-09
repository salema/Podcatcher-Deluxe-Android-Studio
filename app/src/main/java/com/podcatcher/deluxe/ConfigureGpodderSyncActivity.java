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

import android.content.DialogInterface;
import android.os.Bundle;

import com.dragontek.mygpoclient.api.MygPodderClient;
import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.model.sync.gpodder.GpodderSyncController;
import com.podcatcher.deluxe.view.fragments.GpodderSyncConfigFragment;
import com.podcatcher.deluxe.view.fragments.GpodderSyncConfigFragment.ConfigureGpodderSyncDialogListener;

/**
 * Non-UI activity to configure the gpodder synchronization settings.
 */
public class ConfigureGpodderSyncActivity extends BaseActivity implements
        ConfigureGpodderSyncDialogListener {

    /**
     * Tag to find the gpodder sync config dialog fragment under
     */
    private static final String GPODDER_SYNC_CONFIG_DIALOG_TAG = "gpodder_sync_config_dialog";
    /**
     * The fragment containing the configuration UI
     */
    private GpodderSyncConfigFragment configFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create and show dialog fragment
        if (savedInstanceState == null) {
            this.configFragment = new GpodderSyncConfigFragment();
            configFragment.show(getFragmentManager(), GPODDER_SYNC_CONFIG_DIALOG_TAG);
        } else
            this.configFragment = (GpodderSyncConfigFragment)
                    getFragmentManager().findFragmentByTag(GPODDER_SYNC_CONFIG_DIALOG_TAG);
    }

    @Override
    public void onSubmitConfiguration(final String username, final String password,
                                      final String deviceId) {
        // The device id is fine (checked by fragment), make sure we keep that
        preferences.edit().putString(GpodderSyncController.DEVICE_ID_KEY, deviceId).apply();

        // We also need to check the user/pass combination, this has to be
        // done off the main thread because we need to connect to the
        // gpodder.net service.
        // Show progress bar in dialog while checking
        configFragment.showProgress(true, false);
        // Create gpodder.net client and run auth check off-thread
        final MygPodderClient client = new MygPodderClient(username, password);
        new Thread() {
            public void run() {
                if (client.authenticate(username, password)) {
                    // All fine, store the credentials
                    preferences.edit()
                            .putString(GpodderSyncController.USERNAME_KEY, username)
                            .putString(GpodderSyncController.PASSWORD_KEY, password).apply();

                    // Reset the current sync controller (if any)
                    syncManager.setSyncMode(ControllerImpl.GPODDER, null);

                    // Close dialog and end activity
                    configFragment.dismiss();
                    setResult(RESULT_OK);
                    finish();
                } else
                    // Hide progress bar in dialog and show error
                    runOnUiThread(new Runnable() {
                        public void run() {
                            configFragment.showProgress(false, true);
                        }
                    });
            }
        }.start();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
