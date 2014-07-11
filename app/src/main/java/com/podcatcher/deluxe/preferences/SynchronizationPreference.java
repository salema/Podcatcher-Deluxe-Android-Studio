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

package com.podcatcher.deluxe.preferences;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.model.sync.SyncController.SyncMode;

/**
 * The synchronization preference.
 */
public class SynchronizationPreference extends Preference {

    /**
     * For the summary: up arrow in unicode
     */
    private static final String UP_ARROW = "\u2191";
    /**
     * For the summary: up and down arrow in unicode
     */
    private static final String UP_DOWN_ARROW = "\u2195";
    /**
     * Our {@link SyncManager} handle.
     */
    private final SyncManager syncManager;

    /**
     * Create a new preference.
     *
     * @param context Our context.
     * @param attrs   The attributes.
     */
    public SynchronizationPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.syncManager = SyncManager.getInstance();
    }

    @Override
    public CharSequence getSummary() {
        if (syncManager.getActiveControllerCount() == 0)
            return getContext().getResources().getString(R.string.pref_sync_none);
        else {
            // Build a nice status string as the sync summary
            final StringBuilder builder = new StringBuilder(UP_ARROW + " ");

            // Add all controller with send only
            for (ControllerImpl impl : ControllerImpl.values())
                if (SyncMode.SEND_ONLY.equals(syncManager.getSyncMode(impl)))
                    builder.append(impl.getLabel()).append(", ");

            // Delete last ", "
            builder.delete(builder.length() - 2, builder.length());
            // Put intent before up and down arrow if not empty
            if (builder.length() > 0)
                builder.append("  ");

            builder.append(UP_DOWN_ARROW + " ");

            // Add all send + receive controllers
            for (ControllerImpl impl : ControllerImpl.values())
                if (SyncMode.SEND_RECEIVE.equals(syncManager.getSyncMode(impl)))
                    builder.append(impl.getLabel()).append(", ");

            builder.delete(builder.length() - 2, builder.length());

            return builder.toString();
        }
    }
}
