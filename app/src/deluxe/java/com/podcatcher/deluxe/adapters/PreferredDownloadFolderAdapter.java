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

package com.podcatcher.deluxe.adapters;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.view.DownloadFolderItemView;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.DIRECTORY_PODCASTS;

/**
 * Adapter class used for the list of preferred download folders.
 */
public class PreferredDownloadFolderAdapter extends PodcatcherBaseAdapter {

    /**
     * The enum of download folder we want to show to the user as potentially good options.
     */
    public enum PreferredDownloadFolder {
        /**
         * The public "Podcasts" folder (always available)
         */
        INTERNAL_PODCASTS,

        /**
         * The public "Downloads" folder (always available)
         */
        INTERNAL_DOWNLOADS,

        /**
         * The private app folder on the internal storage (always available)
         */
        INTERNAL_APP,

        /**
         * The SD card generic podcast folder, only available if SD card is present
         */
        SDCARD,

        /**
         * The private app folder on the SD card, only available if SD card is present
         */
        SDCARD_APP;

        // Potential path to the external sd card
        private String[] sdCardPaths = {"/mnt/extSdCard", "/storage/extSdCard",
                "/mnt/external_sd", "/storage/external_sd", "/mnt/ext_sd", "/storage/ext_sd",
                "/mnt/sdcard/ext_sd", "/storage/sdcard/ext_sd", "/mnt/external", "/storage/external",
                "/mnt/sdcard/external_sd", "/storage/sdcard/external_sd"};

        /**
         * Find the file object represented by the enum values. The method might access
         * the file system and should therefore not be called from the main thread.
         *
         * @param context The app's context.
         * @return The file object (path) represented by the enum value
         * or <code>null</code> if the enum value does not translate to
         * a valid file under the current conditions (e.g. no SD card).
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public File getFile(Context context) {
            switch (this) {
                case INTERNAL_PODCASTS:
                    return Environment.getExternalStoragePublicDirectory(DIRECTORY_PODCASTS);
                case INTERNAL_DOWNLOADS:
                    return Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS);
                case INTERNAL_APP:
                    final File internalAppFolder = context.getExternalFilesDir(DIRECTORY_PODCASTS);
                    // We prefer /Android/media over /Android/data on API >= 21,
                    // because it is scanned by the media indexer of the Android system
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        final File[] dirs = context.getExternalMediaDirs();
                        return dirs != null && dirs.length > 0 ? dirs[0] : internalAppFolder;
                    } else return internalAppFolder;
                case SDCARD:
                    // Check all paths and select the first that exists
                    for (String path : sdCardPaths) {
                        final File sdCardRoot = new File(path);
                        final File sdCardPodcasts = new File(sdCardRoot, "Podcasts");

                        if (sdCardRoot.exists()
                                && (sdCardPodcasts.isDirectory() || sdCardPodcasts.mkdir()))
                            return sdCardPodcasts;
                    }

                    return null;
                case SDCARD_APP:
                    // The second folder ([1]) should be the one on the sd card,
                    // only show on KITKAT and later
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // We prefer /Android/media over /Android/data on API >= 21,
                        // because it is scanned by the media indexer of the Android system
                        final File[] dirs = context.getExternalMediaDirs();
                        return dirs != null && dirs.length > 1 ? dirs[1] : null;
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        final File[] dirs = context.getExternalFilesDirs(DIRECTORY_PODCASTS);
                        return dirs != null && dirs.length > 1 ? dirs[1] : null;
                    } else return null;
                default:
                    return null;
            }
        }

        /**
         * @return Whether this download folder option is the one recommended the most.
         */
        public boolean isRecommended() {
            return INTERNAL_PODCASTS.equals(this);
        }
    }

    /**
     * Walk through the preferred download folder enum,
     * test them in context and add them to the list
     */
    private class PopulateListTask extends AsyncTask<Void, Map.Entry<PreferredDownloadFolder, File>, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            for (PreferredDownloadFolder preferredFolder : PreferredDownloadFolder.values()) {
                final File fileObject = preferredFolder.getFile(context);

                if (fileObject != null &&
                        // Always include the recommended option, folder will be created
                        // by the SettingsActivity if selected and not created yet
                        (preferredFolder.isRecommended() || canWriteToFolder(fileObject)))
                    //noinspection unchecked
                    publishProgress(new AbstractMap.SimpleEntry<>(preferredFolder, fileObject));
            }

            return null;
        }

        @SafeVarargs
        @Override
        protected final void onProgressUpdate(Map.Entry<PreferredDownloadFolder, File>... values) {
            folders.add(values[0]);
            notifyDataSetChanged();
        }

        private boolean canWriteToFolder(File folder) {
            try {
                return File.createTempFile("42monkey", "tmp", folder).delete();
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * The folders that are showing to the user for selection.
     */
    private List<Map.Entry<PreferredDownloadFolder, File>> folders = new ArrayList<>();

    /**
     * Create new adapter.
     *
     * @param context The activity.
     */
    public PreferredDownloadFolderAdapter(final Context context) {
        super(context);

        // Go async since we test all folders against the file system
        new PopulateListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    @Override
    public int getCount() {
        return folders.size();
    }

    @Override
    public Object getItem(int position) {
        return folders.get(position).getValue().getAbsolutePath();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final DownloadFolderItemView returnView = (DownloadFolderItemView)
                findReturnView(convertView, parent, R.layout.download_folder_list_item);

        // Make the view represent folder at given position
        returnView.show(folders.get(position).getKey(), folders.get(position).getValue());

        return returnView;
    }
}
