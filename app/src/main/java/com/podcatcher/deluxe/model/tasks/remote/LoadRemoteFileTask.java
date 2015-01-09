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

import android.os.AsyncTask;

import com.podcatcher.deluxe.model.types.Progress;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.podcatcher.deluxe.Podcatcher.AUTHORIZATION_KEY;
import static com.podcatcher.deluxe.Podcatcher.USER_AGENT_KEY;
import static com.podcatcher.deluxe.Podcatcher.USER_AGENT_VALUE;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Abstract super class for file download tasks.
 *
 * @param <Params> Params as defined by {@link AsyncTask}
 * @param <Result> Result as defined by {@link AsyncTask}
 * @see AsyncTask
 */
public abstract class LoadRemoteFileTask<Params, Result> extends
        AsyncTask<Params, Progress, Result> {

    /**
     * The connection timeout
     */
    protected static final int CONNECT_TIMEOUT = 8000;
    /**
     * The read timeout
     */
    protected static final int READ_TIMEOUT = 60000;

    /**
     * The use caches flag set to the http connection before it is opened.
     */
    protected boolean useCaches;

    /**
     * The max stale cache control to set
     */
    protected int maxStale = -1;
    /**
     * A file size limit in bytes for the download
     */
    protected int loadLimit = -1;

    /**
     * The authorization to send
     */
    protected String authorization;
    /**
     * The flag to indicate that authorization is/was required
     */
    protected boolean needsAuthorization = false;

    /**
     * Set a "max-stale" cache control directive when downloading the file. The
     * default is a negative number, turning off the directive. If not negative,
     * the cache control directive will be set when requesting the file.
     *
     * @param seconds The max stale time to set in seconds.
     */
    public void setMaxStale(int seconds) {
        this.maxStale = seconds;
    }

    /**
     * Set a load limit for the actual download of the file. The default is a
     * negative number, turning off the limit evaluation. If positive and
     * reached, {@link #loadFile(URL)} below will return <code>null</code>
     * immediately.
     *
     * @param bytes The limit to set in bytes.
     */
    public void setLoadLimit(int bytes) {
        this.loadLimit = bytes;
    }

    /**
     * Download the file and return it as a byte array. Will feed
     * {@link #publishProgress(Object...)}.
     *
     * @param remote URL connection to load from.
     * @return The file content.
     * @throws IOException If something goes wrong.
     */
    protected byte[] loadFile(URL remote) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) remote.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        // Set whether we use the http cache
        connection.setUseCaches(useCaches);
        // We set a custom user agent here because some servers (e.g. ZDF.de)
        // redirect connections from mobile devices to servers where the content
        // we are looking for might not be available.
        connection.setRequestProperty(USER_AGENT_KEY, USER_AGENT_VALUE);
        // Set cache control directive
        if (maxStale >= 0)
            connection.addRequestProperty("Cache-Control", "max-stale=" + maxStale);
        // Allow for password protected feeds
        if (authorization != null)
            connection.setRequestProperty(AUTHORIZATION_KEY, authorization);

        BufferedInputStream bufferedRemoteStream = null;
        ByteArrayOutputStream result = null;

        try {
            // 1. Open stream and check whether we know its length
            bufferedRemoteStream = new BufferedInputStream(connection.getInputStream());
            final int contentLength = connection.getContentLength();
            // Check whether we should abort load since we have a load limit set
            // and the content length is higher.
            if (loadLimit >= 0 && contentLength >= 0 && contentLength > loadLimit)
                throw new IOException("Load limit exceeded (content length reported by remote is "
                        + contentLength + " bytes, limit was " + loadLimit + " bytes)!");
            // Check whether we could calculate the percentage of completion,
            // this only works if a content length is given and the content is
            // not gzipped
            final boolean isZippedResponse = connection.getContentEncoding() != null
                    && connection.getContentEncoding().equals("gzip");
            final boolean sendLoadProgress = contentLength > 0 && !isZippedResponse;

            // showResponseHeaderDetails(connection);

            // 2. Create the byte buffer to write to
            result = new ByteArrayOutputStream();
            publishProgress(Progress.LOAD);

            byte[] buffer = new byte[1024];
            int bytesRead;
            int totalBytes = 0;

            // 3. Read stream and report progress (if possible)
            while ((bytesRead = bufferedRemoteStream.read(buffer)) > 0) {
                if (isCancelled())
                    return null;

                totalBytes += bytesRead;
                if (loadLimit >= 0 && totalBytes > loadLimit)
                    throw new IOException("Load limit exceeded (read " + totalBytes +
                            " bytes, limit was " + loadLimit + " bytes)!");

                result.write(buffer, 0, bytesRead);

                if (sendLoadProgress)
                    publishProgress(new Progress(totalBytes, contentLength));
            }

            // 4. Return result as a byte array
            return result.toByteArray();
        } catch (IOException ioe) {
            // Make sure sub-classes can react if auth is needed
            if (connection.getResponseCode() == HTTP_UNAUTHORIZED)
                needsAuthorization = true;

            throw ioe;
        } finally {
            // Close the streams
            // To remote
            if (bufferedRemoteStream != null)
                try {
                    bufferedRemoteStream.close();
                } catch (Exception e) {
                    // Nothing we can do here
                }

            // To the local byte array
            if (result != null)
                try {
                    result.close();
                } catch (Exception e) {
                    // Nothing we can do here
                }

            // Disconnect
            connection.disconnect();

            // reportCacheStats();
        }
    }
}
