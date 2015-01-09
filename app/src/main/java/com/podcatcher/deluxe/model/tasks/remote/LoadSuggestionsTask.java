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

package com.podcatcher.deluxe.model.tasks.remote;

import android.content.Context;
import android.util.Log;

import com.podcatcher.deluxe.listeners.OnLoadSuggestionListener;
import com.podcatcher.deluxe.model.tags.JSON;
import com.podcatcher.deluxe.model.types.Genre;
import com.podcatcher.deluxe.model.types.Language;
import com.podcatcher.deluxe.model.types.MediaType;
import com.podcatcher.deluxe.model.types.Progress;
import com.podcatcher.deluxe.model.types.Suggestion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A task that loads and reads suggested podcasts.
 */
public class LoadSuggestionsTask extends LoadRemoteFileTask<Void, List<Suggestion>> {

    /**
     * The file encoding
     */
    private static final String SUGGESTIONS_ENCODING = "utf8";
    /**
     * The online resource to find suggestions
     */
    private static final String SOURCE = "http://www.podcatcher-deluxe.com/podcast_suggestions.json";
    /**
     * The local file name for the cached suggestions
     */
    private static final String LOCAL_SUGGESTIONS_FILE = "suggestions.json";
    /**
     * The text that marks isExplicit() == true
     */
    private static final String EXPLICIT_POSITIVE_STRING = "yes";
    /**
     * Our log tag
     */
    private static final String TAG = "LoadSuggestionsTask";
    /**
     * The task's context
     */
    private final Context context;
    /**
     * Call back
     */
    private OnLoadSuggestionListener listener;
    /**
     * Flag to indicate the max age that would trigger re-load.
     */
    private int maxAge = (int) TimeUnit.DAYS.toSeconds(3);

    /**
     * Create new task.
     *
     * @param context  The context the task is carried out in.
     * @param listener Callback to be alerted on progress and completion.
     */
    public LoadSuggestionsTask(Context context, OnLoadSuggestionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected List<Suggestion> doInBackground(Void... params) {
        List<Suggestion> result = new ArrayList<>();
        byte[] suggestions;

        // 1. Load the file from the cache or the Internet
        try {
            publishProgress(Progress.CONNECT);
            // 1.1 This the simple case where we have the local version and
            // it is fresh enough. Use that one.
            if (isCachedLocally() && getCachedLogoAge() <= maxAge)
                suggestions = restoreSuggestionsFromFileCache();
                // 1.2 If that is not the case, we need to go over the air.
            else {
                // We store a cached version ourselves
                // useCaches = false;
                suggestions = loadFile(new URL(SOURCE));

                storeSuggestionsToFileCache(suggestions);
            }
        } catch (Throwable throwable) {
            // Use cached version even if it is stale
            if (isCachedLocally())
                try {
                    suggestions = restoreSuggestionsFromFileCache();
                } catch (IOException ioe) {
                    cancel(true);
                    return null; // Nothing more we could do here
                }
            else {
                Log.d(TAG, "Load failed for podcast suggestions file", throwable);

                cancel(true);
                return null;
            }
        }

        // 2. Parse the result
        try {
            // 2.1 Get result as a document
            publishProgress(Progress.PARSE);
            JSONObject completeJson = new JSONObject(new String(suggestions, SUGGESTIONS_ENCODING));
            if (isCancelled())
                return null;

            // 2.2 Add all featured podcasts
            addSuggestionsFromJsonArray(completeJson.getJSONArray(JSON.FEATURED), result, true);
            if (isCancelled())
                return null;

            // 2.3 Add all suggestions
            addSuggestionsFromJsonArray(completeJson.getJSONArray(JSON.SUGGESTION), result, false);
            if (isCancelled())
                return null;

            // 2.4 Sort the result
            Collections.sort(result);
            publishProgress(Progress.DONE);
        } catch (Exception ex) {
            Log.d(TAG, "Parse failed for podcast suggestions", ex);

            cancel(true);
            return null;
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Progress... progress) {
        if (listener != null)
            listener.onSuggestionsLoadProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(List<Suggestion> suggestions) {
        // Suggestions loaded successfully
        if (listener != null)
            listener.onSuggestionsLoaded(suggestions);
    }

    @Override
    protected void onCancelled(List<Suggestion> suggestions) {
        // Suggestions failed to load
        if (listener != null)
            listener.onSuggestionsLoadFailed();
    }

    /**
     * Add all podcast suggestions in given array to the list.
     *
     * @param array JSON array to scan.
     * @param list  List to add suggestions to.
     */
    private void addSuggestionsFromJsonArray(JSONArray array, List<Suggestion> list,
                                             boolean featured) {
        for (int index = 0; index < array.length(); index++) {
            JSONObject object;

            try {
                object = array.getJSONObject(index);
            } catch (JSONException e) {
                continue; // If an index fails, try the next one...
            }

            Suggestion suggestion = createSuggestion(object);
            if (suggestion != null) {
                suggestion.setFeatured(featured);
                list.add(suggestion);
            }
        }
    }

    /**
     * Create a podcast suggestion for the given JSON object and set its
     * properties.
     *
     * @param json The JSON object to work on.
     * @return The podcast suggestion or <code>null</code> if any problem
     * occurs.
     */
    private Suggestion createSuggestion(JSONObject json) {
        Suggestion suggestion = null;

        try {
            suggestion = new Suggestion(json.getString(JSON.TITLE), json.getString(JSON.URL));
            suggestion.setDescription(json.getString(JSON.DESCRIPTION).trim());
            suggestion.setLanguage(Language.valueOf(json.getString(JSON.LANGUAGE)
                    .toUpperCase(Locale.US).trim()));
            suggestion.setMediaType(MediaType.valueOf(json.getString(JSON.TYPE)
                    .toUpperCase(Locale.US).trim()));
            suggestion.setGenre(Genre.forLabel(json.getString(JSON.CATEGORY)));
            suggestion.setExplicit(EXPLICIT_POSITIVE_STRING.equals(json.getString(JSON.EXPLICIT)
                    .toLowerCase(Locale.US)));
        } catch (JSONException e) {
            Log.d(TAG, "JSON parsing failed for: " + suggestion, e);

            return null;
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Enum value missing for: " + suggestion, e);

            return null;
        }

        return suggestion;
    }

    private File getSuggestionsCacheFile() {
        // Create the complete path leading to where we expect the cached file
        return new File(context.getCacheDir(), LOCAL_SUGGESTIONS_FILE);
    }

    private boolean isCachedLocally() {
        return getSuggestionsCacheFile().exists();
    }

    private int getCachedLogoAge() {
        if (isCachedLocally())
            return (int) ((new Date().getTime() - getSuggestionsCacheFile().lastModified())
                    / TimeUnit.MINUTES.toMillis(1)); // Calculate to minutes
        else
            return -1;
    }

    private byte[] restoreSuggestionsFromFileCache() throws IOException {
        final File cachedFile = getSuggestionsCacheFile();
        final byte[] result = new byte[(int) cachedFile.length()];

        FileInputStream input = null;
        try {
            input = new FileInputStream(cachedFile);
            // noinspection ResultOfMethodCallIgnored
            input.read(result);
        } finally {
            try {
                if (input != null)
                    input.close();
            } catch (Throwable e) {
                // Nothing more we could do here
            }
        }

        return result;
    }

    private void storeSuggestionsToFileCache(byte[] suggestions) {
        FileOutputStream out = null;

        // If this fails, we have no cached version, but that's okay
        try {
            // noinspection ResultOfMethodCallIgnored
            context.getCacheDir().mkdirs();

            out = new FileOutputStream(getSuggestionsCacheFile());
            out.write(suggestions);
            out.flush();
        } catch (Throwable th) {
            // pass
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Throwable e) {
                // Nothing more we could do here
            }
        }
    }
}
