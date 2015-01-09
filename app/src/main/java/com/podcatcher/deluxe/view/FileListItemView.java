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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.podcatcher.deluxe.R;

import java.io.File;

/**
 * A list item view to represent a file/folder.
 */
public class FileListItemView extends LinearLayout {

    /**
     * The icon view
     */
    private ImageView iconView;
    /**
     * The name text view
     */
    private TextView nameTextView;

    /**
     * Create a file item list view.
     *
     * @param context Context for the view to live in.
     * @param attrs   View attributes.
     */
    public FileListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        iconView = (ImageView) findViewById(R.id.file_icon);
        nameTextView = (TextView) findViewById(R.id.file_name);
    }

    /**
     * Make the view update all its child to represent input given.
     *
     * @param file File to represent.
     */
    public void show(final File file) {
        // 1. Set icon
        iconView.setImageResource(
                file.isDirectory() ? R.drawable.ic_file_folder : R.drawable.ic_file);

        // 2. Set the file name as text
        nameTextView.setText(file.getName());
    }
}
