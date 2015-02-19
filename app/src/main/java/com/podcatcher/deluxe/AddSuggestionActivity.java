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

package com.podcatcher.deluxe;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.podcatcher.deluxe.listeners.OnLoadSuggestionListener;
import com.podcatcher.deluxe.model.SuggestionManager;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;
import com.podcatcher.deluxe.view.fragments.ConfirmExplicitSuggestionFragment;
import com.podcatcher.deluxe.view.fragments.ConfirmExplicitSuggestionFragment.ConfirmExplicitSuggestionDialogListener;
import com.podcatcher.deluxe.view.fragments.SuggestionFragment;
import com.podcatcher.deluxe.view.fragments.SuggestionFragment.AddSuggestionDialogListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Add podcast from suggestions activity.
 */
public class AddSuggestionActivity extends BaseActivity implements AddSuggestionDialogListener,
        ConfirmExplicitSuggestionDialogListener, OnLoadSuggestionListener {

    /**
     * Tag to find the add suggestion dialog fragment under
     */
    private static final String ADD_SUGGESTION_DIALOG_TAG = "add_suggestion_dialog";
    /**
     * Key to find "podcast to be confirmed" URL under
     */
    private static final String TO_BE_CONFIRMED_URL_KEY = "TO_BE_CONFIRMED_URL_KEY";
    /**
     * Key to find "podcast to be confirmed" name under
     */
    private static final String TO_BE_CONFIRMED_NAME_KEY = "TO_BE_CONFIRMED_NAME_KEY";
    /**
     * The suggestion manager handle
     */
    private SuggestionManager suggestionManager;
    /**
     * The fragment containing the add suggestion UI
     */
    private SuggestionFragment suggestionFragment;
    /**
     * Helper to store suggestion awaiting confirmation
     */
    private Suggestion suggestionToBeConfirmed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create and show suggestion fragment
        if (savedInstanceState == null) {
            this.suggestionFragment = new SuggestionFragment();
            // Need to set style, because this activity has no UI
            suggestionFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialog);

            suggestionFragment.show(getFragmentManager(), ADD_SUGGESTION_DIALOG_TAG);
        } else {
            this.suggestionFragment = (SuggestionFragment)
                    getFragmentManager().findFragmentByTag(ADD_SUGGESTION_DIALOG_TAG);

            // Restore "suggestion to be confirmed" member
            if (savedInstanceState.containsKey(TO_BE_CONFIRMED_URL_KEY))
                suggestionToBeConfirmed = new Suggestion(
                        savedInstanceState.getString(TO_BE_CONFIRMED_NAME_KEY),
                        savedInstanceState.getString(TO_BE_CONFIRMED_URL_KEY));
        }

        // Get suggestions manager and register call-back
        suggestionManager = SuggestionManager.getInstance();
        suggestionManager.addLoadSuggestionListListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load suggestions (this has to be called after UI fragment is created)
        suggestionManager.load();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (suggestionToBeConfirmed != null) {
            outState.putString(TO_BE_CONFIRMED_URL_KEY, suggestionToBeConfirmed.getUrl());
            outState.putString(TO_BE_CONFIRMED_NAME_KEY, suggestionToBeConfirmed.getName());
        }
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
        for (Suggestion suggestion : suggestions)
            if (!podcastManager.contains(suggestion) &&
                    !(podcastManager.blockExplicit() && suggestion.isExplicit()))
                filteredSuggestionList.add(suggestion);

        // Filter list and update UI
        suggestionFragment.setList(filteredSuggestionList);
    }

    @Override
    public void onSuggestionsLoadFailed() {
        suggestionFragment.showLoadFailed();
    }

    @Override
    public void onAddSuggestion(Suggestion suggestion) {
        if (suggestion.isExplicit()) {
            this.suggestionToBeConfirmed = suggestion;

            // Show confirmation dialog
            new ConfirmExplicitSuggestionFragment().show(getFragmentManager(), null);
        } else {
            podcastManager.addPodcast(suggestion);
            suggestionFragment.notifySuggestionAdded();
        }
    }

    @Override
    public void onConfirmExplicit() {
        podcastManager.addPodcast(suggestionToBeConfirmed);
        suggestionFragment.notifySuggestionAdded();
    }

    @Override
    public void onCancelExplicit() {
        // Nothing to do here...
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
