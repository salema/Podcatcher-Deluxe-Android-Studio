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

package com.podcatcher.deluxe.model;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.podcatcher.deluxe.Podcatcher;
import com.podcatcher.deluxe.SettingsActivity;
import com.podcatcher.deluxe.listeners.OnSyncListener;
import com.podcatcher.deluxe.model.sync.ControllerImpl;
import com.podcatcher.deluxe.model.sync.SyncController;
import com.podcatcher.deluxe.model.sync.SyncController.SyncControllerListener;
import com.podcatcher.deluxe.model.sync.SyncController.SyncMode;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Singleton sync manager to enabled/disable sync controllers. Listens to
 * settings and adds/removes controllers accordingly. Call {@link #syncAll()} to
 * manually trigger a sync for all enabled controllers.
 */
public class SyncManager implements SyncControllerListener {

    /**
     * The timeout that will trigger a sync event (in millis)
     */
    public static final int TIME_TO_SYNC = (int) TimeUnit.MINUTES.toMillis(15);
    /**
     * The timeout that will trigger a sync event when on mobile data (in millis)
     */
    public static final int TIME_TO_SYNC_MOBILE = (int) TimeUnit.MINUTES.toMillis(60);
    /**
     * The interval to check whether sync is needed (fifteen minutes)
     */
    private final static int TRIGGER_SYNC_HANDLER_INTERVAL = TIME_TO_SYNC;
    /**
     * The actual runnable doing the trigger sync work
     */
    private final Runnable triggerSyncRunnable = new Runnable() {

        @Override
        public void run() {
            final boolean online = podcatcher.isOnline();
            final Date lastSync = getLastFullSyncDate();
            final Date triggerIfSyncedBefore = new Date(new Date().getTime() -
                    (podcatcher.isOnMeteredConnection() ? TIME_TO_SYNC_MOBILE : TIME_TO_SYNC));

            // If we are online and the last sync is sufficiently old, trigger a
            // full sync on all active sync controllers (on the main UI thread)
            if (online && (lastSync == null || lastSync.before(triggerIfSyncedBefore)))
                syncAll();

            triggerSyncHandler.postDelayed(this, TRIGGER_SYNC_HANDLER_INTERVAL);
        }
    };
    /**
     * The check sync status timeout (in ms)
     */
    private final static int MONITOR_SYNC_HANDLER_INTERVAL = (int) TimeUnit.SECONDS.toMillis(1);
    /**
     * Our log tag
     */
    private static final String TAG = "SyncManager";
    /**
     * The single instance
     */
    private static SyncManager manager;
    /**
     * The application itself
     */
    private final Podcatcher podcatcher;
    /**
     * Our set of active controllers
     */
    private final Set<SyncController> activeControllers = new HashSet<>();
    /**
     * The shared app preferences
     */
    private final SharedPreferences preferences;
    /**
     * The handler used to periodically trigger sync
     */
    private final Handler triggerSyncHandler = new Handler(Looper.getMainLooper());
    /**
     * The call-back set for the sync listeners
     */
    private final Set<OnSyncListener> syncListeners = new HashSet<>();
    /**
     * The actual runnable doing the sync monitoring work
     */
    private final Runnable monitorSyncRunnable = new Runnable() {

        public void run() {
            if (isSyncRunning())
                monitorSyncHandler.postDelayed(this, MONITOR_SYNC_HANDLER_INTERVAL);
            else
                for (OnSyncListener listener : syncListeners)
                    listener.onSyncCompleted();
        }
    };
    /**
     * The handler used to monitor sync progress
     */
    private final Handler monitorSyncHandler = new Handler(Looper.getMainLooper());

    /**
     * Init the sync manager.
     *
     * @param app The podcatcher application object (also a singleton).
     */
    private SyncManager(Podcatcher app) {
        this.podcatcher = app;

        this.preferences = PreferenceManager.getDefaultSharedPreferences(app);
        initFromSyncPreferences();

        // Start the sync timer (first runs 15 minutes after start-up)
        triggerSyncHandler.postDelayed(triggerSyncRunnable, TRIGGER_SYNC_HANDLER_INTERVAL);
    }

    /**
     * Get the singleton instance of the sync manager.
     *
     * @param podcatcher Application handle.
     * @return The singleton instance.
     */
    public static SyncManager getInstance(Podcatcher podcatcher) {
        // If not done, create single instance
        if (manager == null)
            manager = new SyncManager(podcatcher);

        return manager;
    }

    /**
     * Get the singleton instance of the sync manager.
     *
     * @return The singleton instance.
     */
    public static SyncManager getInstance() {
        // We make sure in Application.onCreate() that this method is not called
        // unless the other one with the application instance actually set ran
        // to least once
        return manager;
    }

    /**
     * Trigger sync for all active controllers.
     * <p>
     * This will push out all changes and make the receiving controller (if any)
     * update the app's data model. If the sync is successful,
     * {@link #getLastFullSyncDate()} will return the point in time
     * {@link SyncController} reported back. All I/O is done off the main
     * thread, so this method returns quickly. Please note that depending on the
     * state of the device (e.g. network availability) the results of the sync
     * operations triggered will vary.
     * </p>
     * <p>
     * To monitor the sync action, implement {@link OnSyncListener} and register
     * using {@link #addSyncListener(OnSyncListener)}.
     * </p>
     */
    public void syncAll() {
        for (SyncController controller : activeControllers)
            controller.sync();

        // This will alert the listeners and start the monitoring
        if (!activeControllers.isEmpty())
            onSyncTriggered(null);
    }

    /**
     * @return Whether any sync operation is currently running.
     */
    public boolean isSyncRunning() {
        boolean result = false;

        for (SyncController controller : activeControllers)
            result = result || controller.isRunning();

        return result;
    }

    /**
     * @param impl A {@link SyncController} implementation to check.
     * @return Whether the given sync controller is currently busy syncing.
     */
    public boolean isSyncRunning(ControllerImpl impl) {
        boolean result = false;

        for (SyncController controller : activeControllers)
            if (controller.getImpl().equals(impl))
                result = controller.isRunning();

        return result;
    }

    /**
     * @return The point in time {@link #syncAll()} has been called the last
     * time or <code>null</code> if it never ran.
     */
    public Date getLastFullSyncDate() {
        return preferences.contains(SettingsActivity.KEY_LAST_SYNC) ?
                new Date(preferences.getLong(SettingsActivity.KEY_LAST_SYNC, 0)) : null;
    }

    /**
     * @return The number of controllers currently syncing.
     */
    public int getActiveControllerCount() {
        return activeControllers.size();
    }

    /**
     * Get the mode for a given controller representation.
     *
     * @param impl The controller to look for.
     * @return The {@link SyncMode} if a controller instance of the given type
     * is active, <code>null</code> otherwise.
     */
    public SyncMode getSyncMode(ControllerImpl impl) {
        for (SyncController controller : activeControllers)
            if (controller.getImpl().equals(impl))
                return controller.getMode();

        return null;
    }

    /**
     * Set the {@link SyncMode} for the given controller representation. This
     * will create new {@link SyncController} instances and activate them as
     * needed. Giving <code>null</code> as its mode will disable a controller.
     * This will also side-effect other active controller when mode is given as
     * {@link SyncMode#SEND_RECEIVE}, since only one controller can be in this
     * mode at any point. Hence this method will overwrite the current setting
     * as needed.
     *
     * @param impl The controller to set mode for.
     * @param mode The new mode for the controller given.
     */
    public void setSyncMode(ControllerImpl impl, SyncMode mode) {
        // There are a couple of cases here. First, disable a controller:
        if (mode == null) {
            final Iterator<SyncController> iterator = activeControllers.iterator();

            while (iterator.hasNext()) {
                SyncController controller = iterator.next();

                if (controller.getImpl().equals(impl)) {
                    iterator.remove();

                    controller.setListener(null);
                    controller.deactivate();
                }
            }
        }
        // Second: Find controller and set its mode (or create it)
        else {
            SyncController controllerChanged = null;

            for (SyncController controller : activeControllers) {
                // We need to make sure only on controller is receiving once
                // this method finishes. Therefore we switch modes for all
                // controllers here and enable the right one below.
                if (SyncMode.SEND_RECEIVE.equals(mode))
                    controller.setMode(SyncMode.SEND_ONLY);

                if (controller.getImpl().equals(impl))
                    controllerChanged = controller;
            }

            // Not found, create new controller
            if (controllerChanged == null) {
                controllerChanged = ControllerImpl.create(podcatcher, impl);

                controllerChanged.setListener(this);
                activeControllers.add(controllerChanged);
            }

            controllerChanged.setMode(mode);
        }

        // Make sure to store the new configuration
        writeToSyncPreferences();

        for (SyncController controller : activeControllers)
            Log.d(TAG, "Controller " + controller.getImpl() + ", mode " + controller.getMode());
    }

    @Override
    public void onSyncTriggered(ControllerImpl impl) {
        // Alert all listeners that there is some action
        for (OnSyncListener listener : syncListeners)
            listener.onSyncStarted();

        // Monitor the sync action and alert listeners when finished
        monitorSyncHandler.postDelayed(monitorSyncRunnable, MONITOR_SYNC_HANDLER_INTERVAL);
    }

    @Override
    public void onSyncCompleted(ControllerImpl impl) {
        Log.d(TAG, "Controller " + impl + " completed sync.");

        preferences.edit()
                .putLong(SettingsActivity.KEY_LAST_SYNC, new Date().getTime())
                .apply();
    }

    @Override
    public void onSyncFailed(ControllerImpl impl, Throwable cause) {
        Log.d(TAG, "Controller " + impl + " failed to sync.", cause);
    }

    /**
     * Add sync listener.
     *
     * @param listener Listener to add.
     * @see OnSyncListener
     */
    public void addSyncListener(OnSyncListener listener) {
        syncListeners.add(listener);
    }

    /**
     * Remove sync listener.
     *
     * @param listener Listener to remove.
     * @see OnSyncListener
     */
    public void removeSyncListener(OnSyncListener listener) {
        syncListeners.remove(listener);
    }

    private void initFromSyncPreferences() {
        // Create, add and register controllers as in preferences
        final Set<String> controllerIds = preferences.getStringSet(
                SettingsActivity.KEY_SYNC_ACTIVE, null);
        if (controllerIds != null)
            for (String id : controllerIds) {
                final SyncController controller =
                        ControllerImpl.create(podcatcher, ControllerImpl.valueOf(id));
                activeControllers.add(controller);
                controller.setListener(this);
                controller.setMode(SyncMode.SEND_ONLY);
            }

        // Activate receiving controller (if any)
        if (preferences.getString(SettingsActivity.KEY_SYNC_RECEIVE, null) != null) {
            final ControllerImpl receivingController = ControllerImpl
                    .valueOf(preferences.getString(SettingsActivity.KEY_SYNC_RECEIVE, null));

            for (SyncController controller : activeControllers)
                if (controller.getImpl().equals(receivingController))
                    controller.setMode(SyncMode.SEND_RECEIVE);
        }

        for (SyncController controller : activeControllers)
            Log.d(TAG, "Controller " + controller.getImpl() + ", mode " + controller.getMode());
    }

    private void writeToSyncPreferences() {
        final Set<String> controllerIds = new HashSet<>();
        String receivingController = null;

        for (SyncController controller : activeControllers) {
            controllerIds.add(controller.getImpl().name());

            if (SyncMode.SEND_RECEIVE.equals(controller.getMode()))
                receivingController = controller.getImpl().name();
        }

        preferences.edit().putStringSet(SettingsActivity.KEY_SYNC_ACTIVE, controllerIds)
                .putString(SettingsActivity.KEY_SYNC_RECEIVE, receivingController).apply();
    }
}
