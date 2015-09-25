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

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.adapters.GenreSpinnerAdapter;
import com.podcatcher.deluxe.adapters.LanguageSpinnerAdapter;
import com.podcatcher.deluxe.adapters.MediaTypeSpinnerAdapter;
import com.podcatcher.deluxe.adapters.SuggestionListAdapter;
import com.podcatcher.deluxe.listeners.OnChangePodcastListListener;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.types.Genre;
import com.podcatcher.deluxe.model.types.Language;
import com.podcatcher.deluxe.model.types.MediaType;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;
import com.podcatcher.deluxe.view.ProgressView;
import com.podcatcher.deluxe.view.SuggestionListItemViewHolder;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment to show podcast suggestions.
 */
public class AddSuggestionFragment extends Fragment implements OnChangePodcastListListener {

    /**
     * The call back we work on
     */
    private AddSuggestionListener listener;

    /**
     * Interface definition for a callback to be invoked when the user interacts
     * with the add podcast suggestion grid.
     */
    public interface AddSuggestionListener {

        /**
         * Called on listener when podcast suggestion is selected.
         *
         * @param suggestion Podcast to add.
         */
        void onAddSuggestion(Suggestion suggestion);

        /**
         * Called on listener when the user filters by a language.
         *
         * @param newLanguage The new language selected or <code>null</code> as a wildcard
         */
        void onLanguageChanged(Language newLanguage);

        /**
         * Called on listener when the user whats to reset all filters.
         */
        void onResetFilters();
    }

    /**
     * The filter wildcard
     */
    public static final String FILTER_WILDCARD = "ALL";
    /**
     * Bundle key for the search query
     */
    private static final String SEARCH_QUERY = "search_query_text";
    /**
     * Bundle key for language filter position
     */
    private static final String LANGUAGE_FILTER_POSITION = "language_filter_position";
    /**
     * Bundle key for genre filter position
     */
    private static final String GENRE_FILTER_POSITION = "genre_filter_position";
    /**
     * Bundle key for media type filter position
     */
    private static final String MEDIATYPE_FILTER_POSITION = "mediatype_filter_position";

    /**
     * The listener to update the list on filter change
     */
    private final OnItemSelectedListener selectionListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            //noinspection EqualsBetweenInconvertibleTypes
            if (parent.equals(languageFilter))
                try {
                    listener.onLanguageChanged((Language) parent.getItemAtPosition(position));
                } catch (ClassCastException cce) {
                    listener.onLanguageChanged(null);
                }

            updateGrid();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            updateGrid();
        }
    };


    /**
     * The currently active search query
     */
    private String searchQuery;

    /**
     * The full list of suggestions
     */
    private List<Suggestion> suggestionList;
    /**
     * The list of suggestions to show with current filters applied
     */
    private final List<Suggestion> filteredSuggestionList = new ArrayList<>();

    /**
     * The suggestion list adapter
     */
    private SuggestionListAdapter adapter;
    /**
     * The suggestion list layout manager
     */
    private StaggeredGridLayoutManager layoutManager;

    /**
     * The language filter
     */
    private Spinner languageFilter;
    /**
     * The genre filter
     */
    private Spinner genreFilter;
    /**
     * The media type filter
     */
    private Spinner mediaTypeFilter;
    /**
     * The progress view
     */
    private ProgressView progressView;
    /**
     * The suggestions list view
     */
    private RecyclerView suggestionsGridView;
    /**
     * The suggestions list empty view
     */
    private ViewStub suggestionsGridEmptyView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (AddSuggestionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AddSuggestionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PodcastManager.getInstance().addChangePodcastListListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.layoutManager = new StaggeredGridLayoutManager(getResources().getInteger(
                R.integer.suggestion_grid_columns), StaggeredGridLayoutManager.VERTICAL);
        this.adapter = new SuggestionListAdapter(filteredSuggestionList, listener);

        return inflater.inflate(R.layout.suggestion_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final View filterBar = view.findViewById(R.id.suggestion_filter_bar);
        filterBar.setVisibility(BuildConfig.FIXED_BUNDLE ? View.GONE : View.VISIBLE);

        languageFilter = (Spinner) view.findViewById(R.id.suggestion_language_select);
        languageFilter.setAdapter(new LanguageSpinnerAdapter(getActivity()));
        languageFilter.setOnItemSelectedListener(selectionListener);

        genreFilter = (Spinner) view.findViewById(R.id.suggestion_genre_select);
        genreFilter.setAdapter(new GenreSpinnerAdapter(getActivity()));
        genreFilter.setOnItemSelectedListener(selectionListener);

        mediaTypeFilter = (Spinner) view.findViewById(R.id.suggestion_type_select);
        mediaTypeFilter.setAdapter(new MediaTypeSpinnerAdapter(getActivity()));
        mediaTypeFilter.setOnItemSelectedListener(selectionListener);

        progressView = (ProgressView) view.findViewById(R.id.suggestion_list_progress);
        suggestionsGridView = (RecyclerView) view.findViewById(R.id.suggestion_grid);
        suggestionsGridView.setHasFixedSize(true);
        suggestionsGridView.setLayoutManager(layoutManager);
        suggestionsGridView.setAdapter(adapter);
        suggestionsGridEmptyView = (ViewStub) view.findViewById(R.id.suggestion_grid_empty);
        suggestionsGridEmptyView.setOnInflateListener(new ViewStub.OnInflateListener() {

            @Override
            public void onInflate(ViewStub stub, View inflated) {
                final Button resetFilters = (Button) inflated.findViewById(R.id.reset_filters_button);
                resetFilters.setVisibility(BuildConfig.FIXED_BUNDLE ? View.GONE : View.VISIBLE);

                resetFilters.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        setInitialFilterSelection(false);
                        listener.onResetFilters();

                        updateGrid();
                    }
                });
            }
        });

        // Restore saved instance state, if any
        if (savedInstanceState != null) {
            searchQuery = savedInstanceState.getString(SEARCH_QUERY);
            languageFilter.setSelection(savedInstanceState.getInt(LANGUAGE_FILTER_POSITION));
            genreFilter.setSelection(savedInstanceState.getInt(GENRE_FILTER_POSITION));
            mediaTypeFilter.setSelection(savedInstanceState.getInt(MEDIATYPE_FILTER_POSITION));
        } else
            setInitialFilterSelection(true);

        // For now (while loading the suggestions) show the progress view.
        suggestionsGridView.setVisibility(View.GONE);
        suggestionsGridEmptyView.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SEARCH_QUERY, searchQuery);
        outState.putInt(LANGUAGE_FILTER_POSITION, languageFilter.getSelectedItemPosition());
        outState.putInt(GENRE_FILTER_POSITION, genreFilter.getSelectedItemPosition());
        outState.putInt(MEDIATYPE_FILTER_POSITION, mediaTypeFilter.getSelectedItemPosition());
    }

    @Override
    public void onDestroy() {
        PodcastManager.getInstance().removeChangePodcastListListener(this);

        super.onDestroy();
    }

    /**
     * Set list of suggestions to show and update the UI.
     *
     * @param suggestions Podcasts to show.
     */
    public void setList(List<Suggestion> suggestions) {
        this.suggestionList = suggestions;

        updateGrid();
    }

    /**
     * Filter suggestions by search query.
     *
     * @param query Text to search for in podcast title and keywords.
     */
    public void setSearchQuery(String query) {
        this.searchQuery = query;

        updateGrid();
    }

    /**
     * @return The currently active search query, might be <code>null</code>.
     */
    public String getSearchQuery() {
        return searchQuery;
    }

    /**
     * Show load suggestions progress.
     *
     * @param progress Progress information to give.
     */
    public void showLoadProgress(Progress progress) {
        progressView.publishProgress(progress);
    }

    /**
     * Show load failed for podcast suggestions.
     */
    public void showLoadFailed() {
        progressView.showError(R.string.suggestions_load_error);
    }

    @Override
    public void onPodcastAdded(Podcast podcast) {
        try {
            final int[] first = layoutManager.findFirstVisibleItemPositions(null);
            final int[] last = layoutManager.findLastVisibleItemPositions(null);

            // Alert all visible item view holders (using -1/+1 as security margins)
            for (int column = 0; column < first.length; column++) // first.length == last.length
                for (int index = first[column] - 1; index <= last[column]; index++) {
                    final SuggestionListItemViewHolder holder =
                            (SuggestionListItemViewHolder) suggestionsGridView.findViewHolderForAdapterPosition(index);

                    if (holder != null)
                        holder.onPodcastAdded(podcast);
                }
        } catch (NullPointerException npe) {
            // This will happen if the listener fires before any items are added,
            // safe to ignore.
        }
    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        // pass
    }

    private void setInitialFilterSelection(boolean resetLanguage) {
        // Reset search query
        this.searchQuery = null;

        // Set language according to locale
        if (resetLanguage)
            try {
                final Locale currentLocale = getActivity().getResources().getConfiguration().locale;
                final Language selectedLanguage = BuildConfig.FIXED_BUNDLE ?
                        Language.forTwoLetterIsoCode(BuildConfig.BUNDLE_LANGUAGE) :
                        Language.forTwoLetterIsoCode(currentLocale.getLanguage());
                for (int index = 0; index < languageFilter.getCount(); index++)
                    if (languageFilter.getItemAtPosition(index).equals(selectedLanguage))
                        languageFilter.setSelection(index);
            } catch (IllegalArgumentException iae) {
                // Unknown language, set to wildcard
                languageFilter.setSelection(0);
            }

        // Set category to wildcard
        genreFilter.setSelection(0);

        // Set type according to media type flavor (audio/video)
        if (BuildConfig.FLAVOR_media.equals("video") || BuildConfig.FIXED_BUNDLE)
            mediaTypeFilter.setSelection(0); // Show all suggestions
        else
            for (int index = 0; index < mediaTypeFilter.getCount(); index++)
                if (mediaTypeFilter.getItemAtPosition(index).equals(MediaType.AUDIO))
                    mediaTypeFilter.setSelection(index); // Show audio only
    }

    private void updateGrid() {
        // Filter the suggestion list and update the list adapter.
        if (suggestionList != null) {
            // Clear filtered list and apply current filter settings
            filteredSuggestionList.clear();
            for (Suggestion suggestion : suggestionList)
                if (matchesCurrentFilter(suggestion))
                    filteredSuggestionList.add(suggestion);

            // Update adapter
            final Language selectedLanguage = languageFilter.getSelectedItem() instanceof Language ?
                    (Language) languageFilter.getSelectedItem() : null;
            final Genre selectedGenre = genreFilter.getSelectedItem() instanceof Genre ?
                    (Genre) genreFilter.getSelectedItem() : null;
            final MediaType selectedType = mediaTypeFilter.getSelectedItem() instanceof MediaType ?
                    (MediaType) mediaTypeFilter.getSelectedItem() : null;
            adapter.setFilterConfiguration(selectedLanguage, selectedGenre, selectedType);
            adapter.notifyDataSetChanged();

            // Update view visibility
            progressView.setVisibility(View.GONE);
            suggestionsGridView.setVisibility(filteredSuggestionList.size() > 0 ? View.VISIBLE : View.GONE);
            suggestionsGridEmptyView.setVisibility(filteredSuggestionList.size() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Checks whether the given podcast matches the filter selection.
     *
     * @param suggestion Podcast to check.
     * @return <code>true</code> if the podcast fits.
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    private boolean matchesCurrentFilter(Suggestion suggestion) {
        final boolean matchesLanguage = languageFilter.getSelectedItemPosition() == 0 ||
                suggestion.getLanguages().contains(languageFilter.getSelectedItem());

        if (matchesLanguage && (genreFilter.getSelectedItemPosition() == 0 ||
                suggestion.getGenres().contains(genreFilter.getSelectedItem())) &&
                (mediaTypeFilter.getSelectedItemPosition() == 0 ||
                        suggestion.getMediaTypes().contains(mediaTypeFilter.getSelectedItem()))) {

            if (searchQuery != null && searchQuery.length() > 0)
                // Split search query at whitespaces and match all
                for (String token : searchQuery.split(" ")) {
                    // TODO It might be clever to use the locale of the podcast to lower the case?
                    final String searchingFor = token.trim().toLowerCase();
                    final String keywords = suggestion.getKeywords();

                    if (suggestion.getName().toLowerCase().contains(searchingFor) ||
                            (keywords != null && keywords.toLowerCase().contains(searchingFor)))
                        return true;
                }
            else
                return true; // No query, filter match
        }

        return false;
    }
}
