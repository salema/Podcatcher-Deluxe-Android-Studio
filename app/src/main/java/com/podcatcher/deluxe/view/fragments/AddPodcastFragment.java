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
import com.podcatcher.deluxe.model.tasks.remote.LoadPodcastTask.PodcastLoadError;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.view.HorizontalProgressView;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import static android.view.View.VISIBLE;

/**
 * A dialog to let the user add a podcast. The activity that shows this need to
 * implement the {@link AddPodcastDialogListener}.
 */
public class AddPodcastFragment extends DialogFragment {

    /**
     * The wrapper layout for the podcast logo and name
     */
    private View podcastWrapper;
    /**
     * Podcast logo view
     */
    private ImageView podcastLogo;
    /**
     * Podcast name and caption text views
     */
    private TextView podcastName;
    private TextView podcastCaption;
    /**
     * The podcast URL text field
     */
    private EditText podcastUrlEditText;
    /**
     * The progress view
     */
    private HorizontalProgressView progressView;
    /**
     * The add podcast help button
     */
    private Button helpButton;
    /**
     * The add podcast button
     */
    private Button addPodcastButton;

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
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.podcast_add_title);

        // Prevent automatic display of the soft keyboard
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        podcastWrapper = view.findViewById(R.id.podcast_wrapper);
        podcastLogo = (ImageView) view.findViewById(R.id.podcast_logo);
        podcastName = (TextView) view.findViewById(R.id.podcast_name);
        podcastCaption = (TextView) view.findViewById(R.id.podcast_caption);

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
        else if (BuildConfig.DEBUG)
            podcastUrlEditText.setText("https://kevin:tantan@www.theskepticsguide.org/premium");
            // This checks for a potential podcast URL in the clipboard
            // and presets it in the text field if available
        else
            checkClipboardForPodcastUrl();

        progressView = (HorizontalProgressView) view.findViewById(R.id.add_podcast_progress);

        addPodcastButton = (Button) view.findViewById(R.id.podcast_add_button);
        addPodcastButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                addPodcast();
            }
        });
        helpButton = (Button) view.findViewById(R.id.podcast_add_help_button);
        helpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onShowHelp();
            }
        });

        // Make sure the podcast logo height matches the dialog width
        view.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @Override
                    public void onGlobalLayout() {
                        // We only need this once
                        // noinspection deprecation - need to call old method for API 14/15
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                        // Update new layout params
                        final ViewGroup.LayoutParams layoutParams = podcastWrapper.getLayoutParams();
                        layoutParams.height = view.getWidth();
                        podcastWrapper.setLayoutParams(layoutParams);
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        // Resize the dialog to a good fit, it can't be too big because
        // we might show the podcast logo. Only needed if podcast is preloaded.
        if (getActivity().getIntent().getData() != null) {
            final DisplayMetrics metrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
            final boolean landscape = metrics.widthPixels > metrics.heightPixels;
            final int width = (landscape ? metrics.heightPixels : metrics.widthPixels) * 7 / 10;
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
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

            case NOT_PARSABLE:
                progressView.showError(R.string.podcast_load_error_not_parsable);
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

    /**
     * Make the dialog change from generic mode to show given podcast.
     *
     * @param podcast Podcast to add.
     */
    public void showPodcast(Podcast podcast) {
        podcastWrapper.setVisibility(View.VISIBLE);
        podcastUrlEditText.setVisibility(View.GONE);
        progressView.setVisibility(View.GONE);
        helpButton.setVisibility(View.GONE);
        addPodcastButton.setEnabled(true);

        podcastName.setText(podcast.getName());
        podcastCaption.setText(getResources().getQuantityString(R.plurals.episodes,
                podcast.getEpisodeCount(), podcast.getEpisodeCount()));

        hideTitleBar();
        if (podcast.getLogoUrl() != null && podcast.getLogoUrl().startsWith("http"))
            Picasso.with(getActivity())
                    .load(podcast.getLogoUrl())
                    .fit().centerCrop() // Resize logo and crop to fit image view
                    .into(podcastLogo);
    }

    private void addPodcast() {
        showProgress(Progress.WAIT);

        // Try to make the input work as an online resource
        String spec = podcastUrlEditText.getText().toString();
        if (!URLUtil.isNetworkUrl(spec)) {
            spec = "http://" + spec;
            podcastUrlEditText.setText(spec);
        }

        final CharSequence name = podcastName.getText();
        listener.onAddPodcast(name != null && name.length() > 0 ? name.toString() : null, spec);
    }

    private void checkClipboardForPodcastUrl() {
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);

        // Get the value to paste (make this fail safe)
        if (clipboard != null && clipboard.hasPrimaryClip()
                && clipboard.getPrimaryClip().getItemCount() > 0) {
            CharSequence candidate = clipboard.getPrimaryClip().getItemAt(0).getText();

            // Check whether this might be a podcast RSS online resource, if so
            // set text field
            if (candidate != null && URLUtil.isNetworkUrl(candidate.toString()))
                podcastUrlEditText.setText(candidate);
        }
    }

    private void hideTitleBar() {
        try {
            final int titleViewId = getResources().getIdentifier("android:id/title", null, null);
            final int dividerViewId = getResources().getIdentifier("android:id/titleDivider", null, null);
            final TextView titleView = (TextView) getDialog().findViewById(titleViewId);
            final View divider = getDialog().findViewById(dividerViewId);

            titleView.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        } catch (RuntimeException npe) {
            // Simply do not apply color
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
         * @param name Podcast title, can be <code>null</code>.
         * @param url  Podcast URL spec to add.
         */
        void onAddPodcast(String name, String url);

        /**
         * Called on the listener when the user presses the help button.
         */
        void onShowHelp();
    }
}
