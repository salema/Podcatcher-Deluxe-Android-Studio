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

package com.podcatcher.deluxe.view.fragments;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.SettingsActivity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.BaseAdapter;

import static com.podcatcher.deluxe.SettingsActivity.KEY_DOWNLOAD_FOLDER;
import static com.podcatcher.deluxe.SettingsActivity.KEY_SYNC_ACTIVE;
import static com.podcatcher.deluxe.SettingsActivity.KEY_SYNC_RECEIVE;

/**
 * Fragment for settings.
 */
public class SettingsFragment extends PreferenceFragment implements
        OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        // Register this fragment to listen to preference changes
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_SYNC_ACTIVE.equals(key) || KEY_SYNC_RECEIVE.equals(key) ||
                KEY_DOWNLOAD_FOLDER.equals(key))
            // Update the preference UIs to reflect new setting
            ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister this fragment
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
