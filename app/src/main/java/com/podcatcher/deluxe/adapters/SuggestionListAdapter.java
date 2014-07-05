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
import android.view.View;
import android.view.ViewGroup;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.types.Suggestion;
import com.podcatcher.deluxe.view.SuggestionListItemView;
import com.podcatcher.deluxe.view.fragments.SuggestionFragment.AddSuggestionDialogListener;

import java.util.List;

/**
 * Adapter for the suggestion list.
 */
public class SuggestionListAdapter extends PodcatcherBaseListAdapter {

    /**
     * Owner for button call backs
     */
    protected final AddSuggestionDialogListener listener;
    /**
     * The list our data resides in
     */
    protected List<Suggestion> list;

    /**
     * The language filter disabled flag
     */
    private boolean languageWildcard;
    /**
     * The genre filter disabled flag
     */
    private boolean genreWildcard;
    /**
     * The media type filter disabled flag
     */
    private boolean typeWildcard;

    /**
     * Create new adapter.
     *
     * @param context     The current context.
     * @param suggestions List of podcasts (suggestions) to wrap.
     * @param listener    Call back for the add button to attach.
     */
    public SuggestionListAdapter(Context context, List<Suggestion> suggestions,
                                 AddSuggestionDialogListener listener) {
        super(context);

        this.list = suggestions;
        this.listener = listener;
    }

    /**
     * Update the adapter on the current filter settings.
     *
     * @param languageWildcard Give <code>true</code> if the all languages are
     *                         shown.
     * @param genreWildcard    Give <code>true</code> if the all genres are shown.
     * @param typeWildcard     Give <code>true</code> if the all media types are
     *                         shown.
     */
    public void setFilterConfiguration(boolean languageWildcard, boolean genreWildcard,
                                       boolean typeWildcard) {
        this.languageWildcard = languageWildcard;
        this.genreWildcard = genreWildcard;
        this.typeWildcard = typeWildcard;
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
        final SuggestionListItemView returnView = (SuggestionListItemView)
                findReturnView(convertView, parent, R.layout.suggestion_list_item);

        // Make the view represent podcast suggestion at given position
        final Suggestion suggestion = (Suggestion) getItem(position);
        returnView.show(suggestion, listener,
                PodcastManager.getInstance().contains(suggestion),
                languageWildcard, genreWildcard, typeWildcard);

        return returnView;
    }
}
