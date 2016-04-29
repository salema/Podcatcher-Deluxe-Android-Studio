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
import com.podcatcher.deluxe.model.sync.dropbox.DropboxSyncController;
import com.podcatcher.deluxe.view.fragments.ConfirmSyncUnlinkFragment;
import com.podcatcher.deluxe.view.fragments.ConfirmSyncUnlinkFragment.ConfirmSyncUnlinkDialogListener;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;

import com.dropbox.core.android.Auth;

/**
 * Non-UI activity to configure the Dropbox synchronization settings.
 */
public class ConfigureDropboxSyncActivity extends BaseActivity implements ConfirmSyncUnlinkDialogListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Toggle link/unlink depending on current state
            if (preferences.contains(DropboxSyncController.ACCESS_TOKEN)) {
                // Show confirmation dialog, action occurs in call-back implementations below
                final ConfirmSyncUnlinkFragment dialog = new ConfirmSyncUnlinkFragment();
                dialog.setController(ControllerImpl.DROPBOX);

                dialog.show(getFragmentManager(), null);
            } else
                Auth.startOAuth2Authentication(this, getString(R.string.dropbox_appkey));
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        final String token = Auth.getOAuth2Token();
        if (token != null && !preferences.contains(DropboxSyncController.ACCESS_TOKEN))
            preferences.edit().putString(DropboxSyncController.ACCESS_TOKEN, token).apply();

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onConfirmUnlink() {
        syncManager.setSyncMode(ControllerImpl.DROPBOX, null);
        preferences.edit().remove(DropboxSyncController.ACCESS_TOKEN).apply();

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
