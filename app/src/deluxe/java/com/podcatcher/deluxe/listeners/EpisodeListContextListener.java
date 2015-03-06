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

package com.podcatcher.deluxe.listeners;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ShareActionProvider;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.adapters.EpisodeListAdapter;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.view.fragments.DeleteDownloadsConfirmationFragment;
import com.podcatcher.deluxe.view.fragments.DeleteDownloadsConfirmationFragment.OnDeleteDownloadsConfirmationListener;
import com.podcatcher.deluxe.view.fragments.EpisodeListFragment;

import java.util.ArrayList;

import static com.podcatcher.deluxe.view.fragments.DeleteDownloadsConfirmationFragment.EPISODE_COUNT_KEY;
import static com.podcatcher.deluxe.view.fragments.DeleteDownloadsConfirmationFragment.TAG;

/**
 * Listener for the episode list context mode.
 */
public class EpisodeListContextListener implements MultiChoiceModeListener,
        ShareActionProvider.OnShareTargetSelectedListener {

    /**
     * Separator for episode duration total and size total
     */
    private static final String SEPARATOR = " • ";

    /**
     * The action mode handle
     */
    private ActionMode actionMode;
    /**
     * The owning fragment
     */
    private final EpisodeListFragment fragment;
    /**
     * The episode manager handle
     */
    private final EpisodeManager episodeManager;

    /**
     * The mark new menu item
     */
    private MenuItem newMenuItem;
    /**
     * The mark old menu item
     */
    private MenuItem oldMenuItem;
    /**
     * The download menu item
     */
    private MenuItem downloadMenuItem;
    /**
     * The delete menu item
     */
    private MenuItem deleteMenuItem;
    /**
     * The add to playlist menu item
     */
    private MenuItem addToPlaylistMenuItem;
    /**
     * The remove from playlist menu item
     */
    private MenuItem removeFromPlaylistMenuItem;
    /**
     * The share episode menu item
     */
    private MenuItem shareEpisodeMenuItem;
    /**
     * The select all menu item
     */
    private MenuItem selectAllMenuItem;

    /**
     * This is the number of items selected that are not downloaded or currently
     * downloading
     */
    private int deletesTriggered = 0;
    /**
     * Flag to indicate whether the mode should do potentially expensive UI
     * updates when a list item is checked
     */
    private boolean updateUi = true;

    /**
     * Create new listener for the episode list context mode.
     *
     * @param fragment The episode list fragment to call back to.
     */
    public EpisodeListContextListener(EpisodeListFragment fragment) {
        this.fragment = fragment;
        this.episodeManager = EpisodeManager.getInstance();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        this.actionMode = mode;
        mode.getMenuInflater().inflate(R.menu.episode_list_context, menu);

        newMenuItem = menu.findItem(R.id.episode_new_contextmenuitem);
        oldMenuItem = menu.findItem(R.id.episode_old_contextmenuitem);
        downloadMenuItem = menu.findItem(R.id.episode_download_contextmenuitem);
        deleteMenuItem = menu.findItem(R.id.episode_remove_contextmenuitem);
        addToPlaylistMenuItem = menu.findItem(R.id.episode_add_to_playlist_contextmenuitem);
        removeFromPlaylistMenuItem = menu.findItem(R.id.episode_remove_from_playlist_contextmenuitem);
        shareEpisodeMenuItem = menu.findItem(R.id.episode_share_contextmenuitem);
        selectAllMenuItem = menu.findItem(R.id.episode_select_all_contextmenuitem);

        // Make sure we are notified when the share action is selected
        // so we can finish the action mode and close the context menu
        ((ShareActionProvider) shareEpisodeMenuItem.getActionProvider()).setOnShareTargetSelectedListener(this);

        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        update(mode);

        return true;
    }

    @Override
    public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
        if (actionMode != null)
            actionMode.finish();

        return false;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
        boolean markNew = false;
        boolean append = false;

        // Get the list of checked positions
        final ArrayList<Integer> positions = getCheckedPositions();

        switch (item.getItemId()) {
            case R.id.episode_new_contextmenuitem:
                markNew = true;
                // No break here, code blow should run
            case R.id.episode_old_contextmenuitem:
                for (Integer position : positions)
                    episodeManager.setState((Episode) fragment.getListAdapter().getItem(position), !markNew);

                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.episode_download_contextmenuitem:
                for (Integer position : positions)
                    episodeManager.download((Episode) fragment.getListAdapter().getItem(position));

                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.episode_remove_contextmenuitem:
                final DeleteDownloadsConfirmationFragment confirmationDialog = new DeleteDownloadsConfirmationFragment();

                // Create bundle to make dialog aware of selection count
                final Bundle args = new Bundle();
                args.putInt(EPISODE_COUNT_KEY, deletesTriggered);
                confirmationDialog.setArguments(args);
                // Set the callback
                confirmationDialog.setListener(new OnDeleteDownloadsConfirmationListener() {

                    @Override
                    public void onConfirmDeletion() {
                        // Go delete the downloads
                        for (Integer position : positions)
                            episodeManager.deleteDownload((Episode) fragment.getListAdapter().getItem(position));

                        // Action picked, so close the CAB
                        mode.finish();
                    }

                    @Override
                    public void onCancelDeletion() {
                        // Nothing to do here
                    }
                });

                // Finally show the dialog
                confirmationDialog.show(fragment.getFragmentManager(), TAG);

                return true;
            case R.id.episode_add_to_playlist_contextmenuitem:
                append = true;
                // No break here, code blow should run
            case R.id.episode_remove_from_playlist_contextmenuitem:
                for (Integer position : positions) {
                    final Episode episode = (Episode) fragment.getListAdapter().getItem(position);

                    if (append)
                        episodeManager.appendToPlaylist(episode);
                    else
                        episodeManager.removeFromPlaylist(episode);
                }

                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.episode_select_all_contextmenuitem:
                // Disable expensive UI updates
                updateUi = false;
                for (int index = 0; index < fragment.getListAdapter().getCount(); index++)
                    fragment.getListView().setItemChecked(index, true);

                // Re-enable UI updates
                updateUi = true;
                update(mode);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        ((EpisodeListAdapter) fragment.getListAdapter()).setCheckedPositions(null);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        update(mode);

        if (fragment.getListView().getCheckedItemCount() == 1)
            updateShareIntent((Episode) fragment.getListAdapter().getItem(getCheckedPositions().get(0)));
    }

    private void update(ActionMode mode) {
        // Only run if UI updates are enabled
        if (updateUi)
            try {
                updateMenuItems();

                // Let list adapter know which items to mark checked (row color)
                ((EpisodeListAdapter) fragment.getListAdapter()).setCheckedPositions(
                        fragment.getListView().getCheckedItemPositions());

                // Update the mode title text
                final int checkedItemCount = fragment.getListView().getCheckedItemCount();
                mode.setTitle(fragment.getResources()
                        .getQuantityString(R.plurals.episodes, checkedItemCount, checkedItemCount));
                mode.setSubtitle(createSubtitle());
            } catch (NullPointerException npe) {
                // This also avoids crashes when the app has been hidden for
                // some time while the context mode was activated and (parts of)
                // the fragment is (are) gone
            }
    }

    private void updateMenuItems() {
        // Initialize counter for the number of deletes the current selection would trigger
        this.deletesTriggered = 0;

        // Make all menu items invisible
        newMenuItem.setVisible(false);
        oldMenuItem.setVisible(false);
        downloadMenuItem.setVisible(false);
        deleteMenuItem.setVisible(false);
        addToPlaylistMenuItem.setVisible(false);
        removeFromPlaylistMenuItem.setVisible(false);

        SparseBooleanArray checkedItems = fragment.getListView().getCheckedItemPositions();

        // Check which option apply to current selection and
        // make corresponding menu items visible
        for (int position = 0; position < fragment.getListAdapter().getCount(); position++) {
            if (checkedItems.get(position)) {
                Episode episode = (Episode) fragment.getListAdapter().getItem(position);

                if (episodeManager.getState(episode))
                    newMenuItem.setVisible(true);
                else
                    oldMenuItem.setVisible(true);

                if (episodeManager.isDownloadingOrDownloaded(episode)) {
                    deletesTriggered++;
                    deleteMenuItem.setVisible(true);
                } else
                    downloadMenuItem.setVisible(true);

                if (episodeManager.isInPlaylist(episode))
                    removeFromPlaylistMenuItem.setVisible(true);
                else
                    addToPlaylistMenuItem.setVisible(true);
            }
        }

        // Show share menu item if only one episode is selected
        shareEpisodeMenuItem.setVisible(fragment.getListView().getCheckedItemCount() == 1);
        // Hide the select all item if all items are selected
        selectAllMenuItem.setVisible(fragment.getListView().getCheckedItemCount() !=
                fragment.getListAdapter().getCount());
    }

    private String createSubtitle() {
        int totalDuration = 0;
        int totalSize = 0;

        final SparseBooleanArray checkedItems = fragment.getListView().getCheckedItemPositions();
        for (int position = 0; position < fragment.getListAdapter().getCount(); position++) {
            if (checkedItems.get(position)) {
                final Episode episode = (Episode) fragment.getListAdapter().getItem(position);

                if (episode.getDuration() > 0)
                    totalDuration += episode.getDuration();
                if (episode.getFileSize() > 0)
                    totalSize += episode.getFileSize();
            }
        }

        final StringBuilder builder = new StringBuilder();

        if (totalDuration > 0)
            builder.append(ParserUtils.formatTime(totalDuration)).append(SEPARATOR);
        if (totalSize > 0)
            builder.append(ParserUtils.formatFileSize(totalSize));

        String result = builder.toString();
        if (result.endsWith(SEPARATOR))
            result = result.substring(0, result.length() - SEPARATOR.length());

        // Only show a subtitle if there is any interesting information, i.e. more than
        // one episode is selected and duration and size are available non-zero
        return checkedItems.size() > 1 && !result.isEmpty() ? result : null;
    }

    private void updateShareIntent(Episode episode) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, fragment.getString(R.string.episode_share_template,
                episode.getName(), episode.getPodcast().getName(), episode.getMediaUrl()));
        sendIntent.setType("text/plain");

        final ShareActionProvider provider = (ShareActionProvider) shareEpisodeMenuItem.getActionProvider();
        provider.setShareIntent(sendIntent);
    }

    private ArrayList<Integer> getCheckedPositions() {
        // Get the list of checked positions from the boolean array
        SparseBooleanArray checkedItems = fragment.getListView().getCheckedItemPositions();
        ArrayList<Integer> positions = new ArrayList<>();

        for (int index = 0; index < fragment.getListView().getCount(); index++)
            if (checkedItems.get(index))
                positions.add(index);

        return positions;
    }
}
