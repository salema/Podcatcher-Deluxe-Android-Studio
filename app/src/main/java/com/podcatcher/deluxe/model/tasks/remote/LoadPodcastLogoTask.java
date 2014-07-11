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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.listeners.OnLoadPodcastLogoListener;
import com.podcatcher.deluxe.model.types.Podcast;
import com.podcatcher.deluxe.model.types.Progress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import static java.lang.Math.round;

/**
 * An async task to load a podcast logo. Implement
 * {@link OnLoadPodcastLogoListener} to be alerted on completion or failure.
 * <p>
 * <b>Scaling and caching:</b> This task has some internal image scaling and
 * caching logic. Podcast logos are only downloaded or updated if absolutely
 * necessary. You can manipulate its behaviour by calling the
 * {@link #setLocalOnly(boolean)} and {@link #setMaxAge(int)} methods. Once
 * downloaded and scaled, all podcast logos are stored in the app's private
 * folders under the path <tt>logoCache/<i>podcast URL hash</i>.jpeg</tt>. The
 * task will prefer these downloaded copies whenever possible.
 * </p>
 */
public class LoadPodcastLogoTask extends LoadRemoteFileTask<Podcast, Bitmap> {

    /**
     * The name of the podcast logo cache directory
     */
    private static final String CACHE_DIR = "logoCache";
    /**
     * The file name ending for cached logos
     */
    private static final String CACHED_LOGO_ENDING = ".jpeg";
    /**
     * Our log tag
     */
    private static final String TAG = "LoadPodcastLogoTask";

    /**
     * Call back
     */
    private final OnLoadPodcastLogoListener listener;
    /**
     * The task's context
     */
    private final Context context;

    /**
     * Podcast currently loading logo for
     */
    private Podcast podcast;

    /**
     * Flag to indicate that we should return local copies only.
     */
    private boolean localOnly = false;
    /**
     * Flag to indicate the max age that would trigger re-load.
     */
    private int maxAge = 60 * 24 * 7; // One week is the default

    /**
     * Create new task.
     *
     * @param context  The context the task is carried out in.
     * @param listener Callback to be alerted on progress and completion.
     */
    public LoadPodcastLogoTask(Context context, OnLoadPodcastLogoListener listener) {
        this.context = context;
        this.listener = listener;
    }

    /**
     * Set whether the task should try to get the podcast logo from the
     * internet. Default is <code>false</code>. This overwrites
     * {@link #setMaxAge(int)}.
     *
     * @param localOnly The flag. If <code>true</code> and there is no cached
     *                  logo available, the task will be cancelled and fail.
     */
    public void setLocalOnly(boolean localOnly) {
        this.localOnly = localOnly;
    }

    /**
     * Set the age of the cached logo that would trigger a reload from the
     * internets. The default is equivalent to one week.
     *
     * @param minutes Maximum age in minutes.
     */
    public void setMaxAge(int minutes) {
        this.maxAge = minutes;
    }

    @Override
    protected Bitmap doInBackground(Podcast... podcasts) {
        this.podcast = podcasts[0];
        Bitmap result = null;

        // Let's go. There are a lot of options here, since we really want to
        // optimize this for re-using the cached versions as much as possible.
        try {
            publishProgress(Progress.CONNECT);
            // 1. So this the simple case where we have the local version and
            // it is fresh enough. Return it.
            if (isCachedLocally(podcast) && getCachedLogoAge(podcast) <= maxAge) {
                publishProgress(Progress.PARSE);
                result = restoreBitmapFromFileCache(podcast);
            }
            // 2. If that is not the case, we need to go over the air, unless
            // the localOnly flag is set or we do not know the remote location.
            else if (!localOnly && podcast.getLogoUrl() != null) {
                // 2a. Get logo data remotely
                this.authorization = podcast.getAuthorization();
                final byte[] logo = loadFile(new URL(podcast.getLogoUrl()));

                // 2b. Decode and sample the result
                if (!isCancelled()) {
                    publishProgress(Progress.PARSE);
                    result = decodeAndSampleBitmap(logo);
                }

                // 2c. Save to file
                if (!isCancelled())
                    storeBitmapToFileCache(podcast, result);
            }
            // 3. No fresh cached logo available and we cannot get it over the
            // air. Throw an exception, the catch clause will try to get any
            // stale cached version.
            else
                throw new IOException();
        } catch (Throwable throwable) {
            // Return the cached version even though it is stale (having an old
            // logo for the podcast is better then having none).
            if (isCachedLocally(podcast)) {
                publishProgress(Progress.PARSE);
                result = restoreBitmapFromFileCache(podcast);
            }
            // We are out of options here
            else {
                Log.d(TAG, "Logo failed to load for podcast \"" + podcast +
                        "\" with logo URL " + podcast.getLogoUrl(), throwable);

                cancel(true);
            }
        } finally {
            publishProgress(Progress.DONE);
        }

        return result;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        // Set the logo as a podcast member
        podcast.setLogo(result);

        // Podcast logo was loaded
        if (listener != null)
            listener.onPodcastLogoLoaded(podcast);
    }

    @Override
    protected void onCancelled(Bitmap result) {
        // Background task failed to complete
        if (listener != null)
            listener.onPodcastLogoLoadFailed(podcast);
    }

    /**
     * Create a memory-efficient bitmap at the correct size needed for the
     * application.
     *
     * @param data Bitmap data loaded from the Internet.
     * @return The decoded and sampled bitmap.
     */
    protected Bitmap decodeAndSampleBitmap(byte[] data) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        // Decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    /**
     * Calculate the sample size for the image.
     *
     * @param options Bitmap options to work with.
     * @return The sample size.
     */
    protected int calculateInSampleSize(BitmapFactory.Options options) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int sampleSize = 1;

        // Adjust max height/width according to screen resolution
        final int max = context.getResources().getDimensionPixelSize(R.dimen.logo_size);
        // ... and calculate sample size
        if (height > max || width > max)
            sampleSize = round((width > height ? (float) height : (float) width) / (float) max);

        return sampleSize;
    }

    private File getLogoCacheFile(Podcast podcast) {
        // Create the complete path leading to where we expect the cached file
        return new File(context.getCacheDir(), CACHE_DIR + File.separator
                + podcast.getUrl().hashCode() + CACHED_LOGO_ENDING);
    }

    private boolean isCachedLocally(Podcast podcast) {
        return getLogoCacheFile(podcast).exists();
    }

    private int getCachedLogoAge(Podcast podcast) {
        if (isCachedLocally(podcast))
            return (int) ((new Date().getTime() - getLogoCacheFile(podcast).lastModified())
                    / (60 * 1000)); // Calculate to minutes
        else
            return -1;
    }

    private Bitmap restoreBitmapFromFileCache(Podcast podcast) {
        return BitmapFactory.decodeFile(getLogoCacheFile(podcast).getAbsolutePath());
    }

    private void storeBitmapToFileCache(Podcast podcast, Bitmap bitmap) {
        final File logoCacheDir = new File(context.getCacheDir(), CACHE_DIR);
        logoCacheDir.mkdirs();

        FileOutputStream out = null;
        // If this fails, we have no cached version, but that's okay
        try {
            out = new FileOutputStream(getLogoCacheFile(podcast));

            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
        } catch (IOException e) {
            // pass
        } finally {
            try {
                out.close();
            } catch (Throwable e) {
                // Nothing more we could do here
            }
        }
    }
}
