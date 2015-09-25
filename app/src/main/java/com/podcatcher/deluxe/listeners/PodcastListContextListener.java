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

package com.podcatcher.deluxe.listeners;

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.ExportOpmlActivity;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.RemovePodcastActivity;
import com.podcatcher.deluxe.adapters.PodcastListAdapter;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.view.fragments.AuthorizationFragment;
import com.podcatcher.deluxe.view.fragments.AuthorizationFragment.OnEnterAuthorizationListener;
import com.podcatcher.deluxe.view.fragments.PodcastListFragment;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ShareActionProvider;

import java.util.ArrayList;
import java.util.Locale;

import static android.net.Uri.encode;
import static com.podcatcher.deluxe.BaseActivity.PODCAST_POSITION_LIST_KEY;
import static com.podcatcher.deluxe.view.fragments.AuthorizationFragment.USERNAME_PRESET_KEY;

/**
 * Listener for the podcast list context mode.
 */
public class PodcastListContextListener implements MultiChoiceModeListener,
        ShareActionProvider.OnShareTargetSelectedListener {

    /**
     * The action mode handle
     */
    private ActionMode actionMode;
    /**
     * The owning fragment
     */
    private final PodcastListFragment fragment;

    /**
     * The share podcast menu item
     */
    private MenuItem sharePodcastMenuItem;

    /**
     * The edit authorization menu item
     */
    private MenuItem editAuthMenuItem;
    /**
     * The send suggestion menu item
     */
    private MenuItem sendSuggestionMenuItem;
    /**
     * The select all menu item
     */
    private MenuItem selectAllMenuItem;

    /**
     * Flag to indicate whether the mode should do potentially expensive UI
     * updates when a list item is checked
     */
    private boolean updateUi = true;

    /**
     * Create new listener for the podcast list context mode.
     *
     * @param fragment The podcast list fragment to call back to.
     */
    public PodcastListContextListener(PodcastListFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        this.actionMode = mode;
        mode.getMenuInflater().inflate(R.menu.podcast_list_context, menu);

        sharePodcastMenuItem = menu.findItem(R.id.podcast_share_menuitem);
        editAuthMenuItem = menu.findItem(R.id.edit_auth_contextmenuitem);
        sendSuggestionMenuItem = menu.findItem(R.id.suggest_podcast_contextmenuitem);
        selectAllMenuItem = menu.findItem(R.id.podcast_select_all_contextmenuitem);

        // Make sure we are notified when the share action is selected
        // so we can finish the action mode and close the context menu
        ((ShareActionProvider) sharePodcastMenuItem.getActionProvider()).setOnShareTargetSelectedListener(this);

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
        // Get the checked positions
        ArrayList<Integer> positions = getCheckedPositions();

        // Act on the selected menu item
        switch (item.getItemId()) {
            case R.id.edit_auth_contextmenuitem:
                // There is only one podcast checked...
                final Podcast podcast = (Podcast) fragment.getListAdapter().getItem(positions.get(0));
                // Show dialog for authorization
                final AuthorizationFragment authorizationFragment = new AuthorizationFragment();

                // Create bundle to make dialog aware of username to pre-set
                if (podcast.getUsername() != null) {
                    final Bundle args = new Bundle();
                    args.putString(USERNAME_PRESET_KEY, podcast.getUsername());
                    authorizationFragment.setArguments(args);
                }

                // Set the callback
                authorizationFragment.setListener(new OnEnterAuthorizationListener() {

                    @Override
                    public void onSubmitAuthorization(String username, String password) {
                        PodcastManager.getInstance().setCredentials(podcast, username, password);

                        // Action picked, so close the CAB
                        mode.finish();
                    }

                    @Override
                    public void onCancelAuthorization() {
                        // No action
                    }
                });

                // Finally show the dialog
                authorizationFragment.show(fragment.getFragmentManager(), AuthorizationFragment.TAG);

                return true;
            case R.id.suggest_podcast_contextmenuitem:
                // There is only one podcast checked...
                final Podcast suggestion =
                        (Podcast) fragment.getListAdapter().getItem(positions.get(0));

                // Construct the email
                final String uriText = String.format(Locale.US, "mailto:%s?subject=%s&body=%s",
                        encode(fragment.getString(R.string.suggestion_address)),
                        encode(fragment.getString(R.string.suggestion_subject,
                                fragment.getString(R.string.app_name), BuildConfig.STORE)),
                        encode(suggestion.getName() + " at " + suggestion.getUrl()));

                // Go start the mail app
                final Intent sendTo = new Intent(Intent.ACTION_SENDTO, Uri.parse(uriText));
                try {
                    fragment.getActivity().startActivity(sendTo);
                } catch (ActivityNotFoundException ex) {
                    // No mail app, this should not happen...
                }

                return true;
            case R.id.podcast_remove_contextmenuitem:
                // Prepare deletion activity
                Intent remove = new Intent(fragment.getActivity(), RemovePodcastActivity.class);
                remove.putIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY, positions);

                // Go remove podcasts
                fragment.startActivity(remove);

                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.opml_export_contextmenuitem:
                // Prepare export activity
                Intent export = new Intent(fragment.getActivity(), ExportOpmlActivity.class);
                export.putIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY, positions);

                // Go export podcasts
                fragment.startActivity(export);

                // Action picked, so close the CAB
                mode.finish();
                return true;
            case R.id.podcast_select_all_contextmenuitem:
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
        ((PodcastListAdapter) fragment.getListAdapter()).setCheckedPositions(null);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        update(mode);

        if (fragment.getListView().getCheckedItemCount() == 1)
            updateShareIntent((Podcast) fragment.getListAdapter().getItem(getCheckedPositions().get(0)));
    }

    private void update(ActionMode mode) {
        // Only run if UI updates are enabled
        if (updateUi)
            try {
                // Let list adapter know which items to mark checked (row color)
                ((PodcastListAdapter) fragment.getListAdapter()).setCheckedPositions(
                        fragment.getListView().getCheckedItemPositions());

                // Update the mode title text
                final int checkedItemCount = fragment.getListView().getCheckedItemCount();
                mode.setTitle(fragment.getResources()
                        .getQuantityString(R.plurals.podcasts, checkedItemCount, checkedItemCount));
                mode.setSubtitle(null);

                // Show/hide edit auth menu item
                sharePodcastMenuItem.setVisible(checkedItemCount == 1);
                editAuthMenuItem.setVisible(checkedItemCount == 1 && !BuildConfig.FIXED_BUNDLE);
                sendSuggestionMenuItem.setVisible(checkedItemCount == 1 && !BuildConfig.FIXED_BUNDLE);
                // Hide the select all item if all items are selected
                selectAllMenuItem.setVisible(checkedItemCount != fragment.getListAdapter().getCount());
            } catch (NullPointerException npe) {
                // pass, this happens when some of the parts (fragment or list view)
                // are not there yet, we simply ignore this since update will be
                // called again.
            }
    }

    private void updateShareIntent(Podcast podcast) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, fragment.getString(R.string.podcast_share_template,
                podcast.getName(), podcast.getUrl()));
        sendIntent.setType("text/plain");

        final ShareActionProvider provider = (ShareActionProvider) sharePodcastMenuItem.getActionProvider();
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
