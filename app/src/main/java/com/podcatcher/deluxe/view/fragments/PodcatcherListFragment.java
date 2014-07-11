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

import android.app.ListFragment;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.adapters.PodcatcherBaseListAdapter;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.view.ProgressView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Generic list fragment sub-class for podcatcher list fragments. Defines some
 * helpers and common functionality.
 */
public abstract class PodcatcherListFragment extends ListFragment {

    /**
     * The list adapter
     */
    protected PodcatcherBaseListAdapter adapter;

    /**
     * The theme color to use for highlighting list items
     */
    protected int themeColor;
    /**
     * The theme color variant to use for pressed and checked items
     */
    protected int lightThemeColor;

    /**
     * The empty view
     */
    protected View emptyView;
    /**
     * The progress bar
     */
    protected ProgressView progressView;

    /**
     * Flags for internal state: show progress
     */
    protected boolean showProgress = false;
    /**
     * Flags for internal state: show error
     */
    protected boolean showLoadFailed = false;
    /**
     * Flags for internal state: select all
     */
    protected boolean selectAll = false;
    /**
     * Member to keep track of current selection
     */
    protected int selectedPosition = -1;

    /**
     * Status flag indicating that our view is created
     */
    private boolean viewCreated = false;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyView = getView().findViewById(R.id.empty);
        progressView = (ProgressView) getView().findViewById(R.id.progress);

        viewCreated = true;
        updateListSelector();
    }

    @Override
    public void onResume() {
        super.onResume();

        updateUiElementVisibility();
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        this.adapter = (PodcatcherBaseListAdapter) adapter;

        // Set theme colors
        if (adapter != null)
            this.adapter.setThemeColors(themeColor, lightThemeColor);

        super.setListAdapter(adapter);
    }

    /**
     * Set the colors to use in the list for selection, checked item etc.
     *
     * @param color        The theme color to use for highlighting list items.
     * @param variantColor The theme color variant to use for pressed and
     *                     checked item.
     */
    public void setThemeColors(int color, int variantColor) {
        this.themeColor = color;
        this.lightThemeColor = variantColor;

        // Set theme colors in adapter
        if (adapter != null)
            this.adapter.setThemeColors(themeColor, lightThemeColor);
        // ...and for the list view
        if (viewCreated)
            updateListSelector();

        refresh();
    }

    /**
     * Select an item.
     *
     * @param position Index of item to select.
     */
    public void select(final int position) {
        selectAll = false;
        selectedPosition = position;

        if (adapter != null && !showProgress)
            adapter.setSelectedPosition(position);

        updateUiElementVisibility();
    }

    /**
     * Select all items.
     */
    public void selectAll() {
        selectAll = true;
        selectedPosition = -1;

        if (adapter != null && !showProgress)
            adapter.setSelectAll();

        updateUiElementVisibility();
    }

    /**
     * Deselect selected item (if any).
     */
    public void selectNone() {
        selectAll = false;
        selectedPosition = -1;

        if (adapter != null && !showProgress)
            adapter.setSelectNone();

        updateUiElementVisibility();
    }

    /**
     * Refresh the list and its views.
     */
    public void refresh() {
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    /**
     * Reset the UI to initial state.
     */
    public void resetUi() {
        reset();

        updateUiElementVisibility();
    }

    /**
     * Show the UI to be working.
     */
    public void resetAndSpin() {
        reset();
        // Show progress should be set to make UI switch
        // to show progress as soon as it is created
        showProgress = true;

        updateUiElementVisibility();
    }

    /**
     * Reset the fragments state. Sub-classes should extends this. Will be
     * called on public reset.
     *
     * @see #resetUi()
     * @see #resetAndSpin()
     */
    protected void reset() {
        showProgress = false;
        showLoadFailed = false;
        selectAll = false;
        selectedPosition = -1;

        setListAdapter(null);
        if (viewCreated)
            progressView.reset();
    }

    /**
     * Update UI with load progress.
     *
     * @param progress Amount loaded or flag from load task.
     */
    public void showProgress(Progress progress) {
        // Only show this if we are visible
        if (viewCreated)
            progressView.publishProgress(progress);
    }

    /**
     * Show error view.
     */
    public void showLoadFailed() {
        showProgress = false;
        showLoadFailed = true;
        selectAll = false;

        updateUiElementVisibility();
    }

    /**
     * Find the view representing the list item at the given index (NOT position
     * in the list widget, but the index in the list backing it).
     *
     * @param index The index of the item in the original data object list.
     * @return The view representing the data object at the given index or
     * <code>null</code> if that object is not represented at the moment
     * (i.e. it is off-screen).
     */
    protected View findListItemViewForIndex(int index) {
        // Adjust the position relative to list scroll state
        final int firstVisiblePosition = getListView().getFirstVisiblePosition();
        return getListView().getChildAt(index - firstVisiblePosition);
    }

    /**
     * Use the internal state variables to determine wanted UI state.
     * Sub-classes might want to extend this.
     */
    protected void updateUiElementVisibility() {
        if (viewCreated) {
            // Progress view is displaying information
            if (showProgress || showLoadFailed) {
                emptyView.setVisibility(GONE);
                getListView().setVisibility(GONE);
                progressView.setVisibility(VISIBLE);
            } // Show the episode list or the empty view
            else {
                boolean itemsAvailable = getListAdapter() != null && !getListAdapter().isEmpty();

                emptyView.setVisibility(itemsAvailable ? GONE : VISIBLE);
                getListView().setVisibility(itemsAvailable ? VISIBLE : GONE);
                progressView.setVisibility(GONE);
            }
        }
    }

    private void updateListSelector() {
        // This takes care of the item pressed state and its color
        StateListDrawable states = new StateListDrawable();

        states.addState(new int[]{
                android.R.attr.state_pressed
        }, new ColorDrawable(lightThemeColor));

        // Set the states drawable
        getListView().setSelector(states);
    }
}
