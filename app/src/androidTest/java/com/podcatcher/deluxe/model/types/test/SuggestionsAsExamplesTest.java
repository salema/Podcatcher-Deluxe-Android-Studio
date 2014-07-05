package com.podcatcher.deluxe.model.types.test;

import android.test.InstrumentationTestCase;
import android.util.Log;

import com.podcatcher.deluxe.model.test.Utils;
import com.podcatcher.deluxe.model.types.Podcast;

import java.util.Date;
import java.util.List;

@SuppressWarnings("javadoc")
public abstract class SuggestionsAsExamplesTest extends InstrumentationTestCase {

    protected static List<Podcast> examplePodcasts;
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
