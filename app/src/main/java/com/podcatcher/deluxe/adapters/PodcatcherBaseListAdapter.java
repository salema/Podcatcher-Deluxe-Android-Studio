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

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.SparseBooleanArray;
import android.view.View;

/**
 * Abstract super class for this app's list adapters. Handles the
 * selection/choice parts. All lists are single selection and have their
 * background changed for the selected item. Some lists offer a context mode and
 * allow multiple choices when it is activated (see
 * {@link #setCheckedPositions(SparseBooleanArray)}.
 */
public abstract class PodcatcherBaseListAdapter extends PodcatcherBaseAdapter {

    /**
     * We need to know the selected item positions in the list
     */
    protected SparseBooleanArray selectedPositions = new SparseBooleanArray();
    /**
     * Also, there might be checked items
     */
    protected SparseBooleanArray checkedPositions = new SparseBooleanArray();
    /**
     * Flag for whether we are in select all mode
     */
    protected boolean selectAll = false;

    /**
     * Create new adapter.
     *
     * @param context The current context.
     */
    public PodcatcherBaseListAdapter(Context context) {
        super(context);
    }

    /**
     * Set the selected item in the list and updates the UI to reflect the
     * selection.
     *
     * @param position Position selected.
     */
    public void setSelectedPosition(int position) {
        selectAll = false;
        selectedPositions.clear();
        selectedPositions.put(position, true);

        notifyDataSetChanged();
    }

    /**
     * Put adapter in select all mode.
     */
    public void setSelectAll() {
        selectAll = true;
        selectedPositions.clear();

        notifyDataSetChanged();
    }

    /**
     * Put adapter in select none mode.
     */
    public void setSelectNone() {
        selectAll = false;
        selectedPositions.clear();

        notifyDataSetChanged();
    }

    /**
     * Set the chosen items in the list.
     *
     * @param positions The array denoting chosen positions. Give
     *                  <code>null</code> to reset.
     */
    public void setCheckedPositions(SparseBooleanArray positions) {
        if (positions == null)
            checkedPositions = new SparseBooleanArray();
        else
            checkedPositions = positions;

        notifyDataSetChanged();
    }

    /**
     * This sets a views background color according to the selection state of
     * the given position.
     *
     * @param view     View to set background for.
     * @param position Position of the view in the list.
     */
    protected void setBackgroundColorForPosition(View view, int position) {
        // This handles the selected, checked and none states
        view.setBackgroundColor(checkedPositions.get(position) ?
                ContextCompat.getColor(context, R.color.theme_light) : selectedPositions.get(position) ?
                ContextCompat.getColor(context, R.color.theme_dark) : Color.TRANSPARENT);
    }
}
