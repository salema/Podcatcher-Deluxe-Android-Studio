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

package com.podcatcher.deluxe.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.podcatcher.deluxe.R;

import java.util.TreeMap;

/**
 * Abstract base for suggestion filter spinner adapters. This default
 * implementation will use the tree map {@link #values} to label the returned
 * list and spinner views.
 */
public abstract class SuggestionFilterSpinnerAdapter extends PodcatcherBaseAdapter {

    /**
     * The sorted map to store our values in, this is needed to account for the
     * sorting in different languages.
     */
    protected final TreeMap<String, Object> values = new TreeMap<>();

    /**
     * Create the adapter.
     *
     * @param context Context we live in.
     */
    public SuggestionFilterSpinnerAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getLabel(position, convertView, parent, false);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getLabel(position, convertView, parent, true);
    }

    private View getLabel(int position, View convertView, ViewGroup parent, boolean dropDown) {
        TextView result;

        // Get the correct return view
        if (dropDown)
            result = (TextView) findReturnView(convertView, parent,
                    android.R.layout.simple_spinner_dropdown_item);
        else
            result = (TextView) findReturnView(convertView, parent,
                    android.R.layout.simple_spinner_item);

        // Apply the appropriate text label
        if (position == 0)
            result.setText(resources.getString(R.string.wildcard));
        else
            result.setText((String) values.keySet().toArray()[position - 1]);

        return result;
    }

    @Override
    public long getItemId(int position) {
        // Since there are only enums behind this, it is actually okay to simply
        // return the position...
        return position;
    }
}
