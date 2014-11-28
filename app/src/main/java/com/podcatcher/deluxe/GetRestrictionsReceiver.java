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

package com.podcatcher.deluxe;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import java.util.ArrayList;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Creates the app's configurable restrictions for restricted users.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GetRestrictionsReceiver extends BroadcastReceiver {

    /**
     * Key to identify the hide explicit restriction
     */
    public static final String BLOCK_EXPLICIT_RESTRICTION_KEY = "block_explicit";

    @Override
    public void onReceive(final Context context, Intent intent) {
        final PendingResult result = goAsync();

        new Thread() {
            public void run() {
                Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
                final Bundle extras = new Bundle();

                // Create the restriction
                final RestrictionEntry hideExplicit = new RestrictionEntry(
                        BLOCK_EXPLICIT_RESTRICTION_KEY, true);
                hideExplicit.setTitle(context.getString(R.string.podcast_block_explicit));

                // Put everything together and send it back
                final ArrayList<RestrictionEntry> list = new ArrayList<>();
                list.add(hideExplicit);

                extras.putParcelableArrayList(Intent.EXTRA_RESTRICTIONS_LIST, list);
                result.setResult(Activity.RESULT_OK, null, extras);
                result.finish();
            }
        }.start();
    }
}
