/**
 * Copyright 2012-2016 Kevin Hausmann
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
import android.content.res.Resources;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Abstract super class for this app's adapters. The keeps some handles useful
 * for all adapters.
 */
public abstract class PodcatcherBaseAdapter extends BaseAdapter {

    /**
     * The resources handle
     */
    protected final Context context;
    /**
     * The resources handle
     */
    protected final Resources resources;
    /**
     * The inflater we use
     */
    private final LayoutInflater inflater;

    /**
     * Create new adapter.
     *
     * @param context The current context.
     */
    public PodcatcherBaseAdapter(Context context) {
        this.context = context;
        this.resources = context.getResources();
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public boolean hasStableIds() {
        // All our adapters have stable ids by default so the framework can
        // optimize our list views.
        return true;
    }

    /**
     * Check whether a view can be recycled and inflate a new one if not.
     *
     * @param convertView View to check.
     * @param parent      View group to attach to.
     * @param inflateId   Id of view to inflate if recycling is not possible.
     * @return A view to use (not <code>null</code>).
     */
    protected View findReturnView(View convertView, ViewGroup parent, @LayoutRes int inflateId) {
        // Can we recycle the convert view?
        // No:
        if (convertView == null)
            return inflater.inflate(inflateId, parent, false);
            // Yes:
        else
            return convertView;
    }
}
