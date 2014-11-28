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

package com.podcatcher.deluxe.view.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.adapters.PreferredDownloadFolderAdapter;

/**
 * Dialog fragment for download folder selection.
 */
public class SelectDownloadFolderFragment extends DialogFragment {

    /**
     * The preferred folder list adapter
     */
    private PreferredDownloadFolderAdapter preferredDownloadFolderAdapter;

    /**
     * The folder list view
     */
    private ListView downloadFolderListView;
    /**
     * The help button
     */
    private Button helpButton;
    /**
     * The advanced button
     */
    private Button advancedButton;

    /**
     * The call back we work on
     */
    private SelectDownloadFolderListener listener;

    /**
     * Interface definition for a callback to be invoked when download folder
     * settings are changed in the dialog.
     */
    public interface SelectDownloadFolderListener extends OnCancelListener {

        /**
         * Called on the listener when the user selected a folder.
         *
         * @param path The absolute path to the download folder selected.
         */
        public void onSelectFolder(String path);

        /**
         * Called on the listener when the user wants to see the help screen.
         */
        public void onShowHelp();

        /**
         * Called on the listener when the user wants to select a folder manually.
         */
        public void onShowAdvanced();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (SelectDownloadFolderListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SelectDownloadFolderListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.download_folder_list, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.pref_download_folder_title);

        preferredDownloadFolderAdapter = new PreferredDownloadFolderAdapter(getDialog().getContext());
        downloadFolderListView = (ListView) view.findViewById(R.id.download_folder_list);
        downloadFolderListView.setAdapter(preferredDownloadFolderAdapter);

        downloadFolderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listener.onSelectFolder(preferredDownloadFolderAdapter.getItem(position).toString());
            }
        });

        helpButton = (Button) view.findViewById(R.id.download_folder_help_button);
        helpButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onShowHelp();
            }
        });

        advancedButton = (Button) view.findViewById(R.id.download_folder_advanced_button);
        advancedButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onShowAdvanced();
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Make sure the parent activity knows when we are closing
        listener.onCancel(dialog);

        super.onCancel(dialog);
    }
}
