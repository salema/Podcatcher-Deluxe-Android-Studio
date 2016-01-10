/**
 * Copyright 2012-2016 Kevin Hausmann
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
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.view.SyncListItemView;
import com.podcatcher.deluxe.view.fragments.ConfigureSyncFragment.ConfigureSyncDialogListener;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * The sync list adapter to provide the data for the list of sync controllers
 * available.
 */
public class SyncListAdapter extends PodcatcherBaseAdapter {

    /**
     * The callback to invoke if the user interacts with list items
     */
    private final ConfigureSyncDialogListener listener;
    /**
     * The {@link SyncManager} handle
     */
    private final SyncManager syncManager;

    /**
     * Our list of available sync controller implementations
     */
    private final List<ControllerImpl> impls;

    /**
     * Create new adapter.
     *
     * @param context  Context we live in.
     * @param listener The listener for user interaction.
     */
    public SyncListAdapter(Context context, ConfigureSyncDialogListener listener) {
        super(context);

        this.listener = listener;
        this.syncManager = SyncManager.getInstance();

        // Create the list of available sync controller implementation for the
        // environment we live in
        this.impls = new ArrayList<>(ControllerImpl.values().length);
        for (ControllerImpl impl : ControllerImpl.values())
            if (impl.isAvailable(context))
                impls.add(impl);
    }

    @Override
    public int getCount() {
        return impls.size();
    }

    @Override
    public Object getItem(int position) {
        return impls.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final SyncListItemView returnView = (SyncListItemView)
                findReturnView(convertView, parent, R.layout.sync_list_item);

        final ControllerImpl impl = impls.get(position);

        // Make the view represent controller at given position
        returnView.show(impl, impl.isLinked(context), syncManager.getSyncMode(impl), listener);

        return returnView;
    }
}
