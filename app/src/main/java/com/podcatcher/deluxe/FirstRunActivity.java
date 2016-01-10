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

import com.podcatcher.deluxe.view.fragments.FirstRunFragment;
import com.podcatcher.deluxe.view.fragments.FirstRunFragment.FirstRunListener;

import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Activity to run on the very first app start. Welcomes the user and gives some
 * hints.
 */
public class FirstRunActivity extends BaseActivity implements FirstRunListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure we only run once
        preferences.edit().putBoolean(SettingsActivity.KEY_FIRST_RUN, false).apply();

        // Create and show the fragment
        if (savedInstanceState == null) {
            final FirstRunFragment firstRunFragment = new FirstRunFragment();
            // Need to set style, because this activity has no UI
            firstRunFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialog);

            firstRunFragment.show(getFragmentManager(), null);
        }
    }

    @Override
    public void onAddPodcasts() {
        finish();

        startActivity(new Intent(this, AddSuggestionActivity.class));
    }

    @Override
    public void onShowHelp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_HELPSITE_ADD)));
        } catch (ActivityNotFoundException e) {
            // We are in a restricted profile without a browser, pass
            showToast(getString(R.string.no_browser));
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
