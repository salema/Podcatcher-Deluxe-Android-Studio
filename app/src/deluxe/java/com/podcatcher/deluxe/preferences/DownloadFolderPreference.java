/** Copyright 2012-2014 Kevin Hausmann
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

package com.podcatcher.deluxe.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;

import com.podcatcher.deluxe.SelectDownloadFolderActivity;
import com.podcatcher.deluxe.model.EpisodeDownloadManager;

import java.io.File;

/**
 * The custom download folder preference. Shows folder selection dialog when
 * clicked.
 */
public class DownloadFolderPreference extends Preference {

    /**
     * Our request code for the folder selection dialog
     */
    public static final int REQUEST_CODE = 99;

    /**
     * Currently set download folder
     */
    private File downloadFolder;

    /**
     * Create new preference.
     *
     * @param context Context the preference lives in.
     * @param attrs   Values from the XML.
     */
    public DownloadFolderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        // We want to init this before any other method is called to avoid a
        // situation where downloadFolder == null
        onSetInitialValue(false, null);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        // The default is the public podcast directory
        downloadFolder = new File(getPersistedString(
                EpisodeDownloadManager.getDefaultDownloadFolder().getAbsolutePath()));
    }

    @Override
    protected void onClick() {
        Intent selectFolderIntent = new Intent(getContext(), SelectDownloadFolderActivity.class);
        ((Activity) getContext()).startActivityForResult(selectFolderIntent, REQUEST_CODE);
    }

    @Override
    public CharSequence getSummary() {
        return downloadFolder.getAbsolutePath();
    }

    /**
     * Set new value for the preference.
     *
     * @param newFolder Updated folder to use (not <code>null</code>).
     */
    public void update(File newFolder) {
        if (newFolder != null) {
            this.downloadFolder = newFolder;
            persistString(newFolder.getAbsolutePath());
        }
    }
}
