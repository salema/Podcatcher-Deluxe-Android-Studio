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

package com.podcatcher.deluxe.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.view.EpisodeListItemView;

import java.util.List;

/**
 * Adapter class used for the list of episodes.
 */
public class EpisodeListAdapter extends PodcatcherBaseListAdapter {

    /**
     * The list our data resides in
     */
    protected List<Episode> list;
    /**
     * Whether the podcast name should be shown
     */
    protected boolean showPodcastNames = false;

    /**
     * Our episode manager handle
     */
    private EpisodeManager episodeManager;

    /**
     * Create new adapter.
     *
     * @param context     The activity.
     * @param episodeList The list of episodes to show in list.
     */
    public EpisodeListAdapter(Context context, List<Episode> episodeList) {
        super(context);

        this.list = episodeList;
        this.episodeManager = EpisodeManager.getInstance();
    }

    /**
     * Replace the current episode list with a new one.
     *
     * @param episodeList The new list (not <code>null</code>).
     */
    public void updateList(List<Episode> episodeList) {
        this.list = episodeList;

        notifyDataSetChanged();
    }

    /**
     * Set whether the podcast name for the episode should be shown. This will
     * redraw the list and take effect immediately.
     *
     * @param show Whether to show each episode's podcast name.
     */
    public void setShowPodcastNames(boolean show) {
        this.showPodcastNames = show;

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
        final EpisodeListItemView returnView = (EpisodeListItemView)
                findReturnView(convertView, parent, R.layout.episode_list_item);
        final Episode item = (Episode) getItem(position);
        final boolean isOld = episodeManager.getState(item);

        // Make sure the coloring is right
        if (checkedPositions.get(position))
            returnView.setBackgroundDrawable(createBackground(lightThemeColor));
        else if (selectedPositions.get(position))
            returnView.setBackgroundDrawable(createBackground(themeColor));
        else if (!isOld)
            returnView.setBackgroundDrawable(createBackground(Color.WHITE));
        else
            returnView.setBackgroundDrawable(createBackground(Color.TRANSPARENT));

        // Make the view represent episode at given position
        returnView.show(item, showPodcastNames, isOld);

        return returnView;
    }

    private StateListDrawable createBackground(int defaultColor) {
        final StateListDrawable background = new StateListDrawable();

        // This is needed because we want the list selector to be visible even
        // though is not drawn on top. To this goal, we set the background to
        // transparent when the list item is pressed.
        background.addState(new int[]{
                android.R.attr.state_pressed
        }, new ColorDrawable(Color.TRANSPARENT));
        background.addState(new int[]{
                android.R.attr.state_focused
        }, new ColorDrawable(Color.TRANSPARENT));
        background.addState(new int[]{
                android.R.attr.state_selected
        }, new ColorDrawable(Color.TRANSPARENT));
        background.addState(new int[]{}, new ColorDrawable(defaultColor));

        return background;
    }
}
