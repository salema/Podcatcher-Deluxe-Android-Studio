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

package com.podcatcher.deluxe.model.types.test;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Suggestion;

import java.util.Date;
import java.util.List;

@SuppressWarnings("javadoc")
public abstract class SuggestionsAsExamplesTest extends InstrumentationTestCase {

    protected static List<Suggestion> examplePodcasts;
    protected static int sampleSize = 5;

    @Override
    protected void setUp() throws Exception {
        if (examplePodcasts == null) {
            Log.d(Utils.TEST_STATUS, "Set up test by loading example podcasts...");

            final Date start = new Date();
            examplePodcasts = Utils.getExamplePodcasts(getInstrumentation().getTargetContext(),
                    sampleSize);

            Log.d(Utils.TEST_STATUS, "Waited " + (new Date().getTime() - start.getTime())
                    + "ms for example podcasts...");

            int size = examplePodcasts.size();
            int index = 0;

            for (Podcast ep : examplePodcasts) {
                Log.d(Utils.TEST_STATUS, "---- Parsing podcast " +
                        ++index + "/" + size + ": " + ep.getName() + " ----");

                Utils.loadAndWait(ep);
            }
        }
    }
}
