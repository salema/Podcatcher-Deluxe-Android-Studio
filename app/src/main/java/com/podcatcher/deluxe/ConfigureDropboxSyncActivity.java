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
import android.content.Intent;
import android.os.Bundle;

import com.dropbox.sync.android.DbxAccountManager;

/**
 * Non-UI activity to configure the Dropbox synchronization settings.
 */
public class ConfigureDropboxSyncActivity extends BaseActivity implements ConfirmSyncUnlinkDialogListener {

    /**
     * Our account manager handle
     */
    private DbxAccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.accountManager = DropboxSyncController.getAccountManager(this);

        if (savedInstanceState == null) {
            // Toggle link/unlink depending on current state
            if (accountManager.hasLinkedAccount()) {
                // Show confirmation dialog, action occurs in call-back implementations below
                final ConfirmSyncUnlinkFragment dialog = new ConfirmSyncUnlinkFragment();
                dialog.setController(ControllerImpl.DROPBOX);

                dialog.show(getFragmentManager(), null);
            } else
                accountManager.startLink(this, 42);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setResult(resultCode);
        finish();
    }

    @Override
    public void onConfirmUnlink() {
        syncManager.setSyncMode(ControllerImpl.DROPBOX, null);
        accountManager.unlink();

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}
