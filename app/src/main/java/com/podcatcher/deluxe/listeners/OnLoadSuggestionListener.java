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

package com.podcatcher.deluxe.listeners;

import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;

import java.util.List;

/**
 * Interface definition for a callback to be invoked when podcast suggestions
 * are loaded.
 */
public interface OnLoadSuggestionListener {

    /**
     * Called on progress update.
     *
     * @param progress Progress of suggestions JSON file loaded or flag from
     *                 <code>Progress</code>. Note that this only works if the http
     *                 connection reports its content length correctly. Otherwise
     *                 (and this happens in the wild out there) percent might be
     *                 >100.
     */
    public void onSuggestionsLoadProgress(Progress progress);

    /**
     * Called on completion.
     *
     * @param suggestions Podcast suggestions loaded.
     */
    public void onSuggestionsLoaded(List<Suggestion> suggestions);

    /**
     * Called when loading the suggestions failed.
     */
    public void onSuggestionsLoadFailed();
}
