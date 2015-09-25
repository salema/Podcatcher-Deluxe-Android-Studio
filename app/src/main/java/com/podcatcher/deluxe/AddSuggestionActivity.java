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

package com.podcatcher.deluxe;

import com.podcatcher.deluxe.listeners.OnLoadSuggestionListener;
import com.podcatcher.deluxe.model.SuggestionManager;
import com.podcatcher.deluxe.model.tasks.remote.ReportAdditionTask;
import com.podcatcher.deluxe.model.types.Language;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;
import com.podcatcher.deluxe.view.fragments.AddSuggestionFragment;
import com.podcatcher.deluxe.view.fragments.AddSuggestionFragment.AddSuggestionListener;
import com.podcatcher.deluxe.view.fragments.ConfirmExplicitSuggestionFragment;
import com.podcatcher.deluxe.view.fragments.ConfirmExplicitSuggestionFragment.ConfirmExplicitSuggestionDialogListener;
import com.podcatcher.deluxe.view.fragments.SelectFeedFragment;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.net.Uri.encode;

/**
 * Add podcast(s) from suggestions activity.
 */
public class AddSuggestionActivity extends BaseActivity implements OnLoadSuggestionListener,
        AddSuggestionListener, ConfirmExplicitSuggestionDialogListener, SelectFeedFragment.SelectFeedDialogListener {

    /**
     * Tag to find the add suggestion fragment under
     */
    private static final String ADD_SUGGESTION_FRAGMENT_TAG = "add_suggestion_fragment";
    /**
     * Key to find "podcast to be added" URL under
     */
    private static final String TO_BE_ADDED_URL_KEY = "TO_BE_ADDED_URL_KEY";

    /**
     * The suggestion manager handle
     */
    private SuggestionManager suggestionManager;
    /**
     * The fragment containing the add suggestion UI
     */
    private AddSuggestionFragment suggestionFragment;

    /**
     * Helper to store suggestion URL awaiting confirmation and/or feed selection
     */
    private String suggestionToAddUrl;
    /**
     * Helper to store suggestion awaiting confirmation and/or feed selection
     */
    private Suggestion suggestionToAdd;

    /**
     * The search view widget
     */
    private SearchView searchView;
    /**
     * The toprunner menu item
     */
    private MenuItem toprunnerMenuItem;
    /**
     * The new suggestions menu item
     */
    private MenuItem newMenuItem;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            this.suggestionFragment = new AddSuggestionFragment();

            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, suggestionFragment, ADD_SUGGESTION_FRAGMENT_TAG)
                    .commit();
        } else {
            this.suggestionFragment = (AddSuggestionFragment)
                    getFragmentManager().findFragmentByTag(ADD_SUGGESTION_FRAGMENT_TAG);

            if (savedInstanceState.containsKey(TO_BE_ADDED_URL_KEY))
                suggestionToAddUrl = savedInstanceState.getString(TO_BE_ADDED_URL_KEY);
        }

        // Get suggestions manager and register call-back
        suggestionManager = SuggestionManager.getInstance();
        suggestionManager.addLoadSuggestionListListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.suggestions, menu);

        // Get handle to other item, we want to hide them if the search is active
        this.toprunnerMenuItem = menu.findItem(R.id.featured_suggestions_menuitem);
        this.newMenuItem = menu.findItem(R.id.new_suggestions_menuitem);

        // Associate searchable configuration with the SearchView
        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        this.searchView = (SearchView) menu.findItem(R.id.search_suggestions_menuitem).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getString(R.string.suggestions_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                suggestionFragment.setSearchQuery(query);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                suggestionFragment.setSearchQuery(query);

                return false;
            }
        });
        menu.findItem(R.id.search_suggestions_menuitem).setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                toprunnerMenuItem.setVisible(!view.isSmallPortrait() && !BuildConfig.FIXED_BUNDLE);
                newMenuItem.setVisible(!view.isSmallPortrait() && !BuildConfig.FIXED_BUNDLE);

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                toprunnerMenuItem.setVisible(!BuildConfig.FIXED_BUNDLE);
                newMenuItem.setVisible(!BuildConfig.FIXED_BUNDLE);

                return true;
            }
        });
        colorSearchView(); // If possible, apply theme color to search plates

        // Hide some options in fixed bundle versions
        toprunnerMenuItem.setVisible(!BuildConfig.FIXED_BUNDLE);
        newMenuItem.setVisible(!BuildConfig.FIXED_BUNDLE);
        menu.findItem(R.id.podcast_add_menuitem).setVisible(!BuildConfig.FIXED_BUNDLE);
        menu.findItem(R.id.import_opml_menuitem).setVisible(!BuildConfig.FIXED_BUNDLE);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.featured_suggestions_menuitem:
                // Toggle featured/all filter
                final boolean isFeaturedFiltered = getString(R.string.suggestion_featured).equals(suggestionFragment.getSearchQuery());
                suggestionFragment.setSearchQuery(isFeaturedFiltered ? null : getString(R.string.suggestion_featured));

                return true;
            case R.id.new_suggestions_menuitem:
                // Toggle new/all filter
                final boolean isNewFiltered = getString(R.string.suggestion_new).equals(suggestionFragment.getSearchQuery());
                suggestionFragment.setSearchQuery(isNewFiltered ? null : getString(R.string.suggestion_new));

                return true;
            case R.id.podcast_add_menuitem:
                startActivity(new Intent(this, AddPodcastActivity.class));
                finish();

                return true;
            case R.id.import_opml_menuitem:
                startActivity(new Intent(this, ImportOpmlActivity.class));
                finish();

                return true;
            case R.id.send_suggestion_menuitem:
                // Construct the email
                final String uriText = String.format(Locale.US, "mailto:%s?subject=%s",
                        encode(getString(R.string.suggestion_address)),
                        encode(getString(R.string.suggestion_subject,
                                getString(R.string.app_name), BuildConfig.STORE)));

                // Go start the mail app
                final Intent sendTo = new Intent(Intent.ACTION_SENDTO, Uri.parse(uriText));
                try {
                    startActivity(sendTo);
                } catch (ActivityNotFoundException ex) {
                    // No mail app, this should not happen...
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // This is only needed for voice search
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);

            // TODO This should not activate the keyboard
            this.searchView.setQuery(query, false);
            suggestionFragment.setSearchQuery(query);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Need to add some space in between the icon and the title
        getActionBar().setTitle(" " + getActionBar().getTitle().toString().trim());

        // Load suggestions (this has to be called after UI fragment is created)
        suggestionManager.load();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (suggestionToAdd != null)
            outState.putString(TO_BE_ADDED_URL_KEY, suggestionToAdd.getUrl());
    }

    @Override
    protected void onDestroy() {
        suggestionManager.removeLoadSuggestionListListener(this);

        super.onDestroy();
    }

    @Override
    public void onSuggestionsLoadProgress(Progress progress) {
        suggestionFragment.showLoadProgress(progress);
    }

    @Override
    public void onSuggestionsLoaded(List<Suggestion> suggestions) {
        // Resulting list
        List<Suggestion> filteredSuggestionList = new ArrayList<>();

        // Do filter!
        for (Suggestion suggestion : suggestions) {
            if (!podcastManager.contains(suggestion) &&
                    !(podcastManager.blockExplicit() && suggestion.isExplicit()))
                filteredSuggestionList.add(suggestion);

            // Restore the "suggestion to be added" member
            if (suggestion.equalByUrl(suggestionToAddUrl))
                suggestionToAdd = suggestion;
        }

        // Filter list and update UI
        suggestionFragment.setList(filteredSuggestionList);
    }

    @Override
    public void onSuggestionsLoadFailed() {
        suggestionFragment.showLoadFailed();
    }

    @Override
    public void onAddSuggestion(Suggestion suggestion) {
        this.suggestionToAdd = suggestion;

        if (suggestion.isExplicit())
            new ConfirmExplicitSuggestionFragment().show(getFragmentManager(), null);
        else
            addConfirmedSuggestion();
    }

    @Override
    public void onConfirmExplicit() {
        addConfirmedSuggestion();
    }

    @Override
    public void onCancelExplicit() {
        // Nothing to do here...
    }

    private void addConfirmedSuggestion() {
        if (suggestionToAdd.getFeeds().size() > 1) {
            final SelectFeedFragment fragment = new SelectFeedFragment();

            fragment.setFeeds(suggestionToAdd.getFeeds());
            fragment.show(getFragmentManager(), null);
        } else
            onFeedSelected(suggestionToAdd.getUrl());
    }

    @Override
    public void onFeedSelected(String podcastUrl) {
        final Podcast podcast = new Podcast(suggestionToAdd.getName(), podcastUrl);

        podcastManager.addPodcast(podcast);
        new ReportAdditionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, podcast);

        showToast(getString(R.string.podcast_added, suggestionToAdd.getName()));
    }

    @Override
    public void onLanguageChanged(Language newLanguage) {
        // TODO Adjust voice search to selected language (looks like there is currently no way to do this)
    }

    @Override
    public void onResetFilters() {
        searchView.setQuery(null, false);
    }

    private void colorSearchView() {
        final int searchPlateId = getResources().getIdentifier("android:id/search_plate", null, null);
        final View searchPlate = searchView.findViewById(searchPlateId);
        final int voicePlateId = getResources().getIdentifier("android:id/submit_area", null, null);
        final View voicePlate = searchView.findViewById(voicePlateId);

        // Make sure not to color only one plate
        if (searchPlate != null && voicePlate != null) {
            searchPlate.setBackgroundResource(R.drawable.textfield_searchview_holo_dark);
            voicePlate.setBackgroundResource(R.drawable.textfield_searchview_holo_dark);
        }
    }
}
