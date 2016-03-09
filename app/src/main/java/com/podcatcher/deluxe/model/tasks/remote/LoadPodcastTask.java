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

package com.podcatcher.deluxe.model.tasks.remote;

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.tags.JSON;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Loads a podcast's RSS file from the server and parses its contents
 * asynchronously.
 * <p>
 * <b>Usage:</b> Implement the {@link OnLoadPodcastListener} interface and give
 * it to the task's constructor to be alerted on completion, progress, or
 * failure. The downloaded file will be used as the podcast's content via
 * {@link Podcast#parse(XmlPullParser)}, use the podcast object given (and
 * returned via callbacks) to access it.
 * </p>
 * <p>
 * <b>Authorization:</b> The task will send the credentials returned by
 * {@link Podcast#getAuthorization()} when requesting the file from the server.
 * If not present or wrong, the task fails and
 * {@link OnLoadPodcastListener#onPodcastLoadFailed(Podcast, PodcastLoadError)}
 * will be called with the code set to {@link PodcastLoadError#AUTH_REQUIRED}.
 * </p>
 */
public class LoadPodcastTask extends LoadRemoteFileTask<Podcast, Void> {

    /**
     * Call back to alert on completion and failure
     */
    private OnLoadPodcastListener listener;
    /**
     * Podcast currently loading by this task
     */
    private Podcast podcast;

    /**
     * Flag indicating whether we strip out explicit episodes
     */
    private boolean blockExplicit = false;

    /**
     * The URL the podcast currently loading moved to
     */
    private String shouldMoveToUrl;
    /**
     * Flag indicating whether we report on new-feed-url tags
     */
    private boolean reportPodcastMovedFromFeed = false;
    /**
     * Flag indicating whether we check podcatcher-deluxe.com for
     * better URLs and abort loading.
     */
    private boolean reportPodcastMovedIfEmpty = false;
    /**
     * Website URL to check for alternative feed URLs.
     */
    private static final String CHECK_FEED_URL = "http://podcatcher-deluxe.com/preferred-url.json/";

    /**
     * The error code returned on failure
     */
    private PodcastLoadError errorCode = PodcastLoadError.UNKNOWN;

    /**
     * Podcast load error codes as returned by
     * {@link OnLoadPodcastListener#onPodcastLoadFailed(Podcast, PodcastLoadError)}
     * .
     */
    public enum PodcastLoadError {
        /**
         * An error occurred, but the reason is unknown and/or does not fit any
         * of the other codes.
         */
        UNKNOWN,

        /**
         * Authorization is required.
         */
        AUTH_REQUIRED,

        /**
         * The authorization failed.
         */
        ACCESS_DENIED,

        /**
         * The restricted profile blocks explicit podcasts.
         */
        EXPLICIT_BLOCKED,

        /**
         * The remote server could not be reached.
         */
        NOT_REACHABLE,

        /**
         * The URL does not point at a valid feed file.
         */
        NOT_PARSABLE,

        /**
         * The feed file was too big to be loaded
         */
        TOO_LARGE
    }

    /**
     * Create new task.
     *
     * @param listener Callback to be alerted on progress and completion (not <code>null</code>).
     */
    public LoadPodcastTask(OnLoadPodcastListener listener) {
        this.listener = listener;
    }

    /**
     * @param block Whether the task should block explicit episodes from showing
     *              up in the episode list of the loaded podcast. If set and the
     *              episode list collapses to zero episodes, the task will fail
     *              with {@link PodcastLoadError#EXPLICIT_BLOCKED}.
     */
    public void setBlockExplicitEpisodes(boolean block) {
        this.blockExplicit = block;
    }

    /**
     * @param report Whether the task should return with
     *               {@link OnLoadPodcastListener#onPodcastMoved(Podcast, String)} instead of
     *               {@link OnLoadPodcastListener#onPodcastLoaded(Podcast)} if the feed parsed
     *               contains a new-feed-url tag. Default is <code>false</code> and ignores
     *               any such tags.
     */
    public void setReportPodcastMovedFromFeed(boolean report) {
        this.reportPodcastMovedFromFeed = report;
    }

    /**
     * @param report Whether the task should return with
     *               {@link OnLoadPodcastListener#onPodcastMoved(Podcast, String)} instead of
     *               {@link OnLoadPodcastListener#onPodcastLoaded(Podcast)} if the feed parsed
     *               contains no valid episodes and the tasks finds an alternative feed URL
     *               on podcatcher-deluxe.com. Default is <code>false</code>.
     */
    public void setReportPodcastMovedIfEmpty(boolean report) {
        this.reportPodcastMovedIfEmpty = report;
    }

    @Override
    protected Void doInBackground(Podcast... podcasts) {
        this.podcast = podcasts[0];

        // Update the thread name to include the podcast working on
        if (BuildConfig.DEBUG && podcast != null)
            Thread.currentThread().setName(Thread.currentThread().getName() +
                    " [" + podcast.getName() + "]");

        try {
            // 1. Load the file from the Internet
            publishProgress(Progress.CONNECT);

            // Set auth
            this.authorization = podcast.getAuthorization();
            // ... and go get the file
            byte[] podcastRssFile = loadFile(new URL(podcast.getUrl()));
            // Take a moment to remove any leading whitespaces that might make the parser fail
            if (podcastRssFile.length > 0 && Character.isWhitespace(podcastRssFile[0]))
                podcastRssFile = removeLeadingWhitespaces(podcastRssFile);

            podcast.setFileSize(podcastRssFile.length);

            if (!isCancelled()) {
                publishProgress(Progress.PARSE);

                // 2. Parse as podcast content
                final XmlPullParser parser = prepareParser(podcastRssFile);
                final String newFeedUrl = podcast.parse(parser);

                // Check for new feed URL we should move to
                if (newFeedUrl != null && reportPodcastMovedFromFeed && isGoodNewUrl(newFeedUrl)) {
                    shouldMoveToUrl = newFeedUrl;
                    cancel(true);
                }

                // Podcast is empty, if enabled try look-up on podcatcher-deluxe.com
                if (!isCancelled() && podcast.getEpisodeCount() == 0 &&
                        reportPodcastMovedIfEmpty && hasAlternativeUrl(podcast))
                    // Cancelling is enough since hasAlternativeUrl()
                    // set the shouldMoveToUrl member variable, see onCancelled()
                    cancel(true);

                // If expand flag is set on the podcast, process all pages
                if (podcast.isExpanded())
                    while (podcast.getNextPage() != null) {
                        final byte[] nextPageRssFile = removeLeadingWhitespaces(loadFile(new URL(podcast.getNextPage())));
                        podcast.parseEpisodes(prepareParser(nextPageRssFile));
                    }

                // 3. Clean out explicit episodes
                if (!isCancelled() && blockExplicit) {
                    final int episodeCount = podcast.getEpisodeCount();
                    final int cleanEpisodeCount = podcast.removeExplicitEpisodes();

                    if (cleanEpisodeCount == 0 && episodeCount > 0) {
                        errorCode = PodcastLoadError.EXPLICIT_BLOCKED;
                        cancel(true);
                    }
                }

                if (!isCancelled()) {
                    // 4. We need to wait here and make sure the episode metadata is
                    // available before we return
                    EpisodeManager.getInstance().blockUntilEpisodeMetadataIsLoaded();

                    // 5. Update additional episode metadata where available, if not
                    // parsed from the feed
                    final EpisodeManager episodeManager = EpisodeManager.getInstance();

                    for (Episode episode : podcast.getEpisodes()) {
                        if (episode.getDuration() <= 0)
                            episode.setDuration(episodeManager.findDuration(episode));

                        if (episode.getFileSize() <= 0)
                            episode.setFileSize(episodeManager.findMediaFileSize(episode));
                    }
                }
            }
        } catch (XmlPullParserException xppe) {
            // Parsing the podcast RSS file failed. Check the podcast repository
            // for a better URL to use, if this is the first page parsed and
            // no new URL has been set yet.
            if (shouldMoveToUrl == null && !podcast.isExpanded())
                // Set the shouldMoveToUrl member variable, see onCancelled()
                hasAlternativeUrl(podcast);

            errorCode = PodcastLoadError.NOT_PARSABLE;
            cancel(true);
        } catch (IOException ioe) {
            // This will also catch mal-formed URLs
            errorCode = PodcastLoadError.NOT_REACHABLE;
            cancel(true);
        } catch (InterruptedException ie) {
            // Cannot wait of metadata, should not be an issue most of the time
            // pass
        } catch (OutOfMemoryError me) {
            errorCode = PodcastLoadError.TOO_LARGE;
            cancel(true);
        } catch (Throwable th) {
            // We do not want the task to ever make the app crash
            cancel(true);
        } finally {
            publishProgress(Progress.DONE);
        }

        // Revert the thread name
        if (BuildConfig.DEBUG)
            Thread.currentThread().setName(Thread.currentThread().getName().split(" \\[")[0]);

        return null;
    }

    @Override
    protected void onProgressUpdate(Progress... progress) {
        listener.onPodcastLoadProgress(podcast, progress[0]);
    }

    @Override
    protected void onPostExecute(Void nothing) {
        listener.onPodcastLoaded(podcast);
    }

    @Override
    protected void onCancelled(Void nothing) {
        // Background task failed to complete
        if (needsAuthorization)
            listener.onPodcastLoadFailed(podcast, PodcastLoadError.AUTH_REQUIRED);
        else if (shouldMoveToUrl != null)
            listener.onPodcastMoved(podcast, shouldMoveToUrl);
        else
            listener.onPodcastLoadFailed(podcast, errorCode);
    }

    private byte[] removeLeadingWhitespaces(byte[] byteArray) {
        int firstNonWhiteSpacePosition = 0;

        // Trailing whitespaces make the XML pull parser fail, remove them
        while (firstNonWhiteSpacePosition < byteArray.length &&
                Character.isWhitespace(byteArray[firstNonWhiteSpacePosition]))
            firstNonWhiteSpacePosition++;

        return Arrays.copyOfRange(byteArray, firstNonWhiteSpacePosition, byteArray.length);
    }

    @NonNull
    private XmlPullParser prepareParser(byte[] podcastRssFile) throws XmlPullParserException {
        final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        final XmlPullParser parser = factory.newPullParser();
        parser.setInput(new ByteArrayInputStream(podcastRssFile), null);

        return parser;
    }

    private boolean isGoodNewUrl(String newFeedUrl) {
        try {
            final Podcast test = new Podcast(null, newFeedUrl);
            final byte[] podcastRssFile = removeLeadingWhitespaces(loadFile(new URL(newFeedUrl)));
            final XmlPullParser parser = prepareParser(podcastRssFile);

            return test.parse(parser) == null && test.getEpisodeCount() > 0;
        } catch (IOException | RuntimeException | XmlPullParserException e) {
            return false;
        }
    }

    private boolean hasAlternativeUrl(Podcast podcast) {
        try {
            // Go to podcatcher-deluxe.com, find and normalize alt. URL if any
            final byte[] response = loadFile(new URL(CHECK_FEED_URL + podcast.getUrl()));
            final String newUrl = new Podcast(null,
                    new JSONArray(new String(response, Charset.forName("UTF8")))
                            .getJSONObject(0).getString(JSON.FEED)
                            .split(JSON.VALUE_DELIMITER)[0]).getUrl();

            // Make sure it parses and is actually different from the URL we started with
            if (newUrl != null && !newUrl.equalsIgnoreCase(podcast.getUrl()))
                this.shouldMoveToUrl = new URL(newUrl).toString();
        } catch (IOException | JSONException | RuntimeException e) {
            // pass, no URL found -> return false
        }

        return shouldMoveToUrl != null;
    }
}
