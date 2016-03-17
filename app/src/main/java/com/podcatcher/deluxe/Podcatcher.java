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

package com.podcatcher.deluxe;

import com.podcatcher.deluxe.model.EpisodeManager;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.SuggestionManager;
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.tasks.LoadEpisodeMetadataTask;
import com.podcatcher.deluxe.model.tasks.LoadPodcastListTask;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.podcatcher.deluxe.BuildConfig.VERSION_NAME;

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
    public static String userAgentValue;
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

        // First things first, set-up user agent
        userAgentValue = String.format("%1$s/%2$s", getString(R.string.app_name), VERSION_NAME);

        Picasso.with(this).setIndicatorsEnabled(BuildConfig.DEBUG);

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

        // If permissions are limited, disable auto-download and auto-delete.
        // This code will run if the user pulls the permission from under us,
        // since the system will restart the app.
        if (!canWriteExternalStorage()) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            preferences.edit()
                    .putBoolean(SettingsActivity.KEY_AUTO_DELETE, false)
                    .putBoolean(SettingsActivity.KEY_AUTO_DOWNLOAD, false).apply();
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

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        // Make sure we save our state before eventually being killed
        PodcastManager.getInstance().saveState();
        EpisodeManager.getInstance().saveState();
    }

    /**
     * Write http cache data to disk (async).
     */
    public void flushHttpCache() {
        new FlushCacheThread().start();
    }

    /**
     * @return The state of the 'write external storage' runtime permission.
     * Will always be <code>true</code> on Android versions < 6.0.
     */
    public boolean canWriteExternalStorage() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks whether the device is currently online and can receive data from
     * the internets.
     *
     * @return <code>true</code> iff we have Internet access.
     */
    public boolean isOnline() {
        final NetworkInfo activeNetwork = getConnectivityManager().getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Checks whether the device is currently on a fast network (such as wifi)
     * as opposed to a mobile network.
     *
     * @return <code>true</code> iff we have fast (and potentially free) Internet access.
     */
    public boolean isOnFastConnection() {
        final NetworkInfo activeNetwork = getConnectivityManager().getActiveNetworkInfo();

        return activeNetwork != null &&
                (activeNetwork.getType() == ConnectivityManager.TYPE_ETHERNET ||
                        activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ||
                        activeNetwork.getType() == ConnectivityManager.TYPE_WIMAX);
    }

    /**
     * Checks whether the device is currently on a slow (i.e. 2nd generation)
     * mobile data connection.
     *
     * @return <code>true</code> iff the current data connection is via mobile data and slow.
     * Note that this will return <code>false</code> when offline.
     */
    public boolean isOnSlowMobileData() {
        final NetworkInfo activeNetwork = getConnectivityManager().getActiveNetworkInfo();
        final TelephonyManager telephonyManager =
                (TelephonyManager) getApplicationContext().getSystemService(TELEPHONY_SERVICE);

        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE &&
                (telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE ||
                        telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_GPRS);
    }

    /**
     * Checks whether the device is on a metered network. If this information is not available,
     * the inversion of {@link #isOnFastConnection()} is returned.
     *
     * @return <code>true</code> iff we have metered (and potentially slow) Internet access.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public boolean isOnMeteredConnection() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                getConnectivityManager().isActiveNetworkMetered() :
                !isOnFastConnection();
    }

    private ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
    }
}
