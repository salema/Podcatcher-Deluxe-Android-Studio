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

package com.podcatcher.deluxe.model.tasks;

import android.content.Context;
import android.net.Uri;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.listeners.OnStorePodcastListListener;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.tags.OPML;
import com.podcatcher.deluxe.model.types.Podcast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;

import static android.text.TextUtils.htmlEncode;
import static com.podcatcher.deluxe.model.PodcastManager.OPML_FILENAME;

/**
 * Stores the a podcast list to the file system asynchronously. Use
 * {@link OnStorePodcastListListener} to monitor the task.
 */
public class StorePodcastListTask extends StoreFileTask<List<Podcast>> {

    /**
     * The call-back
     */
    protected final OnStorePodcastListListener listener;
    /**
     * Our context
     */
    protected final Context context;
    /**
     * Content of OPML file title tag
     */
    protected final String opmlFileTitle;

    /**
     * The podcast list to store
     */
    protected List<Podcast> podcastList;
    /**
     * The file/dir that we write to.
     */
    protected Uri exportLocation;
    /**
     * Flag to indicate whether the task should write authorization information
     * to the resulting file.
     */
    protected boolean writeAuthorization = false;
    /**
     * The exception that might have been occurred
     */
    protected Exception exception;

    /**
     * Create new task with a call-back attached.
     *
     * @param context  Context to get file handle from (not <code>null</code>).
     * @param listener Listener to alert on completion or failure.
     * @see PodcastManager#OPML_FILENAME
     */
    public StorePodcastListTask(Context context, OnStorePodcastListListener listener) {
        this.context = context;
        this.listener = listener;

        // This is the default case, write to the private app directory
        this.exportLocation = Uri.fromFile(new File(context.getFilesDir(), OPML_FILENAME));
        this.opmlFileTitle = context.getString(R.string.app_name) + " podcast file";
    }

    /**
     * Define where the task should store the podcast OPML file. Not setting
     * this (or given <code>null</code> here) will result in the file being
     * stored to the private app directory.
     *
     * @param location The location to write to.
     */
    public void setCustomLocation(Uri location) {
        this.exportLocation = location;
    }

    /**
     * Sets the write authorization flag. If set to <code>true</code>, the
     * resulting OPML file will contain extra information on the user's
     * credentials for all podcasts in the list. The default is
     * <code>false</code>. Use with care!
     *
     * @param write Whether credentials should be written to output,
     *              defaults to <code>false</code>.
     */
    @SuppressWarnings("SameParameterValue")
    public void setWriteAuthorization(boolean write) {
        this.writeAuthorization = write;
    }

    @SafeVarargs
    @Override
    protected final Void doInBackground(List<Podcast>... params) {
        this.podcastList = params[0];

        try {
            // 1. Open the file and get a writer
            OutputStream fileStream;
            try {
                fileStream = context.getContentResolver().openOutputStream(exportLocation);
            } catch (FileNotFoundException fnfe) {
                // As a fall-back, we try to write into the directory of
                // the URI's path under the default name
                exportLocation = Uri.fromFile(
                        new File(exportLocation.getPath(), PodcastManager.OPML_FILENAME));
                fileStream = new FileOutputStream(exportLocation.getPath());
            }
            // Finally get the writer
            writer = new BufferedWriter(new OutputStreamWriter(fileStream,
                    PodcastManager.OPML_FILE_ENCODING));

            // 2. Write new file content
            writeHeader(opmlFileTitle);
            for (Podcast podcast : podcastList)
                writePodcast(podcast);
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
            listener.onPodcastListStored(podcastList, exportLocation);
    }

    @Override
    protected void onCancelled(Void nothing) {
        if (listener != null)
            listener.onPodcastListStoreFailed(podcastList, exportLocation, exception);
    }

    private void writePodcast(Podcast podcast) throws IOException {
        // Only write out valid podcasts
        if (hasNameAndUrl(podcast)) {
            String opmlString = String.format("<%s %s=\"%s\" %s=\"%s\" %s=\"%s\" %s=\"%s\"",
                    OPML.OUTLINE,
                    OPML.TEXT, htmlEncode(podcast.getName()),
                    OPML.TITLE, htmlEncode(podcast.getName()),
                    OPML.TYPE, OPML.RSS_TYPE,
                    OPML.XMLURL, htmlEncode(podcast.getUrl()));

            if (podcast.hasLogoUrl())
                opmlString = String.format("%s %s=\"%s\"", opmlString,
                        OPML.PCD_NS_PREFIX + OPML.PCD_LOGO, podcast.getLogoUrl());

            if (writeAuthorization && podcast.getAuthorization() != null)
                // We store the podcast password in the app's private folder
                // (but in the clear). This is justified because it is hard to
                // attack the file (unless you get your hands on the device) and
                // the password is not very sensitive since it is only a
                // podcast we are accessing, not personal information.
                opmlString = String.format("%s %s=\"%s\" %s=\"%s\"", opmlString,
                        OPML.PCD_NS_PREFIX + OPML.PCD_USER, htmlEncode(podcast.getUsername()),
                        OPML.PCD_NS_PREFIX + OPML.PCD_PASS, htmlEncode(podcast.getPassword()));

            writeLine(2, opmlString + " />");
        }
    }

    /**
     * @return Whether given podcast has an non-empty name and an URL.
     */
    private boolean hasNameAndUrl(Podcast podcast) {
        return podcast.getName() != null && podcast.getName().trim().length() > 0
                && podcast.getUrl() != null && podcast.getUrl().startsWith("http");
    }

    private void writeHeader(String fileName) throws IOException {
        writeLine(0, "<?xml version=\"1.0\" encoding=\"" + FILE_ENCODING + "\"?>");
        writeLine(0, "<opml xmlns:pcd=\"" + OPML.PCD_NAMESPACE + "\" version=\"2.0\">");
        writeLine(1, "<head>");
        writeLine(2, "<title>" + fileName + "</title>");
        writeLine(2, "<dateModified>" + new Date().toString() + "</dateModified>");
        writeLine(1, "</head>");
        writeLine(1, "<body>");
    }

    private void writeFooter() throws IOException {
        writeLine(1, "</body>");
        writeLine(0, "</opml>");
    }
}
