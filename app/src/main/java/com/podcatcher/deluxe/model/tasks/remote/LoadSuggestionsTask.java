/**
 * Copyright 2012-2015 Kevin Hausmann
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

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.R;
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

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
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
    private static final String SOURCE = BuildConfig.SUGGESTIONS;
    /**
     * The local file name for the cached suggestions
     */
    private static final String LOCAL_SUGGESTIONS_FILE = "suggestions.v3.json";
    /**
     * Delimiter value used in JSON strings
     */
    private static final String JSON_VALUE_DELIMITER = ", ";
    /**
     * The text that marks isExplicit() == true
     */
    private static final String EXPLICIT_POSITIVE_STRING = "yes";
    /**
     * The date format used for the "date added" value
     */
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd MMM yyyy", Locale.US);
    /**
     * The date after which an added podcast suggestion is considered recent
     */
    private static final Date RECENT_LIMIT = new Date(new Date().getTime() - TimeUnit.DAYS.toMillis(30));
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
    private int maxAge = (int) TimeUnit.DAYS.toMinutes(1);

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

            final int age = getCachedFileAge();
            if (age >= 0 && age <= maxAge)
                // 1.1 This the simple case where we have the local version and
                // it is fresh enough. Use that one.
                suggestions = restoreSuggestionsFromFileCache();
            else {
                // 1.2 If that is not the case, we need to go over the air and
                // store a cached version ourselves (useCaches = false)
                suggestions = loadFile(new URL(SOURCE));
                storeSuggestionsToFileCache(suggestions);
            }
        } catch (IOException ioe1) {
            try {
                // 1.3 Use cached version even if it is stale
                suggestions = restoreSuggestionsFromFileCache();
            } catch (IOException ioe2) {
                try {
                    // 1.4 Finally fall back to the version from getResources()
                    suggestions = restoreSuggestionsFromResources();
                } catch (IOException ioe3) {
                    Log.d(TAG, "Loading podcast suggestions failed", ioe3);

                    cancel(true);
                    return null;
                }
            }
        }

        // 2. Parse the result
        publishProgress(Progress.PARSE);
        try {
            // 2.1 Get result as a document
            final JSONArray completeJson = new JSONArray(new String(suggestions, SUGGESTIONS_ENCODING));
            if (isCancelled())
                return null;

            // 2.2 Add all podcast suggestions
            addSuggestionsFromJsonArray(completeJson, result);
            if (isCancelled())
                return null;

            // 2.3 Sort the result
            Collections.sort(result);
        } catch (Exception ex) {
            Log.d(TAG, "Parse failed for podcast suggestions", ex);

            cancel(true);
            return null;
        }

        publishProgress(Progress.DONE);
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
    private void addSuggestionsFromJsonArray(JSONArray array, List<Suggestion> list) {
        // So, this get a little bit involved. In order to make a good judgement which
        // podcast suggestions should be featured, we need a breakdown by language as
        // well as votes. This is done in the hitlist:
        final Map<Language, SortedMap<Integer, List<Suggestion>>> hitlist = new HashMap<>();

        // Fill hitlist of suggestions
        for (int index = 0; index < array.length(); index++)
            try {
                final JSONObject object = array.getJSONObject(index);
                final int votes = object.has(JSON.VOTES) ? object.getInt(JSON.VOTES) : 0;
                final Suggestion suggestion = createSuggestion(object);

                // Might be null if parsing failed -> do not add to result list
                if (suggestion != null) {
                    // We use the podcasts first language for featuring
                    final Language language = (Language) suggestion.getLanguages().toArray()[0];
                    SortedMap<Integer, List<Suggestion>> languageHitlist;

                    // Make sure hitlist for the suggestion's language is present
                    if (hitlist.containsKey(language))
                        languageHitlist = hitlist.get(language);
                    else {
                        languageHitlist = new TreeMap<>(Collections.reverseOrder());
                        hitlist.put(language, languageHitlist);
                    }
                    // Put suggestion into the right bucket
                    if (languageHitlist.containsKey(votes))
                        languageHitlist.get(votes).add(suggestion);
                    else {
                        final List<Suggestion> entry = new ArrayList<>();
                        entry.add(suggestion);

                        languageHitlist.put(votes, entry);
                    }
                }
            } catch (JSONException | ArrayIndexOutOfBoundsException e) {
                Log.d(TAG, "Cannot create JSON object for index: " + index, e);
            }

        // Make sure the best suggestions are featured and all are added to the plain list
        for (SortedMap<Integer, List<Suggestion>> languageHitlist : hitlist.values()) {
            int featuredCount = 0;
            // Calculate total suggestions count for this language as needed below
            int languageSuggestionCount = 0;
            for (List<Suggestion> suggestionList : languageHitlist.values())
                languageSuggestionCount += suggestionList.size();

            for (Map.Entry<Integer, List<Suggestion>> entry : languageHitlist.entrySet()) {
                // Mark the top 15 percent as featured
                for (Suggestion suggestion : entry.getValue()) {
                    suggestion.setFeatured(featuredCount < languageSuggestionCount / 7);
                    addKeywordsForStatus(suggestion);
                }

                list.addAll(entry.getValue());
                featuredCount += entry.getValue().size();
            }
        }
    }

    /**
     * Create a podcast suggestion for the given JSON object and set its properties.
     *
     * @param json The JSON object to work on.
     * @return The podcast suggestion or <code>null</code> if any problem occurs.
     */
    private Suggestion createSuggestion(JSONObject json) {
        Suggestion suggestion = null;

        try {
            final String[] feeds = json.getString(JSON.FEED).split(JSON_VALUE_DELIMITER);
            suggestion = new Suggestion(json.getString(JSON.TITLE).trim(), feeds[0]);
            suggestion.setDescription(json.getString(JSON.DESCRIPTION).trim());
            suggestion.setLanguages(Language.valueOfJson(json.getString(JSON.LANGUAGE), JSON_VALUE_DELIMITER));
            suggestion.setMediaTypes(MediaType.valueOfJson(json.getString(JSON.TYPE), JSON_VALUE_DELIMITER));
            suggestion.setGenres(Genre.valueOfJson(json.getString(JSON.CATEGORY), JSON_VALUE_DELIMITER));
            suggestion.setExplicit(EXPLICIT_POSITIVE_STRING.equals(json.getString(JSON.EXPLICIT).toLowerCase(Locale.US).trim()));
            suggestion.setNew(json.has(JSON.DATE_ADDED) && isRecentDate(json.getString(JSON.DATE_ADDED)));
            suggestion.setLogoUrl(json.getString(JSON.LOGO));
            suggestion.setKeywords(json.has(JSON.KEYWORDS) ? json.getString(JSON.KEYWORDS) : null);

            final String[] labels = json.getString(JSON.FEED_LABEL).split(JSON_VALUE_DELIMITER);
            for (int index = 0; index < labels.length; index++)
                suggestion.addFeed(labels[index], feeds[index]);
        } catch (JSONException e) {
            Log.d(TAG, "JSON parsing failed for: " + suggestion, e);
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.d(TAG, "Feed and label counts do not match for: " + suggestion, e);
            return null;
        } catch (RuntimeException e) {
            // We do not want one suggestion to make the whole process fail...
            Log.d(TAG, "Something went wrong parsing suggestion: " + suggestion, e);
            return null;
        }

        return suggestion;
    }

    private boolean isRecentDate(String dateAdded) {
        try {
            return DATE_FORMATTER.parse(dateAdded).after(RECENT_LIMIT);
        } catch (ParseException e) {
            return false;
        }
    }

    private void addKeywordsForStatus(Suggestion suggestion) {
        final String delimiter = ", ";
        String extraKeywords = suggestion.isFeatured() ?
                context.getString(R.string.suggestion_featured) + delimiter : "";
        extraKeywords = extraKeywords + (suggestion.isNew() ?
                context.getString(R.string.suggestion_new) + delimiter : "");
        extraKeywords = extraKeywords + (suggestion.isExplicit() ?
                context.getString(R.string.suggestion_explicit) + delimiter : "");

        suggestion.setKeywords(suggestion.getKeywords() == null ?
                extraKeywords : suggestion.getKeywords() + delimiter + extraKeywords);
    }

    private File getSuggestionsCacheFile() {
        // Create the complete path leading to where we expect the cached file
        return new File(context.getCacheDir(), LOCAL_SUGGESTIONS_FILE);
    }

    private boolean isCachedLocally() {
        return getSuggestionsCacheFile().exists();
    }

    private int getCachedFileAge() {
        if (isCachedLocally())
            return (int) ((new Date().getTime() - getSuggestionsCacheFile().lastModified())
                    / TimeUnit.MINUTES.toMillis(1)); // Calculate to minutes
        else
            return -1;
    }

    private byte[] restoreSuggestionsFromFileCache() throws IOException {
        final FileInputStream input = new FileInputStream(getSuggestionsCacheFile());

        try {
            return readFileToByteArray(input);
        } finally {
            try {
                input.close();
            } catch (Throwable e) {
                // Nothing more we could do here
            }
        }
    }

    private byte[] restoreSuggestionsFromResources() throws IOException {
        final InputStream input = context.getResources().openRawResource(R.raw.podcast_suggestions_v3);

        try {
            return readFileToByteArray(input);
        } finally {
            try {
                input.close();
            } catch (Throwable e) {
                // Nothing more we could do here
            }
        }
    }

    private byte[] readFileToByteArray(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = stream.read(data, 0, data.length)) != -1)
            buffer.write(data, 0, nRead);

        buffer.flush();

        return buffer.toByteArray();
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
        } catch (IOException ioe) {
            Log.d(TAG, "Storing podcast suggestions to cache failed", ioe);
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
