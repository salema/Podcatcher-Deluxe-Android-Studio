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
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.adapters.EpisodeListAdapter;
import com.podcatcher.deluxe.listeners.EpisodeListContextListener;
import com.podcatcher.deluxe.listeners.OnReorderEpisodeListener;
import com.podcatcher.deluxe.listeners.OnReverseSortingListener;
import com.podcatcher.deluxe.listeners.OnSelectEpisodeListener;
import com.podcatcher.deluxe.listeners.OnToggleFilterListener;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.view.EpisodeListItemView;
import com.podcatcher.deluxe.view.fragments.SwipeReorderListViewTouchListener.ReorderCallback;

import java.util.ArrayList;
import java.util.List;

/**
 * List fragment to display the list of episodes.
 */
public class EpisodeListFragment extends PodcatcherListFragment implements ReorderCallback {

    /**
     * The list of episodes we are currently showing.
     */
    private List<Episode> currentEpisodeList;

    /**
     * The activity we are in (listens to user selection)
     */
    private OnSelectEpisodeListener episodeSelectionListener;
    /**
     * The activity we are in (listens to reorder requests)
     */
    private OnReorderEpisodeListener episodeReorderListener;
    /**
     * The activity we are in (listens to filter toggles)
     */
    private OnToggleFilterListener filterListener;
    /**
     * The activity we are in (listens to sorting toggles)
     */
    private OnReverseSortingListener sortingListener;

    /**
     * Out swipe to reorder listener
     */
    private SwipeReorderListViewTouchListener swipeReorderListener;

    /**
     * Flag for show sort menu item state
     */
    private boolean showSortMenuItem = false;
    /**
     * Flag for the state of the sort menu item
     */
    private boolean sortMenuItemState = false;
    /**
     * Flag for show filter menu item state
     */
    private boolean showFilterMenuItem = false;
    /**
     * Flag for the state of the filter menu item
     */
    private boolean filterMenuItemState = false;
    /**
     * Flag for the top progress bar state
     */
    private boolean showTopProgressBar = false;
    /**
     * Flag for show info box state state
     */
    private boolean showInfoBox = false;
    /**
     * The text string displayed in the info box
     */
    private String infoBoxText;
    /**
     * Flag to indicate whether podcast names should be shown for episodes
     */
    private boolean showPodcastNames = false;
    /**
     * Flag indicating whether list items can be swiped to reorder
     */
    private boolean enableSwipeReorder = false;

    /**
     * Identifier for the string the empty view shows.
     */
    private int emptyStringId = R.string.episode_none;

    /**
     * The sort episodes menu bar item
     */
    private MenuItem sortMenuItem;
    /**
     * The top progress bar
     */
    private ProgressBar topProgressBar;
    /**
     * The filter episodes menu bar item
     */
    private MenuItem filterMenuItem;
    /**
     * The info box label
     */
    private TextView infoBoxTextView;
    /**
     * The info box label divider
     */
    private View infoBoxDivider;

    /**
     * Status flag indicating that our view is created
     */
    private boolean viewCreated = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.episodeSelectionListener = (OnSelectEpisodeListener) activity;
            this.episodeReorderListener = (OnReorderEpisodeListener) activity;
            this.filterListener = (OnToggleFilterListener) activity;
            this.sortingListener = (OnReverseSortingListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSelectEpisodeListener, OnReorderEpisodeListener, " +
                    "OnFilterToggleListener, and OnReverseSortingListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.episode_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        topProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar_top);

        infoBoxTextView = (TextView) view.findViewById(R.id.info_box);
        infoBoxDivider = view.findViewById(R.id.info_box_divider);

        viewCreated = true;

        // Set list choice listener (context action mode)
        getListView().setMultiChoiceModeListener(new EpisodeListContextListener(this));

        // Create and set the swipe-to-reorder listener on the listview.
        swipeReorderListener = new SwipeReorderListViewTouchListener(getListView(), this);
        getListView().setOnTouchListener(swipeReorderListener);
        getListView().setOnScrollListener(swipeReorderListener.makeScrollListener());

        // This will make sure we show the right information once the view
        // controls are established (the list might have been set earlier)
        if (currentEpisodeList != null) {
            setEpisodeList(currentEpisodeList);
            setShowTopInfoBox(showInfoBox, infoBoxText);
            setShowTopProgress(showTopProgressBar);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.episode_list, menu);

        sortMenuItem = menu.findItem(R.id.sort_menuitem);
        setSortMenuItemVisibility(showSortMenuItem, sortMenuItemState);

        filterMenuItem = menu.findItem(R.id.filter_menuitem);
        setFilterMenuItemVisibility(showFilterMenuItem, filterMenuItemState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_menuitem:
                // Tell activity to re-order the list
                sortingListener.onReverseOrder();

                return true;
            case R.id.filter_menuitem:
                // Tell activity to toggle the filter
                filterListener.onToggleFilter();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        // Find selected episode and alert listener
        Episode selectedEpisode = (Episode) adapter.getItem(position);
        episodeSelectionListener.onEpisodeSelected(selectedEpisode);
    }

    @Override
    public boolean canReorder(int position) {
        return enableSwipeReorder && currentEpisodeList != null && currentEpisodeList.size() > 1;
    }

    @Override
    public void onMoveUp(int[] positions) {
        if (currentEpisodeList != null && positions.length > 0) {
            try {
                final Episode episode = currentEpisodeList.get(positions[0]);
                episodeReorderListener.onMoveEpisodeUp(episode);
            } catch (IndexOutOfBoundsException e) {
                // pass
            }
        }
    }

    @Override
    public void onMoveDown(int[] positions) {
        if (currentEpisodeList != null && positions.length > 0) {
            try {
                final Episode episode = currentEpisodeList.get(positions[0]);
                episodeReorderListener.onMoveEpisodeDown(episode);
            } catch (IndexOutOfBoundsException e) {
                // pass
            }
        }
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set the list of episodes to show in this fragment. You can call this any
     * time and the view will catch up as soon as it is created.
     *
     * @param episodeList List of episodes to show.
     */
    public void setEpisodeList(List<Episode> episodeList) {
        this.currentEpisodeList = episodeList;

        showProgress = false;
        showLoadFailed = false;

        // Update UI
        if (viewCreated) {
            // We need to store any currently checked items here because
            // we might want to re-check them after updating the list
            final List<Episode> checkedEpisodes = getCheckedEpisodes();

            if (adapter == null)
                // This also set the member
                setListAdapter(new EpisodeListAdapter(getActivity(), episodeList));
            else
                ((EpisodeListAdapter) adapter).updateList(episodeList);

            // Update adapter setting
            ((EpisodeListAdapter) adapter).setShowPodcastNames(showPodcastNames);

            // Restore checked items, if any
            boolean shouldResetAllCheckedStates = true;
            if (checkedEpisodes != null && checkedEpisodes.size() > 0) {
                getListView().clearChoices();

                for (Episode episode : checkedEpisodes) {
                    final int newPosition = currentEpisodeList.indexOf(episode);

                    if (newPosition >= 0) {
                        getListView().setItemChecked(newPosition, true);
                        shouldResetAllCheckedStates = false;
                    }
                }
            }

            // This will clear all check states and end the context mode
            if (shouldResetAllCheckedStates)
                getListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);

            // Update other UI elements
            if (episodeList.isEmpty())
                ((TextView) emptyView).setText(emptyStringId);

            // Make sure to match selection state
            if (selectAll)
                selectAll();
            else if (selectedPosition >= 0 && selectedPosition < episodeList.size())
                select(selectedPosition);
            else
                selectNone();
        }
    }

    /**
     * Set whether the fragment should show the sort icon. You can call this any
     * time and can expect it to happen on fragment resume at the latest. You
     * also have to set the sort icon state, <code>true</code> for "reverse" and
     * <code>false</code> for "normal" (i.e. latest first).
     *
     * @param show    Whether to show the sort menu item.
     * @param reverse State of the sort menu item (reverse / normal)
     */
    public void setSortMenuItemVisibility(boolean show, boolean reverse) {
        this.showSortMenuItem = show;
        this.sortMenuItemState = reverse;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (sortMenuItem != null) {
            sortMenuItem.setVisible(showSortMenuItem);
            sortMenuItem.setIcon(sortMenuItemState ?
                    R.drawable.ic_menu_sort_reverse : R.drawable.ic_menu_sort);
        }
    }

    /**
     * Set whether the fragment should show the filter icon. You can call this
     * any time and can expect it to happen on fragment resume at the latest.
     * You also have to set the filter icon state, <code>true</code> for
     * "new only" and <code>false</code> for "show all".
     *
     * @param show   Whether to show the filter menu item.
     * @param filter State of the filter menu item (new / all)
     */
    public void setFilterMenuItemVisibility(boolean show, boolean filter) {
        this.showFilterMenuItem = show;
        this.filterMenuItemState = filter;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (filterMenuItem != null) {
            filterMenuItem.setVisible(showFilterMenuItem);
            filterMenuItem.setTitle(filterMenuItemState ?
                    R.string.episodes_filter_all : R.string.episodes_filter_new);
            filterMenuItem.setIcon(filterMenuItemState ?
                    R.drawable.ic_menu_filter_reverse : R.drawable.ic_menu_filter);
        }
    }

    /**
     * Configure whether the fragment should show the info box at the top of the
     * list.
     *
     * @param show Whether to show the info box at all.
     * @param info The info text to show.
     */
    public void setShowTopInfoBox(boolean show, String info) {
        this.showInfoBox = show;
        this.infoBoxText = info;

        if (viewCreated) {
            infoBoxTextView.setVisibility(show ? View.VISIBLE : View.GONE);
            infoBoxDivider.setVisibility(show ? View.VISIBLE : View.GONE);
            infoBoxTextView.setText(info);
        }
    }

    /**
     * Update the progress information for the episode given to reflect the
     * percentage of given. Does nothing if the episode is off the screen.
     *
     * @param episode The episode to update progress for.
     * @param percent The percentage value to show.
     */
    public void showProgress(Episode episode, int percent) {
        // To prevent this if we are not ready to handle progress update
        // e.g. on app termination
        if (viewCreated && currentEpisodeList != null) {
            final EpisodeListItemView listItemView = (EpisodeListItemView)
                    findListItemViewForIndex(currentEpisodeList.indexOf(episode));

            // Is the position visible?
            if (listItemView != null)
                listItemView.updateProgress(percent);
        }
    }

    /**
     * Select an episode in the list. Calls {@link #selectNone()} if the episode
     * is not present in the current list. Does nothing if there is no list set
     * or the episode is <code>null</code>.
     *
     * @param episode Episode to select.
     */
    public void select(Episode episode) {
        if (currentEpisodeList != null && episode != null) {
            final int index = currentEpisodeList.indexOf(episode);

            if (index >= 0)
                select(index);
            else
                selectNone();
        }
    }

    /**
     * Set to <code>true</code> to make list items (episodes) "flingable". Uses
     * the {@link SwipeReorderListViewTouchListener} and its callback methods.
     *
     * @param enable Whether list item can be swiped. This will switch back to
     *               <code>false</code> every time the fragment is reset.
     */
    @SuppressWarnings("SameParameterValue")
    public void setEnableSwipeReorder(boolean enable) {
        this.enableSwipeReorder = enable;
    }

    /**
     * Set whether the fragment should show the top progress bar. You can call
     * this any time and can expect it to happen on fragment resume at the
     * latest.
     *
     * @param show Whether to show the top progress bar.
     */
    public void setShowTopProgress(boolean show) {
        this.showTopProgressBar = show;

        if (viewCreated)
            topProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Set whether the fragment should show the podcast name for each episode
     * item. Change will be reflected upon next call of
     * {@link #setEpisodeList(List)}
     *
     * @param show Whether to show the podcast names.
     */
    public void setShowPodcastNames(boolean show) {
        this.showPodcastNames = show;
    }

    /**
     * Define which text label the list's empty view shows. Will only have an
     * effect if you call {@link #setEpisodeList(List)} with an empty list
     * afterwards.
     *
     * @param id The empty string resource identifier.
     */
    public void setEmptyStringId(int id) {
        this.emptyStringId = id;
    }

    @Override
    protected void reset() {
        if (viewCreated) {
            ((TextView) emptyView).setText(R.string.podcast_none_selected);
            setShowTopInfoBox(false, null);
            setShowTopProgress(false);
        }

        currentEpisodeList = null;
        showPodcastNames = false;
        enableSwipeReorder = false;

        super.reset();
    }

    /**
     * Show error view.
     *
     * @param code The error code loading the podcast failed with.
     */
    public void showLoadFailed(PodcastLoadError code) {
        if (viewCreated)
            switch (code) {
                case ACCESS_DENIED:
                    progressView.showError(R.string.podcast_load_error_access_denied);
                    break;

                case NOT_PARSABLE:
                    progressView.showError(R.string.podcast_load_error_not_parsable);
                    break;

                case NOT_REACHABLE:
                    progressView.showError(R.string.podcast_load_error_not_reachable);
                    break;

                case EXPLICIT_BLOCKED:
                    progressView.showError(R.string.podcast_load_error_explicit);
                    break;

                default:
                    progressView.showError(R.string.podcast_load_error);
                    break;
            }

        super.showLoadFailed();
    }

    /**
     * Show error view for "select all podcasts" failed.
     */
    public void showLoadAllFailed() {
        if (viewCreated)
            progressView.showError(R.string.podcast_load_multiple_error_all);

        super.showLoadFailed();
    }

    private List<Episode> getCheckedEpisodes() {
        List<Episode> result = null;

        if (getListAdapter() != null && getListView().getCheckedItemCount() > 0) {
            result = new ArrayList<>();
            final SparseBooleanArray checkedItems = getListView().getCheckedItemPositions();

            for (int position = 0; position < getListAdapter().getCount(); position++)
                if (checkedItems.get(position))
                    result.add((Episode) getListAdapter().getItem(position));
        }

        return result;
    }
}
