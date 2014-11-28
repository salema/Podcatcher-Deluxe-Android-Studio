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

package com.podcatcher.deluxe.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.view.FileListItemView;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

/**
 * The file list adapter to provide the data for the list in the file/folder
 * selection dialog.
 */
public class FileListAdapter extends PodcatcherBaseListAdapter {

    /**
     * The default file filter to apply
     */
    private static final FileFilter filter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden();
        }
    };
    /**
     * The current path items
     */
    private File[] files;

    /**
     * Create new adapter. Sub-files of given path will be sorted. Hidden files
     * are excluded.
     *
     * @param context Context we live in.
     * @param path    Path to represent children of.
     */
    public FileListAdapter(Context context, File path) {
        super(context);

        readFilesAndFolders(path);
    }

    /**
     * Switch to new folder. Triggers {@link #notifyDataSetChanged()}.
     *
     * @param path Folder to represent.
     */
    public void setPath(File path) {
        readFilesAndFolders(path);

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return files.length;
    }

    @Override
    public Object getItem(int position) {
        return files[position];
    }

    @Override
    public long getItemId(int position) {
        return files[position].hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final FileListItemView returnView = (FileListItemView)
                findReturnView(convertView, parent, R.layout.file_list_item);

        // Make sure the coloring is right
        setBackgroundColorForPosition(returnView, position);
        // Make the view represent file at given position
        returnView.show((File) getItem(position));

        return returnView;
    }

    private void readFilesAndFolders(File path) {
        this.files = path.listFiles(filter);
        Arrays.sort(files);
    }
}
