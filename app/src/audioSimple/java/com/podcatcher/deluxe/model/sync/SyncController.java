/**
 * Copyright 2012-2015 Kevin Hausmann
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

package com.podcatcher.deluxe.model.sync;

import com.podcatcher.deluxe.listeners.OnChangePodcastListListener;
import com.podcatcher.deluxe.listeners.OnLoadPodcastListListener;
import com.podcatcher.deluxe.model.PodcastManager;
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.types.Podcast;

import android.net.Uri;

import java.util.List;

/**
 * An abstract sync controller to be extended for each specific service.
 * Existing implementations are listed in {@link ControllerImpl}.
 */
public abstract class SyncController implements OnChangePodcastListListener,
        OnLoadPodcastListListener {

    /**
     * The podcast manager handle
     */
    protected final PodcastManager podcastManager;

    /**
     * Interface definition for a callback to be alerted about the action taking
     * place in a {@link SyncController}. This is implemented by the
     * {@link SyncManager} to monitor the controller's state and behavior and to
     * make this information available to the rest of the app as needed.
     */
    public interface SyncControllerListener {

        /**
         * Called by a {@link SyncController} on its call-back to indicate that
         * a sync action was triggered internally. After this is called,
         * {@link SyncController#isRunning()} should return <code>true</code>
         * until {@link #onSyncCompleted(ControllerImpl)} or
         * {@link #onSyncFailed(ControllerImpl, Throwable)} is called.
         *
         * @param impl Controller that synced successfully.
         */
        void onSyncTriggered(ControllerImpl impl);

        /**
         * Called by a {@link SyncController} on its call-back to indicate that
         * a sync action completed normally. After this is called,
         * {@link SyncController#isRunning()} should return <code>false</code>.
         *
         * @param impl Controller that synced successfully.
         */
        void onSyncCompleted(ControllerImpl impl);

        /**
         * Called by a {@link SyncController} on its call-back when a sync
         * action failed to complete. After this is called,
         * {@link SyncController#isRunning()} should return <code>false</code>.
         *
         * @param impl  Controller that failed to sync contents.
         * @param cause The reason for the failure (might be <code>null</code>).
         */
        void onSyncFailed(ControllerImpl impl, Throwable cause);
    }

    /**
     * The sync listener handle
     */
    protected SyncControllerListener listener;

    /**
     * The sync mode the controller operates in.
     */
    public enum SyncMode {
        /**
         * Only send changes out to the service connected, do not change the
         * local data on the device.
         */
        SEND_ONLY,

        /**
         * Send and receive data. Update the local model to match the service
         * state. Only one controller can be in this mode at any point in time.
         */
        SEND_RECEIVE
    }

    /**
     * The current sync mode
     */
    protected SyncMode mode = SyncMode.SEND_ONLY;

    /**
     * Init the sync controller
     */
    protected SyncController() {
        this.podcastManager = PodcastManager.getInstance();

        podcastManager.addLoadPodcastListListener(this);
        podcastManager.addChangePodcastListListener(this);
    }

    /**
     * @return The {@link ControllerImpl} represented by this
     * {@link SyncController}.
     */
    public abstract ControllerImpl getImpl();

    /**
     * @return The {@link SyncMode} this controller is in.
     */
    public SyncMode getMode() {
        return mode;
    }

    /**
     * Set the {@link SyncMode} for this controller.
     *
     * @param mode Mode of operation.
     */
    public void setMode(SyncMode mode) {
        this.mode = mode;
    }

    /**
     * Set the listener to be alerted on sync events for this controller. Set
     * <code>null</code> to disable.
     *
     * @param listener The call-back.
     */
    public void setListener(SyncControllerListener listener) {
        this.listener = listener;
    }

    /**
     * Trigger all sync events for this controller. Depending on the
     * {@link SyncMode} set, the controller implementation sends and receives
     * all "syncable" data. The method returns quickly, all I/O is done off the
     * calling thread.
     */
    public final void sync() {
        syncSettings();
        syncPodcastList();
    }

    /**
     * @return Whether this controller is currently running any sync operation.
     */
    public abstract boolean isRunning();

    /**
     * Trigger sync of the settings for this controller. Depending on the
     * {@link SyncMode} set, the controller implementation sends and receives
     * the current settings. The method returns quickly, all I/O is done off the
     * calling thread.
     */
    protected abstract void syncSettings();

    /**
     * Trigger sync of the podcast list for this controller. Depending on the
     * {@link SyncMode} set, the controller implementation sends and receives
     * subscription information. The method returns quickly, all I/O is done off
     * the calling thread.
     */
    protected abstract void syncPodcastList();

    @Override
    public final void onPodcastListLoaded(List<Podcast> podcastList, Uri inputFile) {
        syncPodcastList();
    }

    @Override
    public final void onPodcastListLoadFailed(Uri inputFile, Exception error) {
        // pass, this is not a sync event
    }

    /**
     * Called when the controller is disabled.
     */
    public final void deactivate() {
        podcastManager.removeLoadPodcastListListener(this);
        podcastManager.removeChangePodcastListListener(this);

        onDeactivate();
    }

    /**
     * Hook for sub-classes that might want to react on the closing signal.
     */
    protected void onDeactivate() {
        // pass, sub-classes might want to do some house keeping here...
    }
}
