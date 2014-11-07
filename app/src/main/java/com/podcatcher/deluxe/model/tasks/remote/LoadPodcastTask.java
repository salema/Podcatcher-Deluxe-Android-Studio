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

package com.podcatcher.deluxe.model.tasks.remote;

import com.podcatcher.deluxe.BuildConfig;
import com.podcatcher.deluxe.listeners.OnLoadPodcastListener;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

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
     * The error code returned on failure
     */
    private PodcastLoadError errorCode = PodcastLoadError.UNKNOWN;

    /**
     * Podcast load error codes as returned by
     * {@link OnLoadPodcastListener#onPodcastLoadFailed(Podcast, PodcastLoadError)}
     * .
     */
    public static enum PodcastLoadError {
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
        NOT_PARSABLE
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

    @Override
    protected Void doInBackground(Podcast... podcasts) {
        this.podcast = podcasts[0];

        // Update the thread name to include the podcast working on
        if (BuildConfig.DEBUG)
            Thread.currentThread().setName(Thread.currentThread().getName() +
                    " [" + podcast.getName() + "]");

        try {
            // 1. Load the file from the Internet
            publishProgress(Progress.CONNECT);

            // Set auth
            this.authorization = podcast.getAuthorization();
            // ... and go get the file
            byte[] podcastRssFile = loadFile(new URL(podcast.getUrl()));

            if (!isCancelled()) {
                publishProgress(Progress.PARSE);

                // 2. Parse as podcast content
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(new ByteArrayInputStream(podcastRssFile), null);

                podcast.parse(parser);

                // 3. Clean out explicit episodes
                if (!isCancelled() && blockExplicit) {
                    final int episodeCount = podcast.getEpisodeCount();
                    final int cleanEpisodeCount = podcast.removeExplicitEpisodes();

                    if (cleanEpisodeCount == 0 && episodeCount > 0) {
                        errorCode = PodcastLoadError.EXPLICIT_BLOCKED;
                        cancel(true);
                    }
                }

                // 4. We need to wait here and make sure the episode metadata is
                // available before we return
                EpisodeManager.getInstance().blockUntilEpisodeMetadataIsLoaded();

                // 5. Update additional episode metadata where available, if not
                // parsed from the feed
                final EpisodeManager episodeManager = EpisodeManager.getInstance();
                if (!isCancelled())
                    for (Episode episode : podcast.getEpisodes()) {
                        if (episode.getDuration() <= 0)
                            episode.setDuration(episodeManager.findDuration(episode));

                        if (episode.getMediaSize() <= 0)
                            episode.setMediaSize(episodeManager.findMediaFileSize(episode));
                    }
            }
        } catch (XmlPullParserException xppe) {
            errorCode = PodcastLoadError.NOT_PARSABLE;
            cancel(true);
        } catch (IOException ioe) {
            // This will also catch mal-formed URLs
            errorCode = PodcastLoadError.NOT_REACHABLE;
            cancel(true);
        } catch (InterruptedException ie) {
            // Cannot wait of metadata, should not be an issue most of the time
            // pass
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
        else
            listener.onPodcastLoadFailed(podcast, errorCode);
    }
}
