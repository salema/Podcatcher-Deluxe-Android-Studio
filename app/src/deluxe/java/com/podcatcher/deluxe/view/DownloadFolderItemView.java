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

package com.podcatcher.deluxe.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.adapters.PreferredDownloadFolderAdapter;

import java.io.File;

/**
 * A list item view to represent a download folder.
 */
public class DownloadFolderItemView extends RelativeLayout {

    /**
     * The folder icon view
     */
    private ImageView iconView;
    /**
     * The name text view
     */
    private TextView nameTextView;
    /**
     * The name text view
     */
    private TextView pathTextView;

    /**
     * Create a download folder item list view.
     *
     * @param context Context for the view to live in.
     * @param attrs   View attributes.
     */
    public DownloadFolderItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        iconView = (ImageView) findViewById(R.id.download_folder_icon);
        nameTextView = (TextView) findViewById(R.id.download_folder_name);
        pathTextView = (TextView) findViewById(R.id.download_folder_path);
    }

    /**
     * Make the view update all its children to represent input given.
     *
     * @param folder The download folder to represent (enum value)
     * @param file   The actual filesystem object to retrieve path from
     */
    public void show(PreferredDownloadFolderAdapter.PreferredDownloadFolder folder, File file) {
        switch (folder) {
            case INTERNAL_PODCASTS:
                iconView.setImageResource(R.drawable.ic_internal_storage);
                nameTextView.setText(R.string.pref_download_folder_internal_podcasts);
                break;
            case INTERNAL_DOWNLOADS:
                iconView.setImageResource(R.drawable.ic_internal_storage);
                nameTextView.setText(R.string.pref_download_folder_internal_downloads);
                break;
            case INTERNAL_APP:
                iconView.setImageResource(R.drawable.ic_internal_storage);
                nameTextView.setText(R.string.pref_download_folder_internal_app);
                break;
            case SDCARD:
                iconView.setImageResource(R.drawable.ic_sd_card);
                nameTextView.setText(R.string.pref_download_folder_sdcard);
                break;
            case SDCARD_APP:
                iconView.setImageResource(R.drawable.ic_sd_card);
                nameTextView.setText(R.string.pref_download_folder_sdcard_app);
                break;
        }

        pathTextView.setText(file.getAbsolutePath());
    }
}
