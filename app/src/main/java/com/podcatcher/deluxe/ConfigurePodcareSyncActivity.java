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

package com.podcatcher.deluxe;

import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.model.sync.podcare.PodcareSyncController;
import com.podcatcher.deluxe.view.fragments.ConfirmSyncUnlinkFragment;
import com.podcatcher.deluxe.view.fragments.PodcareSyncConfigFragment;
import com.podcatcher.labs.sync.podcare.PodcareClient;
import com.podcatcher.labs.sync.podcare.PodcareException;
import com.podcatcher.labs.sync.podcare.auth.IntentIntegrator;
import com.podcatcher.labs.sync.podcare.auth.IntentResult;
import com.podcatcher.labs.sync.podcare.callbacks.OnConnectListener;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import static com.podcatcher.deluxe.BuildConfig.DEBUG;
import static com.podcatcher.deluxe.Podcatcher.userAgentValue;

/**
 * Non-UI activity to configure the Podcare synchronization settings.
 */
public class ConfigurePodcareSyncActivity extends BaseActivity implements
        ConfirmSyncUnlinkFragment.ConfirmSyncUnlinkDialogListener {

    /**
     * Prefix from QR code for the Podcare connect key.
     */
    private static final String PODCARE_CONNECT_KEY = "pod.care-connect-key";
    /**
     * Our Podcare client.
     */
    private PodcareClient podcare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.podcare = new PodcareClient(getString(R.string.podcare_api_key), userAgentValue, DEBUG);

        if (savedInstanceState == null) {
            // Toggle link/unlink depending on current state
            if (preferences.contains(PodcareSyncController.CONNECT_ID_KEY)) {
                // Show confirmation dialog, action occurs in call-back implementations below
                final ConfirmSyncUnlinkFragment dialog = new ConfirmSyncUnlinkFragment();
                dialog.setController(ControllerImpl.PODCARE);

                dialog.show(getFragmentManager(), null);
            } else {
                // Show configuration dialog for Podcare
                final PodcareSyncConfigFragment dialog = new PodcareSyncConfigFragment();
                dialog.show(getFragmentManager(), null);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            final IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            final String connectKey = new JSONObject(scanResult.getContents()).getString(PODCARE_CONNECT_KEY);

            podcare.connectAsync(connectKey, new OnConnectListener() {

                @Override
                public void onConnect(@NonNull String connectId) {
                    preferences.edit().putString(PodcareSyncController.CONNECT_ID_KEY, connectId).apply();

                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onConnectFailed(PodcareException pe) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
        } catch (JSONException | NullPointerException e) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onConfirmUnlink() {
        syncManager.setSyncMode(ControllerImpl.PODCARE, null);
        //noinspection ConstantConditions Cannot be null since we would not unlink if it was
        podcare.disconnectAsync(preferences.getString(PodcareSyncController.CONNECT_ID_KEY, null), null);
        preferences.edit().remove(PodcareSyncController.CONNECT_ID_KEY).apply();

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
