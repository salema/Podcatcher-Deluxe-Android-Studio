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

package com.podcatcher.deluxe.model.sync.dropbox;

import com.podcatcher.deluxe.model.types.Episode;
import com.podcatcher.deluxe.model.types.Podcast;

import android.content.Context;

import java.nio.charset.Charset;
import java.security.MessageDigest;


/**
 * A sync controller for the dropbox service dealing with the episode state.
 */
abstract class DropboxEpisodeMetadataSyncController extends DropboxPodcastListSyncController {

    /**
     * The id charset
     */
    protected Charset utf8 = Charset.forName("UTF-8");
    /**
     * The message digest hash function for id creation
     */
    private MessageDigest md5Hash;

    /**
     * The sync running flag
     */
    private boolean syncRunning = false;

    protected DropboxEpisodeMetadataSyncController(Context context) {
        super(context);
    }

    @Override
    public boolean isRunning() {
        return syncRunning || super.isRunning();
    }

    @Override
    protected void syncEpisodeMetadata() {

    }

    @Override
    public void onPodcastRemoved(Podcast podcast) {
        super.onPodcastRemoved(podcast);

        // TODO Remove all episode metadata for the removed podcast here?
    }

    @Override
    public void onStateChanged(Episode episode, boolean newState) {

    }

    @Override
    public void onResumeAtChanged(Episode episode, Integer millis) {

    }

    @Override
    public void onDownloadSuccess(Episode episode) {
        // pass, download events are not synced via Dropbox
    }

    @Override
    public void onDownloadDeleted(Episode episode) {
        // pass, deletion events are not synced via Dropbox
    }

    /**
     * Convert a given string to a valid Dropbox data store ID, to be used for
     * records etc. This is a well-behaved hash function, giving equal output
     * for equal input and avoiding collision.
     *
     * @param input The string to be hashed.
     * @return A valid, unique data store id.
     */
    protected String toValidDataStoreId(String input) {
        md5Hash.update(input.getBytes(utf8));
        byte[] bytes = md5Hash.digest();

        // The bytes[] has bytes in decimal format, convert it
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes)
            sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));

        return sb.toString();
    }
}
