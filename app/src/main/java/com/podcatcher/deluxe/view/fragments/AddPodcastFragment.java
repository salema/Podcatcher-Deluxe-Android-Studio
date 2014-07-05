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
import android.app.DialogFragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.podcatcher.deluxe.Podcatcher;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.view.HorizontalProgressView;

import static android.view.View.VISIBLE;

/**
 * A dialog to let the user add a podcast. The activity that shows this need to
 * implement the {@link AddPodcastDialogListener}.
 */
public class AddPodcastFragment extends DialogFragment {

    /**
     * The podcast URL text field
     */
    private EditText podcastUrlEditText;
    /**
     * The progress view
     */
    private HorizontalProgressView progressView;
    /**
     * The show suggestions button
     */
    private Button showSuggestionsButton;
    /**
     * The add podcast button
     */
    private Button addPodcastButton;
    /**
     * The import OPML button
     */
    private Button importOpmlButton;

    /**
     * The listener we report back to
     */
    private AddPodcastDialogListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.listener = (AddPodcastDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement AddPodcastDialogListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.add_podcast, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.podcast_add_title);

        // Prevent automatic display of the soft keyboard
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        podcastUrlEditText = (EditText) view.findViewById(R.id.podcast_url);
        podcastUrlEditText.setOnEditorActionListener(new OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (EditorInfo.IME_ACTION_GO == actionId)
                    addPodcast();

                return EditorInfo.IME_ACTION_GO == actionId;
            }
        });

        // Put the URI given in the intent if any
        if (getActivity().getIntent().getData() != null)
            podcastUrlEditText.setText(getActivity().getIntent().getDataString());
            // This is for testing only
        else if (((Podcatcher) getActivity().getApplication()).isInDebugMode())
            podcastUrlEditText.setText("https://www.theskepticsguide.org/premium");
            // This checks for a potential podcast URL in the clipboard
            // and presets it in the text field if available
        else
            checkClipboardForPodcastUrl();

        progressView = (HorizontalProgressView) view.findViewById(R.id.add_podcast_progress);

        showSuggestionsButton = (Button) view.findViewById(R.id.suggestion_add_button);
        showSuggestionsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onShowSuggestions();
            }
        });

        addPodcastButton = (Button) view.findViewById(R.id.podcast_add_button);
        addPodcastButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addPodcast();
            }
        });

        importOpmlButton = (Button) view.findViewById(R.id.import_opml_button);
        importOpmlButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onImportOpml();
            }
        });
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // Make sure the parent activity knows when we are closing
        listener.onCancel(dialog);

        super.onCancel(dialog);
    }

    /**
     * Show progress in the dialog.
     *
     * @param progress Progress information to show.
     */
    public void showProgress(Progress progress) {
        // Prepare UI
        podcastUrlEditText.setEnabled(false);
        addPodcastButton.setEnabled(false);
        progressView.setVisibility(VISIBLE);

        // Show progress
        progressView.publishProgress(progress);
    }

    /**
     * Show load failure in the dialog UI.
     *
     * @param code The error code loading the podcast failed with.
     */
    public void showPodcastLoadFailed(PodcastLoadError code) {
        switch (code) {
            case ACCESS_DENIED:
                progressView.showError(R.string.podcast_load_error_access_denied);
                break;

            case NOT_PARSEABLE:
                progressView.showError(R.string.podcast_load_error_not_parseable);
                break;

            case NOT_REACHABLE:
                progressView.showError(R.string.podcast_load_error_not_reachable);
                break;

            default:
                progressView.showError(R.string.podcast_load_error);
                break;
        }

        podcastUrlEditText.setEnabled(true);
        addPodcastButton.setEnabled(true);
    }

    private void addPodcast() {
        showProgress(Progress.WAIT);

        // Try to make the input work as a online resource
        String spec = podcastUrlEditText.getText().toString();
        if (!URLUtil.isNetworkUrl(spec)) {
            spec = "http://" + spec;
            podcastUrlEditText.setText(spec);
        }

        listener.onAddPodcast(spec);
    }

    private void checkClipboardForPodcastUrl() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);

        // Get the value to paste (make this failsafe)
        if (clipboard != null && clipboard.hasPrimaryClip()
                && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence candidate = clipboard.getPrimaryClip().getItemAt(0).getText();

            // Check whether this might be a podcast RSS online resource, if so
            // set text field
            if (candidate != null && URLUtil.isNetworkUrl(candidate.toString()))
                podcastUrlEditText.setText(candidate);
        }
    }

    /**
     * Interface definition for a callback to be invoked when the user interacts
     * with the add podcast dialog.
     */
    public interface AddPodcastDialogListener extends OnCancelListener {

        /**
         * Called on listener when podcast url is given.
         *
         * @param podcastUrl Podcast URL spec to add.
         */
        public void onAddPodcast(String podcastUrl);

        /**
         * Called on listener if the user wants to see suggestions for podcasts
         * to add.
         */
        public void onShowSuggestions();

        /**
         * Called on listener if the user wants to import an OPML file.
         */
        public void onImportOpml();
    }
}
