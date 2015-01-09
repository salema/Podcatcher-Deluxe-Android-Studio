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

import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.podcatcher.deluxe.model.EpisodeDownloadManager;
import com.podcatcher.deluxe.preferences.DownloadFolderPreference;
import com.podcatcher.deluxe.view.fragments.SelectDownloadFolderFragment;
import com.podcatcher.deluxe.view.fragments.SelectDownloadFolderFragment.SelectDownloadFolderListener;

import java.io.File;

/**
 * Non-UI activity to select download folder. Will use a fragment to show the
 * selection dialog. Helps users to find preferred download locations on different
 * Android versions.
 */
public class SelectDownloadFolderActivity extends BaseActivity implements SelectDownloadFolderListener {

    /**
     * The podcatcher help web site URL (download anchor)
     */
    private static final String PODCATCHER_HELPSITE_DOWNLOAD = "http://www.podcatcher-deluxe.com/help#download";

    /**
     * Tag to find the select download folder dialog fragment under
     */
    private static final String SELECT_FOLDER_DIALOG_TAG = "select_download_folder_dialog";
    /**
     * The fragment containing the select download folder UI
     */
    private SelectDownloadFolderFragment selectDownloadFolderFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            selectDownloadFolderFragment = new SelectDownloadFolderFragment();
            selectDownloadFolderFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialog);

            // Show the fragment
            selectDownloadFolderFragment.show(getFragmentManager(), SELECT_FOLDER_DIALOG_TAG);
        } else
            this.selectDownloadFolderFragment = (SelectDownloadFolderFragment)
                    getFragmentManager().findFragmentByTag(SELECT_FOLDER_DIALOG_TAG);
    }

    @Override
    public void onSelectFolder(String path) {
        if (selectDownloadFolderFragment != null)
            selectDownloadFolderFragment.dismiss();

        Intent result = new Intent();
        result.setData(Uri.fromFile(new File(path)));

        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onShowHelp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PODCATCHER_HELPSITE_DOWNLOAD)));
        } catch (ActivityNotFoundException e) {
            // We are in a restricted profile without a browser
            showToast(getString(R.string.no_browser));
        }
    }

    @Override
    public void onShowAdvanced() {
        if (selectDownloadFolderFragment != null)
            selectDownloadFolderFragment.dismiss();

        // Create select folder intent
        Intent selectFolderIntent = new Intent(this, SelectFileActivity.class);
        selectFolderIntent.putExtra(SelectFileActivity.SELECTION_MODE_KEY,
                SelectFileActivity.SelectionMode.FOLDER);
        selectFolderIntent.putExtra(SelectFileActivity.INITIAL_PATH_KEY,
                getPreferences(MODE_PRIVATE).getString(SettingsActivity.KEY_DOWNLOAD_FOLDER,
                        EpisodeDownloadManager.getDefaultDownloadFolder().getAbsolutePath()));

        // Start activity. Result will be pushed down to the SettingsActivity.
        startActivityForResult(selectFolderIntent, DownloadFolderPreference.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (resultCode == RESULT_OK && requestCode == DownloadFolderPreference.REQUEST_CODE)
            onSelectFolder(result.getData().getPath());
        else finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(RESULT_CANCELED);
        finish();
    }
}
