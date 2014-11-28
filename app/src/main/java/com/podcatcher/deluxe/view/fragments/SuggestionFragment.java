/** Copyright 2012-2014 Kevin Hausmann
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

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.adapters.GenreSpinnerAdapter;
import com.podcatcher.deluxe.adapters.LanguageSpinnerAdapter;
import com.podcatcher.deluxe.adapters.MediaTypeSpinnerAdapter;
import com.podcatcher.deluxe.adapters.SuggestionListAdapter;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;
import com.podcatcher.deluxe.view.ProgressView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment to show podcast suggestions.
 */
public class SuggestionFragment extends DialogFragment {

    /**
     * The filter wildcard
     */
    public static final String FILTER_WILDCARD = "ALL";
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
     * Video media type flavor identifier, used for drop down pre-selection
     */
    private static final String VIDEO_MEDIA_TYPE_FLAVOR = "video";

    /**
     * The listener to update the list on filter change
     */
    private final OnItemSelectedListener selectionListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updateList();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            updateList();
        }
    };

    /**
     * The list of suggestions to show
     */
    private List<Suggestion> suggestionList;
    /**
     * The suggestion list adapter
     */
    private SuggestionListAdapter suggestionListAdapter;
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
    private ListView suggestionsListView;
    /**
     * The suggestions list empty view
     */
    private ViewStub suggestionsListEmptyView;

    /**
     * The call back we work on
     */
    private AddSuggestionDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (AddSuggestionDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AddSuggestionDialogListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        final View layout = inflater.inflate(R.layout.suggestion_list, container, false);

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
        getDialog().setTitle(R.string.suggested_podcasts);

        languageFilter = (Spinner) view.findViewById(R.id.suggestion_language_select);
        languageFilter.setAdapter(new LanguageSpinnerAdapter(getDialog().getContext()));
        languageFilter.setOnItemSelectedListener(selectionListener);

        genreFilter = (Spinner) view.findViewById(R.id.suggestion_genre_select);
        genreFilter.setAdapter(new GenreSpinnerAdapter(getDialog().getContext()));
        genreFilter.setOnItemSelectedListener(selectionListener);

        mediaTypeFilter = (Spinner) view.findViewById(R.id.suggestion_type_select);
        mediaTypeFilter.setAdapter(new MediaTypeSpinnerAdapter(getDialog().getContext()));
        mediaTypeFilter.setOnItemSelectedListener(selectionListener);

        progressView = (ProgressView) view.findViewById(R.id.suggestion_list_progress);
        suggestionsListView = (ListView) view.findViewById(R.id.suggestion_list);
        suggestionsListEmptyView = (ViewStub) view.findViewById(R.id.suggestion_list_empty);
        suggestionsListEmptyView.setOnInflateListener(new ViewStub.OnInflateListener() {

            @Override
            public void onInflate(ViewStub stub, View inflated) {
                final Button resetFilters = (Button) inflated
                        .findViewById(R.id.reset_filters_button);

                resetFilters.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        setInitialFilterSelection();
                    }
                });
            }
        });

        // For now (while loading the suggestions) set the empty view of the
        // list to be the progress view.
        suggestionsListEmptyView.setVisibility(View.GONE);
        suggestionsListView.setEmptyView(progressView);

        TextView sendSuggestionView = (TextView) view.findViewById(R.id.suggestion_send);
        sendSuggestionView.setText(Html.fromHtml("<a href=\"mailto:" +
                getString(R.string.suggestion_address) + "?subject=" +
                getString(R.string.suggestion_subject,
                        getString(R.string.app_name), BuildConfig.STORE) + "\">" +
                getString(R.string.suggestions_send) + "</a>"));
        sendSuggestionView.setMovementMethod(LinkMovementMethod.getInstance());

        // Set/restore filter settings
        // Coming from configuration change
        if (savedInstanceState != null) {
            languageFilter.setSelection(savedInstanceState.getInt(LANGUAGE_FILTER_POSITION));
            genreFilter.setSelection(savedInstanceState.getInt(GENRE_FILTER_POSITION));
            mediaTypeFilter.setSelection(savedInstanceState.getInt(MEDIATYPE_FILTER_POSITION));
        } // Initial opening of the dialog
        else
            setInitialFilterSelection();
    }

    @Override
    public void onResume() {
        super.onResume();

        // The list might have changed while we were paused
        updateList();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(LANGUAGE_FILTER_POSITION, languageFilter.getSelectedItemPosition());
        outState.putInt(GENRE_FILTER_POSITION, genreFilter.getSelectedItemPosition());
        outState.putInt(MEDIATYPE_FILTER_POSITION, mediaTypeFilter.getSelectedItemPosition());
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Make sure the parent activity knows when we are closing
        listener.onCancel(dialog);

        super.onCancel(dialog);
    }

    /**
     * Set list of suggestions to show and update the UI.
     *
     * @param suggestions Podcasts to show.
     */
    public void setList(List<Suggestion> suggestions) {
        // Set the list to show
        this.suggestionList = suggestions;

        // Filter list and update UI (if ready)
        if (isResumed())
            updateList();
    }

    /**
     * Notify the fragment that a suggestion as been added and the list might
     * have to update.
     */
    public void notifySuggestionAdded() {
        if (suggestionListAdapter != null)
            suggestionListAdapter.notifyDataSetChanged();
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

    private void setInitialFilterSelection() {
        // Set according to locale
        final Locale currentLocale = getActivity().getResources().getConfiguration().locale;
        if (currentLocale.getLanguage().equalsIgnoreCase(Locale.ENGLISH.getLanguage()))
            languageFilter.setSelection(1);
        else if (currentLocale.getLanguage().equalsIgnoreCase(Locale.FRENCH.getLanguage()))
            languageFilter.setSelection(4);
        else if (currentLocale.getLanguage().equalsIgnoreCase(Locale.GERMAN.getLanguage()))
            languageFilter.setSelection(1);
        else if (currentLocale.getLanguage().equalsIgnoreCase("es"))
            languageFilter.setSelection(2);
            // No filter for this language, set to "all"
        else
            languageFilter.setSelection(0);

        // Set to "all"
        genreFilter.setSelection(0);
        // Set according to media type flavor (audio/video)
        mediaTypeFilter.setSelection(BuildConfig.FLAVOR_media.equals(VIDEO_MEDIA_TYPE_FLAVOR) ? 2 : 1);
    }

    private void updateList() {
        // Filter the suggestion list and update the list adapter. This should
        // not run too early since some pieces might not be in place and
        // onResume() will call us anyway.
        if (suggestionList != null && isResumed()) {
            // Resulting list
            List<Suggestion> filteredSuggestionList = new ArrayList<>();
            // Do filter!
            for (Suggestion suggestion : suggestionList)
                if (matchesFilter(suggestion))
                    filteredSuggestionList.add(suggestion);

            // Set filtered list
            suggestionListAdapter = new SuggestionListAdapter(
                    getDialog().getContext(), filteredSuggestionList, listener);
            suggestionListAdapter.setFilterConfiguration(
                    languageFilter.getSelectedItemPosition() == 0,
                    genreFilter.getSelectedItemPosition() == 0,
                    mediaTypeFilter.getSelectedItemPosition() == 0);
            suggestionsListView.setAdapter(suggestionListAdapter);

            // As soon as the list is available, hide progress and switch to the
            // "real" empty view for the list
            progressView.setVisibility(View.GONE);
            suggestionsListView.setEmptyView(suggestionsListEmptyView);
        }
    }

    /**
     * Checks whether the given podcast matches the filter selection.
     *
     * @param suggestion Podcast to check.
     * @return <code>true</code> if the podcast fits.
     */
    private boolean matchesFilter(Suggestion suggestion) {
        return (languageFilter.getSelectedItemPosition() == 0 ||
                languageFilter.getSelectedItem().equals(suggestion.getLanguage())) &&
                (genreFilter.getSelectedItemPosition() == 0 ||
                        genreFilter.getSelectedItem().equals(suggestion.getGenre())) &&
                (mediaTypeFilter.getSelectedItemPosition() == 0 ||
                        mediaTypeFilter.getSelectedItem().equals(suggestion.getMediaType()));
    }

    /**
     * Interface definition for a callback to be invoked when the user interacts
     * with the add podcast suggestion dialog.
     */
    public interface AddSuggestionDialogListener extends OnCancelListener {

        /**
         * Called on listener when podcast suggestion is selected.
         *
         * @param suggestion Podcast to add.
         */
        public void onAddSuggestion(Suggestion suggestion);
    }
}
