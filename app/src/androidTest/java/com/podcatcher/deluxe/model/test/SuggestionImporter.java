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

package com.podcatcher.deluxe.model.test;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.EpisodeDownloadManager;
import com.podcatcher.deluxe.model.tags.RSS;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Genre;
import com.podcatcher.deluxe.model.types.Suggestion;

import android.content.res.Resources;
import android.os.Environment;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Creates JSON files (placed on the device test runs on)
 * for importing podcast suggestions to podcatcher-deluxe.com
 */
@SuppressWarnings("javadoc")
public class SuggestionImporter extends InstrumentationTestCase {

    private static final String TAG = "IMPORTER";

    /**
     * String to put in JSON for missing values, use null to disable
     */
    private static String NO_VALUE = null;

    /**
     * Max age of latest podcast episode. Podcasts with older episodes
     * only will be skipped. Give negative value to disable.
     */
    private static final int REJECT_RECENT_DAYS = -1; // 90

    private static final File RESULT_FILE = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS),
            "suggestions.json");

    public final void testCreateImportFile() {
        // Put feed URLs here:
        final String[] urls = {"http://feeds.feedburner.com/SlatesWorking", "http://feeds.feedburner.com/hearpitch",
                "http://readinglives.libsyn.com/rss", "http://rss.cnn.com/services/podcasting/fareedzakaria/rss.xml",
                "http://rss.earwolf.com/topics", "http://throwingshade.libsyn.com/rss", "http://feeds.5by5.tv/dlc",
                "http://geeknation.com/podcast-feed/the-movie-crypt-with-green-lynch/", "http://meninblazers.buzzsprout.com/5628.rss"};
        final List<Suggestion> existingSuggestions = Utils.getExamplePodcasts(getInstrumentation().getTargetContext());
        Log.i(TAG, "List of existing suggestions loaded: " + existingSuggestions.size() + " podcasts");
        final List<JsonDummy> dummies = new ArrayList<>();

        Log.i(TAG, "Setup complete, starting process with " + urls.length + " podcast suggestion(s).");
        if (!"en".equals(Locale.getDefault().getLanguage()))
            Log.w(TAG, "Device should be set to English for taxonomy to match!");

        for (String url : urls) {
            // Make sure we do not import a duplicate
            final SuggestionImport si = new SuggestionImport(url);
            if (existingSuggestions.contains(si)) {
                Log.w(TAG, "Skipping existing podcast suggestion: " + url);
                continue;
            } else Utils.loadAndWait(si);

            if (si.getEpisodeCount() > 0) {
                final List<Episode> episodes = si.getEpisodes();
                Collections.sort(episodes);

                long ageInDays = 0;
                // Negative age might happen, just invert those (too far in the future is also strange...)
                if (episodes.get(0).getPubDate() != null) ageInDays = Math.abs(
                        (new Date().getTime() - episodes.get(0).getPubDate().getTime()) / TimeUnit.DAYS.toMillis(1));

                if (REJECT_RECENT_DAYS < 1 || ageInDays < REJECT_RECENT_DAYS) {
                    dummies.add(new JsonDummy(si));
                    Log.i(TAG, "Podcast " + si.getName() + " added.");
                } else
                    Log.w(TAG, "Podcast " + si.getName() + " latest episode is " +
                            ageInDays + " days old, skipped.");
            } else
                Log.w(TAG, "Podcast " + si.getName() + " has no episodes, skipped.");
        }

        writeResultToFile(dummies);
        Log.i(TAG, "Finished. Resulting JSON written to " + RESULT_FILE.getAbsolutePath() +
                ", containing " + dummies.size() + " entries.");
    }

    private void writeResultToFile(List<JsonDummy> dummies) {
        final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(RESULT_FILE);
            stream.write(gson.toJson(dummies).getBytes());
        } catch (IOException e) {
            Log.w(TAG, "Failed to write JSON file.", e);
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException e) {
                // pass
            }
        }
    }

    private static String cleanUp(String text) {
        if (text != null)
            return text.trim()
                    .replaceAll("\\p{C}", " ") // Control characters
                    .replaceAll(" +", " "); // Multiple whitespaces
        else return null;
    }

    private class SuggestionImport extends Suggestion {

        String subtitle;
        String summary;
        String keywords;
        String link;
        String language;
        Set<String> categories = new HashSet<>();

        public SuggestionImport(String podcastFeed) {
            super(null, podcastFeed);
        }

        @Override
        protected void parse(XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
            Log.v(TAG, "TAGNAME: " + tagName);
            final Resources res = getInstrumentation().getTargetContext().getResources();

            if (RSS.LINK.equals(tagName) && link == null) // We only want the first one from the header
                link = normalizeUrl(parser.nextText().trim());
            else if (RSS.SUBTITLE.equals(tagName) && subtitle == null)
                subtitle = cleanUp(parser.nextText());
            else if (RSS.DESCRIPTION.equals(tagName) && description == null)
                description = cleanUp(parser.nextText());
            else if (RSS.SUMMARY.equals(tagName) && summary == null)
                summary = cleanUp(parser.nextText());
            else if (RSS.KEYWORDS.equals(tagName) && keywords == null)
                keywords = cleanUp(parser.nextText());
            else if (RSS.LANGUAGE.equals(tagName) && language == null)
                // We need to two letter code here, because that's what the importer understands
                language = parser.nextText().trim().substring(0, 2).toLowerCase(Locale.US);
            else if (RSS.CATEGORY.equals(tagName)) {
                String category = parser.getAttributeValue("", "text");
                if (category == null || category.length() < 2)
                    category = parser.nextText();

                try {
                    category = cleanUp(category).replace("&amp;", "&").split("/")[0];

                    final Genre genre = Genre.forLabel(category);
                    categories.add(res.getStringArray(R.array.genres)[genre.ordinal()]);
                } catch (IllegalArgumentException iae) {
                    // pass
                    Log.w(TAG, "Unknown category: " + category);
                } catch (Throwable th) {
                    // pass
                    Log.w(TAG, "Bad category: " + category);
                }
            }
        }


    }

    private class JsonDummy {

        // Field will be access via GSON reflection
        private String title;
        private Object[] category;
        private String language;
        private String type;
        private String path;

        public JsonDummy(SuggestionImport si) {
            this.title = cleanUp(si.getName());
            this.subtitle = si.subtitle == null || si.subtitle.length() == 0 ? NO_VALUE : si.subtitle;

            this.keywords = si.keywords == null || si.keywords.length() == 0 ? NO_VALUE : si.keywords;
            if (si.keywords != null && si.keywords.length() > 160)
                Log.w(TAG, "Podcast " + title + " keywords are too long!");

            this.feed = si.getUrl().replace("http://", "feed://");

            this.logo = si.getLogoUrl() == null ? NO_VALUE : si.getLogoUrl();
            if (si.getLogoUrl() == null)
                Log.w(TAG, "Podcast " + title + " has no logo!");

            this.site = si.link != null && si.link.length() > 5 ?
                    si.link.endsWith("/") ? si.link.substring(0, si.link.length() - 1) : si.link : NO_VALUE;

            if (si.getDescription() == null)
                this.description = si.summary;
            else if (si.summary == null)
                this.description = si.getDescription();
            else
                this.description = si.getDescription().length() > si.summary.length() ?
                        si.getDescription() : si.summary;

            this.language = si.language;
            if ("pt".equalsIgnoreCase(language))
                this.language = "pt-br"; // Drupal needs this

            this.category = si.categories.toArray();
            if (category.length == 0)
                Log.w(TAG, "Podcast " + title + " has no category");
            try {
                final String typeString = si.getEpisodes().get(0).getMediaType().split("/")[0];
                this.type = typeString.substring(0, 1).toUpperCase(Locale.ENGLISH) +
                        typeString.substring(1, 5).toLowerCase(Locale.ENGLISH);

                if (!("Audio".equals(type) || "Video".equals(type)))
                    Log.w(TAG, "Podcast " + title + " has invalid media type: " + type);
            } catch (Throwable th) {
                Log.w(TAG, "Cannot get media type from episodes for " + title);
            }

            this.explicit = si.isExplicit() ? "Yes" : "No";

            this.path = cleanUpAsPath(title);
            if (path.length() < 5)
                Log.w(TAG, "Podcast " + title + " has short path:" + path);
        }

        private String cleanUpAsPath(String text) {
            text = Normalizer.normalize(text, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

            return EpisodeDownloadManager.sanitizeAsFilename(text)
                    .replace("-", " ").replaceAll(" +", " ").replace(" ", "-").replace("%", "");
        }
    }
}
