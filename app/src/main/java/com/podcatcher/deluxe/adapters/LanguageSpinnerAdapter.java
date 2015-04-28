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

package com.podcatcher.deluxe.adapters;

import android.content.Context;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.types.Language;
import com.podcatcher.deluxe.view.fragments.AddSuggestionFragment;

/**
 * Adapter for the language spinner in the suggestion dialog.
 */
public class LanguageSpinnerAdapter extends SuggestionFilterSpinnerAdapter {

    /**
     * Create new adapter.
     *
     * @param context The current context.
     */
    public LanguageSpinnerAdapter(Context context) {
        super(context);

        // Put all languages into the value map where they are sorted by
        // language because we are using the corresponding resources as keys
        for (int index = 0; index < Language.values().length; index++) {
            final String key = resources.getStringArray(R.array.languages)[index];
            values.put(key, Language.values()[index]);
        }
    }

    @Override
    public Object getItem(int position) {
        if (position == 0)
            return AddSuggestionFragment.FILTER_WILDCARD;
        else
            return values.values().toArray()[position - 1];
    }

    @Override
    public int getCount() {
        return Language.values().length + 1;
    }
}
