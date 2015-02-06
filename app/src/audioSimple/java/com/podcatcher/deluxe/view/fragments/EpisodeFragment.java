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

package com.podcatcher.deluxe.view.fragments;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.listeners.OnDownloadEpisodeListener;
import com.podcatcher.deluxe.model.ParserUtils;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.view.Utils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Fragment showing episode details.
 */
public class EpisodeFragment extends Fragment {

    /**
     * The listener for the menu item
     */
    private OnDownloadEpisodeListener downloadListener;
    /**
     * The currently shown episode
     */
    private Episode currentEpisode;

    /**
     * Flag for show download menu item state
     */
    private boolean showDownloadMenuItem = false;
    /**
     * Flag for the state of the download menu item
     */
    private boolean downloadMenuItemState = true;
    /**
     * Flag for show download icon state
     */
    private boolean showDownloadIcon = false;
    /**
     * Flag for the state of the download icon
     */
    private boolean downloadIconState = true;

    /**
     * Separator for date and podcast name
     */
    private static final String SEPARATOR = " • ";
    /**
     * The mime type set for the episode description
     */
    private static final String EPISODE_DESCRIPTION_MIME_TYPE = "text/html";
    /**
     * The encoding of the episode description if not specified
     */
    private static final String EPISODE_DESCRIPTION_DEFAULT_ENCODING = "UTF-8";

    /**
     * Status flag indicating that our view is created
     */
    private boolean viewCreated = false;
    /**
     * Flag for transition animation fix
     */
    private boolean needsLayoutTransitionFix = true;

    /**
     * The download episode menu bar item
     */
    private MenuItem downloadMenuItem;

    /**
     * The empty view
     */
    private View emptyView;
    /**
     * The episode title view
     */
    private TextView titleView;
    /**
     * The podcast title view
     */
    private TextView subtitleView;
    /**
     * The download icon view
     */
    private ImageView downloadIconView;
    /**
     * The divider view between title and description
     */
    private View dividerView;
    /**
     * The metadata box label
     */
    private TextView metadataBoxTextView;
    /**
     * The metadata box label divider
     */
    private View metadataBoxDivider;
    /**
     * The episode description web view
     */
    private WebView descriptionView;

    /**
     * The ad shown under episode description
     */
    private String ad;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Make sure our listener is present
        try {
            this.downloadListener = (OnDownloadEpisodeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnDownloadEpisodeListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prepare the ad
        String audioLink = "<a href=\"" + BuildConfig.STORE_URL_PREFIX +
                "com.podcatcher.deluxe\">Podcatcher Deluxe</a>";
        String videoLink = "<a href=\"" + BuildConfig.STORE_URL_PREFIX +
                "com.podcatcher.deluxe.video\">Video Podcatcher Deluxe</a>";
        ad = "<hr style=\"color: gray; width: 100%\">" +
                "<div style=\"color: gray; font-size: smaller; text-align: center; width: 100%\">" +
                getString(R.string.ad, audioLink, videoLink) + "</div>";

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.episode, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find UI widgets
        emptyView = view.findViewById(android.R.id.empty);
        titleView = (TextView) view.findViewById(R.id.episode_title);
        subtitleView = (TextView) view.findViewById(R.id.podcast_title);
        downloadIconView = (ImageView) view.findViewById(R.id.download_icon);
        dividerView = view.findViewById(R.id.episode_divider);
        metadataBoxTextView = (TextView) view.findViewById(R.id.metadata_box);
        metadataBoxDivider = view.findViewById(R.id.metadata_box_divider);

        // Get and configure the web view showing the episode description
        descriptionView = (WebView) view.findViewById(R.id.episode_description);
        final WebSettings settings = descriptionView.getSettings();
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        viewCreated = true;

        // This will make sure we show the right information once the view
        // controls are established (the episode might have been set earlier)
        if (currentEpisode != null) {
            setEpisode(currentEpisode);
            setDownloadIconVisibility(showDownloadIcon, downloadIconState);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.episode, menu);

        downloadMenuItem = menu.findItem(R.id.episode_download_menuitem);
        setDownloadMenuItemVisibility(showDownloadMenuItem, downloadMenuItemState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.episode_download_menuitem:
                // Tell activity to load/unload the current episode
                downloadListener.onToggleDownload();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        viewCreated = false;

        super.onDestroyView();
    }

    /**
     * Set the displayed episode, all UI will be updated.
     *
     * @param selectedEpisode Episode to show.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setEpisode(Episode selectedEpisode) {
        // Set handle to episode in case we are not resumed
        this.currentEpisode = selectedEpisode;

        // If the fragment's view is actually visible and the episode is
        // valid,
        // show episode information
        if (viewCreated && currentEpisode != null) {
            // Title and sub-title
            titleView.setText(currentEpisode.getName());
            subtitleView.setText(currentEpisode.getPodcast().getName());
            // Episode publication data
            if (currentEpisode.getPubDate() != null)
                subtitleView.setText(subtitleView.getText() + SEPARATOR
                        + Utils.getRelativePubDate(currentEpisode));

            // Episode metadata
            metadataBoxTextView.setText(createMetadataBoxText());

            // Set episode description
            final boolean isNewWebView = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
            final boolean hasHtmlDescription = currentEpisode.getLongDescription() != null;
            final String encoding = currentEpisode.getPodcast().getFeedEncoding();

            final WebSettings settings = descriptionView.getSettings();
            settings.setLoadWithOverviewMode(isNewWebView && hasHtmlDescription);
            settings.setUseWideViewPort(isNewWebView && hasHtmlDescription);
            settings.setLayoutAlgorithm(isNewWebView ?
                    hasHtmlDescription ?
                            WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING :
                            WebSettings.LayoutAlgorithm.NORMAL :
                    WebSettings.LayoutAlgorithm.SINGLE_COLUMN);

            final String webViewData = (!hasHtmlDescription ?
                    currentEpisode.getDescription() == null ?
                            getString(R.string.episode_no_description) :
                            currentEpisode.getDescription() :
                    currentEpisode.getLongDescription()) + ad;
            descriptionView.loadDataWithBaseURL(null, // Even a null baseURL somehow helps
                    webViewData,
                    EPISODE_DESCRIPTION_MIME_TYPE,
                    encoding != null ? encoding : EPISODE_DESCRIPTION_DEFAULT_ENCODING,
                    null);
        }

        // Update the UI widget's visibility to reflect state
        updateUiElementVisibility();

        // This is a workaround for the fact that declaring animateLayoutChanges
        // in combination with a webview breaks the fragment on some devices
        // (such as the HP Touchpad). Activating the layout transition after the
        // view has been shown once, works.
        if (needsLayoutTransitionFix && viewCreated) {
            ViewGroup parent = (ViewGroup) getView().getParent();

            // In small view we need to go two steps up
            if (!(parent instanceof LinearLayout))
                parent = (ViewGroup) parent.getParent();

            if (parent.getLayoutTransition() == null) {
                parent.setLayoutTransition(new LayoutTransition());
                needsLayoutTransitionFix = false;
            }
        }
    }

    /**
     * Set whether the fragment should show the download menu item. You can call
     * this any time and can expect it to happen on menu creation at the latest.
     * You also have to set the download menu state, <code>true</code> for
     * "Download" and <code>false</code> for "Delete".
     *
     * @param show     Whether to show the download menu item.
     * @param download State of the download menu item (download / delete)
     */
    public void setDownloadMenuItemVisibility(boolean show, boolean download) {
        this.showDownloadMenuItem = show;
        this.downloadMenuItemState = download;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (downloadMenuItem != null) {
            downloadMenuItem.setVisible(show);

            downloadMenuItem.setTitle(download ? R.string.download : R.string.remove);
            downloadMenuItem.setIcon(download ? R.drawable.ic_menu_download
                    : R.drawable.ic_menu_delete);
        }
    }

    /**
     * Set whether the fragment should show the download icon. You can call this
     * any time and can expect it to happen on fragment resume at the latest.
     * You also have to set the download icon state, <code>true</code> for
     * "is downloaded" and <code>false</code> for "is currently downloading".
     *
     * @param show       Whether to show the download menu item.
     * @param downloaded State of the download menu item (download / delete)
     */
    public void setDownloadIconVisibility(boolean show, boolean downloaded) {
        this.showDownloadIcon = show;
        this.downloadIconState = downloaded;

        // Only do it right away if resumed and menu item is available,
        // otherwise onResume or the menu creation callback will call us.
        if (viewCreated) {
            downloadIconView.setVisibility(show ? VISIBLE : GONE);
            downloadIconView.setImageResource(downloaded ?
                    R.drawable.ic_internal_storage : R.drawable.ic_media_downloading);
        }
    }

    /**
     * Update the episode metadata displayed. This will check the metadata for the
     * currently set episode and make sure any updated values are shown.
     */
    public void updateEpisodeMetadata() {
        if (viewCreated && currentEpisode != null) {
            metadataBoxTextView.setText(createMetadataBoxText());
            updateUiElementVisibility();
        }
    }

    private void updateUiElementVisibility() {
        if (viewCreated) {
            emptyView.setVisibility(currentEpisode == null ? VISIBLE : GONE);

            titleView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            subtitleView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            dividerView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
            metadataBoxTextView.setVisibility(currentEpisode == null ? GONE :
                    metadataBoxTextView.getText().length() == 0 ? GONE : VISIBLE);
            metadataBoxDivider.setVisibility(currentEpisode == null ? GONE :
                    metadataBoxTextView.getText().length() == 0 ? GONE : VISIBLE);
            descriptionView.setVisibility(currentEpisode == null ? GONE : VISIBLE);
        }
    }

    private String createMetadataBoxText() {
        final StringBuilder builder = new StringBuilder();

        if (currentEpisode.getDuration() > 0)
            builder.append(ParserUtils.formatTime(currentEpisode.getDuration())).append(SEPARATOR);
        if (currentEpisode.getFileSize() > 0)
            builder.append(ParserUtils.formatFileSize(currentEpisode.getFileSize())).append(SEPARATOR);
        if (currentEpisode.getMediaType() != null)
            builder.append(currentEpisode.getMediaType());

        String result = builder.toString();
        if (result.endsWith(SEPARATOR))
            result = result.substring(0, result.length() - SEPARATOR.length());

        // At least one numerical information should be there, otherwise return null
        return currentEpisode.getDuration() > 0 || currentEpisode.getFileSize() > 0 ? result : null;
    }
}
