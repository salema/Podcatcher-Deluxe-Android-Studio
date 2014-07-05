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

package com.podcatcher.deluxe;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.podcatcher.deluxe.listeners.OnLoadPodcastListListener;
import com.podcatcher.deluxe.model.tasks.LoadPodcastListTask;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.List;

/**
 * Activity that imports podcasts from an OPML file.
 */
public class ImportOpmlActivity extends BaseActivity implements OnLoadPodcastListListener {

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Only do this on initial creation in order to avoid multiple file
        // selection dialogs if the user rotates the device while active
        if (savedInstanceState == null) {
            // For Android version 4.4 and newer, we use the storage access
            // framework for file selection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                final Intent openIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                openIntent.addCategory(Intent.CATEGORY_OPENABLE);
                openIntent.setType("*/*");

                startActivityForResult(openIntent, 42);
            }
            // We are on a device with Android < 4.4, use the app's own file
            // selection dialog (local files only)
            else {
                final Intent selectFolderIntent = new Intent(this, SelectFileActivity.class);
                startActivityForResult(selectFolderIntent, 42);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (RESULT_OK == resultCode) {
            // We need the result to contain a file URI
            if (result != null && result.getData() != null) {
                // Create and configure the import task
                final LoadPodcastListTask importTask = new LoadPodcastListTask(this, this);
                importTask.setCustomLocation(result.getData());

                importTask.execute();
            } else
                // This should not happen...
                showToast(getString(R.string.opml_import_failed));
        }

        // Make sure we finish here
        finish();
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList, Uri location) {
        // Add all podcasts to the list
        for (Podcast podcast : podcastList)
            podcastManager.addPodcast(podcast);
    }

    @Override
    public void onPodcastListLoadFailed(Uri inputFile, Exception error) {
        showToast(getString(R.string.opml_import_failed));
    }
}
