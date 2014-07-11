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

package com.podcatcher.deluxe;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.os.Process;

import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.SuggestionManager;
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.tasks.LoadEpisodeMetadataTask;
import com.podcatcher.deluxe.model.tasks.LoadPodcastListTask;

import java.io.File;
import java.io.IOException;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Our application subclass. Holds global state and model. The Podcatcher
 * application object is created on application startup and will be alive for
 * all the app's lifetime. Its main purpose is to hold handles to the singleton
 * instances of our model data and data managers. In addition, it provides some
 * generic convenience methods.
 */
public class Podcatcher extends Application {

    /**
     * The http request header field key for the user agent
     */
    public static final String USER_AGENT_KEY = "User-Agent";
    /**
     * The user agent string we use to identify us
     */
    public static final String USER_AGENT_VALUE = "Podcatcher Deluxe";
    /**
     * The http request header field key for the authorization
     */
    public static final String AUTHORIZATION_KEY = "Authorization";

    /**
     * The HTTP cache size
     */
    public static final long HTTP_CACHE_SIZE = 8 * 1024 * 1024; // 8 MiB

    /**
     * Thread to move the http cache flushing off the UI thread
     */
    private static class FlushCacheThread extends Thread {

        @Override
        public void run() {
            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);

            final HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache != null)
                cache.flush();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // This will only run once in the lifetime of the app
        // since the application is an implicit singleton. We create the other
        // singletons here to make sure they know their application instance.
        PodcastManager.getInstance(this);
        // And this one as well
        EpisodeManager.getInstance(this);
        // dito
        SuggestionManager.getInstance(this);
        // and sync as well
        SyncManager.getInstance(this);

        // Enabled caching for our HTTP connections
        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            HttpResponseCache.install(httpCacheDir, HTTP_CACHE_SIZE);
        } catch (IOException ioe) {
            // This should not happen, but the app works without the cache
        }

        // Now we will trigger the preparation on start-up, steps include:
        // 1. Load podcast list from file async, once this is finished the
        // podcast manager is alerted and in turn tells the controller activity.
        // Then the UI can show the list and we are ready to go
        new LoadPodcastListTask(this, PodcastManager.getInstance())
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
        // 2. At the same time we load episode metadata from file async (this
        // has the potential to take a lot of time, since the amount of data
        // might be quite big). The UI is functional without this having
        // completed, but loading of podcasts, downloads or the playlist will
        // block until the data is available.
        new LoadEpisodeMetadataTask(this, EpisodeManager.getInstance())
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    /**
     * Write http cache data to disk (async).
     */
    public void flushHttpCache() {
        new FlushCacheThread().start();
    }

    /**
     * Checks whether the device is currently online and can receive data from
     * the internets.
     *
     * @return <code>true</code> iff we have Internet access.
     */
    public boolean isOnline() {
        final NetworkInfo activeNetwork = getNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Checks whether the device is currently on a fast network (such as wifi)
     * as opposed to a mobile network.
     *
     * @return <code>true</code> iff we have fast (and potentially free)
     * Internet access.
     */
    public boolean isOnFastConnection() {
        final NetworkInfo activeNetwork = getNetworkInfo();

        if (activeNetwork == null)
            return false;
        else
            switch (activeNetwork.getType()) {
                case ConnectivityManager.TYPE_ETHERNET:
                case ConnectivityManager.TYPE_WIFI:
                case ConnectivityManager.TYPE_WIMAX:
                    return true;
                default:
                    return false;
            }
    }

    private NetworkInfo getNetworkInfo() {
        final ConnectivityManager manager = (ConnectivityManager) getApplicationContext()
                .getSystemService(CONNECTIVITY_SERVICE);

        return manager.getActiveNetworkInfo();
    }
}
