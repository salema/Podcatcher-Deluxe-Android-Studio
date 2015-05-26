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

package com.podcatcher.deluxe.adapters;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.view.PodcastListItemView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Adapter class used for the list of podcasts.
 */
public class PodcastListAdapter extends PodcatcherBaseListAdapter {

    /**
     * The list our data resides in
     */
    protected List<Podcast> list;
    /**
     * Member flag to indicate whether we show the podcast logo
     */
    protected boolean showLogoView = false;

    /**
     * Create new adapter.
     *
     * @param context     The current context.
     * @param podcastList List of podcasts to wrap (not <code>null</code>).
     */
    public PodcastListAdapter(Context context, List<Podcast> podcastList) {
        super(context);

        this.list = podcastList;
    }

    /**
     * Replace the current podcast list with a new one.
     *
     * @param newList The new list (not <code>null</code>).
     */
    public void updateList(List<Podcast> newList) {
        this.list = newList;

        notifyDataSetChanged();
    }

    /**
     * Set whether the podcast logo should be shown. This will redraw the list
     * and take effect immediately.
     *
     * @param show Whether to show each podcast's logo.
     */
    public void setShowLogo(boolean show) {
        this.showLogoView = show;

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return list.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final PodcastListItemView returnView = (PodcastListItemView)
                findReturnView(convertView, parent, R.layout.podcast_list_item);

        // Make sure the coloring is right
        setBackgroundColorForPosition(returnView, position);
        // Make the view represent podcast at given position
        returnView.show((Podcast) getItem(position), showLogoView, selectAll);

        return returnView;
    }
}
