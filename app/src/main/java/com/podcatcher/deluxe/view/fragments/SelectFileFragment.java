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
import com.podcatcher.deluxe.SelectFileActivity.SelectionMode;
import com.podcatcher.deluxe.adapters.FileListAdapter;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

/**
 * Fragment dialog for file and folder selection.
 */
public class SelectFileFragment extends DialogFragment {

    /**
     * The key to store the selected position under
     */
    private static final String SELECTED_POSITION_KEY = "selected_position";

    /**
     * The file list view adapter
     */
    private FileListAdapter fileListAdapter;
    /**
     * The path we are currently showing
     */
    private File currentPath;
    /**
     * The current selection mode
     */
    private SelectionMode selectionMode = SelectionMode.FILE;
    /**
     * The currently selected position in list (used only for file selection)
     */
    private int selectedPosition = -1;
    /**
     * The current path view
     */
    private TextView currentPathView;
    /**
     * The up button
     */
    private ImageButton upButton;
    /**
     * The file list view
     */
    private ListView fileListView;
    /**
     * The select button
     */
    private Button selectButton;

    /**
     * Status flag indicating that our view is created
     */
    private boolean viewCreated = false;

    /**
     * The call back we work on
     */
    private SelectFileDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (SelectFileDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SelectFileDialogListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
            this.selectedPosition = savedInstanceState.getInt(SELECTED_POSITION_KEY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        final View layout = inflater.inflate(R.layout.file_list, container, false);

        // Get the display dimensions
        Rect displayRectangle = new Rect();
        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

        // Adjust the layout minimum height so the dialog always has the same
        // height and does not bounce around depending on the list content
        layout.setMinimumHeight((int) (displayRectangle.height() * 0.9f));

        return layout;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        updateDialogTitle();

        currentPathView = (TextView) view.findViewById(R.id.current_path);
        upButton = (ImageButton) view.findViewById(R.id.path_up);
        upButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                final File up = currentPath.getParentFile();

                // Switch to parent if not root
                if (up != null) {
                    updateSelection(-1);
                    setPath(up);
                }
            }
        });

        fileListView = (ListView) view.findViewById(R.id.files);
        fileListView.setEmptyView(view.findViewById(android.R.id.empty));
        fileListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final File subFile = (File) fileListView.getItemAtPosition(position);

                // Switch down to sub directory
                if (subFile.isDirectory()) {
                    updateSelection(-1);
                    setPath(subFile);
                } else
                    updateSelection(position);
            }
        });

        selectButton = (Button) view.findViewById(R.id.select_file);
        selectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (SelectionMode.FOLDER.equals(selectionMode))
                    listener.onFileSelected(currentPath);
                else if (selectedPosition >= 0)
                    listener.onFileSelected((File) fileListView.getItemAtPosition(selectedPosition));
            }
        });

        viewCreated = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        updateDialogTitle();

        if (currentPath != null) {
            setPath(currentPath);

            updateSelection(selectedPosition);
            fileListView.smoothScrollToPosition(selectedPosition);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SELECTED_POSITION_KEY, selectedPosition);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        viewCreated = false;
        listener.onCancel(dialog);

        super.onCancel(dialog);
    }

    /**
     * Set the directory path for the fragment to show. You can call this
     * anytime and assume the latest call to take effect {@link #onResume()}.
     *
     * @param path The directory to show content for. Cannot be a file and has
     *             to exist. Do not set to <code>null</code>.
     */
    public void setPath(File path) {
        this.currentPath = path;

        if (viewCreated && path != null && path.isDirectory() && path.exists()) {
            if (path.canRead()) {
                currentPathView.setText(path.getAbsolutePath());
                upButton.setEnabled(path.getParent() != null);

                if (fileListAdapter == null) {
                    fileListAdapter = new FileListAdapter(getDialog().getContext(), path);
                    fileListView.setAdapter(fileListAdapter);
                } else
                    fileListAdapter.setPath(path);

                selectButton.setEnabled(SelectionMode.FOLDER.equals(selectionMode));

                listener.onDirectoryChanged(path);
            } else
                listener.onAccessDenied(path);
        }
    }

    /**
     * Set the selection mode. You can call this anytime and assume the latest
     * call to take effect {@link #onResume()}.
     *
     * @param selectionMode The selection mode to use (file or folder)
     * @see SelectionMode
     */
    public void setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode;

        if (viewCreated)
            updateDialogTitle();
    }

    private void updateSelection(int position) {
        if (SelectionMode.FILE.equals(selectionMode)) {
            // Mark file as selected
            selectedPosition = position;

            if (viewCreated && fileListAdapter != null) {
                selectButton.setEnabled(position >= 0);
                fileListAdapter.setSelectedPosition(position);
            }
        }
    }

    private void updateDialogTitle() {
        if (SelectionMode.FOLDER.equals(selectionMode))
            getDialog().setTitle(R.string.file_select_folder);
        else
            getDialog().setTitle(R.string.file_select_file);
    }

    /**
     * Interface definition for a callback to be invoked when an file or folder
     * is selected by the user in the select file dialog.
     */
    public interface SelectFileDialogListener extends OnCancelListener {

        /**
         * A file/folder was selected by the user in the dialog.
         *
         * @param selectedFile The file/folder selected.
         */
        void onFileSelected(File selectedFile);

        /**
         * The current folder set in the file dialog changed.
         *
         * @param path The new path.
         */
        void onDirectoryChanged(File path);

        /**
         * The user tried to navigate to an unavailable path.
         *
         * @param path The path.
         */
        void onAccessDenied(File path);
    }
}
