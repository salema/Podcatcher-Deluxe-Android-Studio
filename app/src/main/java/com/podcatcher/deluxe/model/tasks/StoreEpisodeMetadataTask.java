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

package com.podcatcher.deluxe.model.tasks;

import android.content.Context;
import android.text.TextUtils;

import com.podcatcher.deluxe.listeners.OnStoreEpisodeMetadataListener;
import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.types.EpisodeMetadata;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static com.podcatcher.deluxe.model.tags.METADATA.DOWNLOAD_ID;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_DATE;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_DESCRIPTION;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_DURATION;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_FILE_SIZE;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_MEDIA_TYPE;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_NAME;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_RESUME_AT;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_STATE;
import static com.podcatcher.deluxe.model.tags.METADATA.EPISODE_URL;
import static com.podcatcher.deluxe.model.tags.METADATA.LOCAL_FILE_PATH;
import static com.podcatcher.deluxe.model.tags.METADATA.METADATA;
import static com.podcatcher.deluxe.model.tags.METADATA.PLAYLIST_POSITION;
import static com.podcatcher.deluxe.model.tags.METADATA.PODCAST_NAME;
import static com.podcatcher.deluxe.model.tags.METADATA.PODCAST_URL;

/**
 * Stores the episode metadata information to the file system.
 */
public class StoreEpisodeMetadataTask extends StoreFileTask<Map<String, EpisodeMetadata>> {

    /**
     * Our context
     */
    protected Context context;
    /**
     * The call-back
     */
    protected OnStoreEpisodeMetadataListener listener;

    /**
     * The exception that might have been occurred
     */
    protected Exception exception;

    /**
     * Create a new persistence task.
     *
     * @param context  Context to use for file writing.
     * @param listener Call-back to alert on completion or failure.
     */
    public StoreEpisodeMetadataTask(Context context, OnStoreEpisodeMetadataListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @SafeVarargs
    @Override
    protected final Void doInBackground(Map<String, EpisodeMetadata>... params) {
        try {
            // 1. Do house keeping and remove all metadata instances without data
            cleanMetadata(params[0]);

            // 2. Open the file and get a writer
            OutputStream fileStream =
                    context.openFileOutput(EpisodeManager.METADATA_FILENAME, Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(fileStream, FILE_ENCODING));

            // 3. Write new file content
            writeHeader();
            for (Entry<String, EpisodeMetadata> entry : params[0].entrySet())
                writeRecord(entry.getKey(), entry.getValue());
            writeFooter();
        } catch (Exception ex) {
            this.exception = ex;

            cancel(true);
        } finally {
            // Make sure we close the file stream
            if (writer != null)
                try {
                    writer.close();
                } catch (IOException e) {
                    // Nothing we can do here
                }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void nothing) {
        if (listener != null)
            listener.onEpisodeMetadataStored();
    }

    @Override
    protected void onCancelled(Void nothing) {
        if (listener != null)
            listener.onEpisodeMetadataStoreFailed(exception);
    }

    private void writeRecord(String key, EpisodeMetadata value) throws IOException {
        writeLine(1, "<" + METADATA + " " + EPISODE_URL + "=\"" + TextUtils.htmlEncode(key) + "\">");

        // Data will only be written if present, see null checks in writeData()
        writeData(value.episodeName, EPISODE_NAME);
        if (value.episodePubDate != null)
            writeData(value.episodePubDate.getTime(), EPISODE_DATE);
        writeData(value.episodeDuration, EPISODE_DURATION);
        writeData(value.episodeFileSize, EPISODE_FILE_SIZE);
        writeData(value.episodeMediaType, EPISODE_MEDIA_TYPE);
        writeData(value.episodeDescription, EPISODE_DESCRIPTION);
        writeData(value.podcastName, PODCAST_NAME);
        writeData(value.podcastUrl, PODCAST_URL);
        writeData(value.downloadId, DOWNLOAD_ID);
        writeData(value.filePath, LOCAL_FILE_PATH);
        writeData(value.resumeAt, EPISODE_RESUME_AT);
        if (value.isOld != null && value.isOld)
            writeData(Boolean.TRUE.toString(), EPISODE_STATE);
        writeData(value.playlistPosition, PLAYLIST_POSITION);

        writeLine(1, "</" + METADATA + ">");
    }

    private void writeData(String data, String tag) throws IOException {
        // For all fields: only write data that is actually there!
        if (data != null)
            writeLine(2, "<" + tag + ">" + TextUtils.htmlEncode(data) + "</" + tag + ">");
    }

    private void writeData(Long data, String tag) throws IOException {
        // For all fields: only write data that is actually there!
        if (data != null)
            writeLine(2, "<" + tag + ">" + data + "</" + tag + ">");
    }

    private void writeData(Integer data, String tag) throws IOException {
        if (data != null)
            writeData(Long.valueOf(data), tag);
    }

    private void writeHeader() throws IOException {
        writeLine(0, "<?xml version=\"1.0\" encoding=\"" + FILE_ENCODING + "\"?>");
        writeLine(0, "<xml dateModified=\"" + new Date().getTime() + "\">");
    }

    private void writeFooter() throws IOException {
        writeLine(0, "</xml>");
    }

    private void cleanMetadata(Map<String, EpisodeMetadata> metadata) {
        Iterator<Entry<String, EpisodeMetadata>> iterator = metadata.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, EpisodeMetadata> entry = iterator.next();

            if (!entry.getValue().hasData())
                iterator.remove();
        }
    }
}
