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

package com.podcatcher.deluxe.view.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.adapters.SyncListAdapter;
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.model.sync.SyncController;
import com.podcatcher.deluxe.model.sync.SyncController.SyncMode;

import java.util.Date;

/**
 * Dialog fragment for sync configuration.
 */
public class ConfigureSyncFragment extends DialogFragment {

    /**
     * The sync list adapter
     */
    private SyncListAdapter syncListAdapter;
    /**
     * Our sync manager handle
     */
    private SyncManager syncManager;

    /**
     * The last sync run label
     */
    private TextView lastRunTextView;
    /**
     * The sync controller list view
     */
    private ListView syncListView;
    /**
     * The help button
     */
    private Button helpButton;
    /**
     * The sync now button
     */
    private Button syncNowButton;

    /**
     * Status flag indicating that our view is created
     */
    private boolean viewCreated = false;

    /**
     * The call back we work on
     */
    private ConfigureSyncDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (ConfigureSyncDialogListener) activity;
            this.syncManager = SyncManager.getInstance();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ConfigureSyncDialogListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout
        return inflater.inflate(R.layout.sync_list, container, false);
    }

    ;

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.pref_sync_title);

        lastRunTextView = (TextView) view.findViewById(R.id.last_sync);

        syncListAdapter = new SyncListAdapter(getDialog().getContext(), listener);
        syncListView = (ListView) view.findViewById(R.id.sync_controllers);
        syncListView.setAdapter(syncListAdapter);

        helpButton = (Button) view.findViewById(R.id.sync_help_button);
        helpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onShowHelp();
            }
        });

        syncNowButton = (Button) view.findViewById(R.id.sync_now_button);
        syncNowButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onSyncNow();
            }
        });

        viewCreated = true;
        refresh();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        viewCreated = false;

        // Make sure the parent activity knows when we are closing
        listener.onCancel(dialog);

        super.onCancel(dialog);
    }

    /**
     * Refresh the complete dialog UI, including the sync status UI
     */
    public void refresh() {
        if (viewCreated) {
            syncListAdapter.notifyDataSetChanged();

            updateLastSyncRunLabel();
            updateSyncButton();
        }
    }

    private void updateLastSyncRunLabel() {
        final Date lastSyncDate = syncManager.getLastFullSyncDate();

        if (lastSyncDate != null) {
            final String dateString = getString(R.string.pref_sync_last_run,
                    DateUtils.getRelativeTimeSpanString(lastSyncDate.getTime(),
                            System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
            );

            lastRunTextView.setText(dateString);
            lastRunTextView.setVisibility(View.VISIBLE);
        } else
            lastRunTextView.setVisibility(View.GONE);
    }

    private void updateSyncButton() {
        syncNowButton.setEnabled(!syncManager.isSyncRunning()
                && syncManager.getActiveControllerCount() > 0);
    }

    /**
     * Interface definition for a callback to be invoked when sync settings are
     * changed in the dialog.
     */
    public interface ConfigureSyncDialogListener extends OnCancelListener {

        /**
         * Called on the listener when the user request the
         * {@link SyncController}'s settings to be displayed (and possibly
         * changed).
         *
         * @param impl The controller to present settings for.
         */
        public void onUpdateSettings(ControllerImpl impl);

        /**
         * Called on the listener when the user set the {@link SyncMode} for a
         * {@link SyncController}.
         *
         * @param impl The controller mode is set for.
         * @param mode The new sync mode. By giving <code>null</code> here, the
         *             controller is disabled.
         */
        public void onUpdateMode(ControllerImpl impl, SyncMode mode);

        /**
         * Called on the listener when the user wants to see the help screen.
         */
        public void onShowHelp();

        /**
         * Called on the listener when the user triggered a sync all event.
         */
        public void onSyncNow();
    }
}
