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

package com.podcatcher.deluxe.view;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.listeners.OnSelectPodcastListener;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.PodcastManager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * The spinner for the main action bar menu, that allows for the selection of
 * the content mode. This is a bit of a hack since it does not work exactly like
 * usual spinner, but allows the user to pick an action twice. The initial
 * default selection is ignored. In addition, we override the default behavior
 * to show the selected view when closed. Instead, we show another view with
 * name and status information.
 */
public class ContentSpinner extends Spinner implements OnItemSelectedListener {

    /**
     * The listener call-back to alert on content mode selection
     */
    private final OnSelectPodcastListener listener;

    /**
     * Handle to the closed spinner title text view
     */
    private final TextView closedTitleView;
    /**
     * Handle to the closed spinner subtitle text view
     */
    private final TextView closedSubtitleView;

    /**
     * Our spinner handle
     */
    private final NavigationSpinnerAdapter spinnerAdapter;

    /**
     * Create a new content mode spinner to be added as a custom view to the
     * app's action bar.
     *
     * @param context  Context the view lives in.
     * @param listener The action call-back to alert when a content selection is
     *                 made.
     */
    public ContentSpinner(Context context, OnSelectPodcastListener listener) {
        super(context, null, android.R.attr.actionDropDownStyle);

        this.listener = listener;
        this.spinnerAdapter = new NavigationSpinnerAdapter(this);

        closedTitleView = (TextView) spinnerAdapter.getClosedView().findViewById(R.id.title);
        closedSubtitleView = (TextView) spinnerAdapter.getClosedView().findViewById(R.id.subtitle);

        setAdapter(spinnerAdapter);
        setOnItemSelectedListener(this);
    }

    /**
     * Dummy constructor, used by tools only
     *
     * @param context Context the view lives in.
     */
    public ContentSpinner(Context context) {
        this(context, null);
    }

    /**
     * Set the main text for the closed spinner view.
     *
     * @param title Text to show.
     */
    public void setTitle(String title) {
        closedTitleView.setText(title);
    }

    /**
     * Set the sub title for the closed spinner view.
     *
     * @param subtitle The subtitle, set to <code>null</code> to hide.
     */
    public void setSubtitle(String subtitle) {
        closedSubtitleView.setText(subtitle);
        closedSubtitleView.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // The initial selection on creation of position 0 is ignored and
        // actions are only triggered for real user selections.
        if (position > 0) {
            switch (Long.valueOf(id).intValue()) {
                case R.string.podcast_select_all:
                    listener.onAllPodcastsSelected();
                    break;
                case R.string.downloads:
                    listener.onDownloadsSelected();
                    break;
                default:
                    // Nothing to do here
                    break;
            }

            // This invalidates the selection, the same item can be picked again
            setSelection(getAdapter().getCount());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // pass
    }

    /**
     * The data adapter used to populate the spinner
     */
    private static class NavigationSpinnerAdapter extends BaseAdapter {

        /**
         * The inflater we use
         */
        private final LayoutInflater inflater;
        /**
         * The view we return for a closed spinner
         */
        private final View closedView;

        public NavigationSpinnerAdapter(ViewGroup parent) {
            this.inflater = LayoutInflater.from(parent.getContext());
            this.closedView = inflater.inflate(R.layout.content_spinner_item, parent, false);

            // For the closed view, no padding is needed
            closedView.setPadding(0, 0, 0, 0);

            // Set the initial name and hide unneeded views
            ((TextView) closedView.findViewById(R.id.title)).setText(R.string.app_name);
            closedView.findViewById(R.id.subtitle).setVisibility(View.GONE);
            closedView.findViewById(R.id.icon).setVisibility(View.GONE);
        }

        /**
         * @return The view shown when the spinner is closed. This is never
         * <code>null</code>.
         */
        public View getClosedView() {
            return closedView;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            // case 0: is the dummy view
            switch (position) {
                case 1:
                    return R.string.podcast_select_all;
                case 2:
                    return R.string.downloads;
                default:
                    return 0;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // This is shown if the spinner is closed
            return closedView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            // Create the return view (this should not be recycled)
            final View spinnerItemView = inflater.inflate(
                    R.layout.content_spinner_item, parent, false);

            // Get handles on the view we need to update
            final ImageView imageView = (ImageView) spinnerItemView.findViewById(R.id.icon);
            final TextView titleView = (TextView) spinnerItemView.findViewById(R.id.title);
            final TextView subtitleView = (TextView) spinnerItemView.findViewById(R.id.subtitle);

            switch (position) {
                case 0:
                    // The initial selection dummy view should be hidden
                    spinnerItemView.getLayoutParams().height = 1;
                    break;
                case 1:
                    imageView.setImageResource(R.drawable.ic_menu_select_all);
                    titleView.setText(R.string.podcast_select_all);

                    // Set the subtitle
                    final int podcastCount = PodcastManager.getInstance().size();
                    if (podcastCount == 0)
                        subtitleView.setText(R.string.podcast_none);
                    else
                        subtitleView.setText(parent.getContext().getResources()
                                .getQuantityString(R.plurals.podcasts, podcastCount, podcastCount));
                    break;
                case 2:
                    imageView.setImageResource(R.drawable.ic_menu_download);
                    titleView.setText(R.string.downloads);

                    // Set the subtitle
                    final int downloadsCount = EpisodeManager.getInstance().getDownloadsSize();
                    setEpisodeNumberText(parent, subtitleView, downloadsCount);

                    break;
            }

            // Make sure to hide empty sub-titles
            if (subtitleView.getText().length() == 0)
                subtitleView.setVisibility(View.GONE);

            return spinnerItemView;
        }

        private void setEpisodeNumberText(ViewGroup parent, final TextView subtitleView,
                                          final int count) {
            if (count == 0)
                subtitleView.setText(null);
            else
                subtitleView.setText(parent.getContext().getResources()
                        .getQuantityString(R.plurals.episodes, count, count));
        }
    }
}
