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

package com.podcatcher.deluxe.model.sync.dropbox;

import com.podcatcher.deluxe.R;
import com.podcatcher.deluxe.model.SyncManager;
import com.podcatcher.deluxe.model.sync.SyncController;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxAccountManager.AccountListener;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastore.SyncStatusListener;
import com.dropbox.sync.android.DbxDatastoreStatus;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A sync controller for the Dropbox service, abstract base class. Provides some
 * common functionality, in particular the {@link #syncStore()} method.
 * Sub-classes should call it and react to {@link #onSyncStoreComplete()}. Also,
 * use {@link #toValidDataStoreId(String)} for table record creation.
 */
abstract class DropboxBaseSyncController extends SyncController
        implements AccountListener, SyncStatusListener {

    /**
     * Our log tag
     */
    protected static final String TAG = "DropboxSyncController";
    /**
     * Our account manager handle
     */
    private final DbxAccountManager accountManager;
    /**
     * Our datastore handle
     */
    protected DbxDatastore store;
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

    protected DropboxBaseSyncController(Context context) {
        super();

        this.accountManager = getAccountManager(context);
        accountManager.addListener(this);

        final DbxAccount account = accountManager.getLinkedAccount();

        try {
            // Open the Dropbox data store
            this.store = DbxDatastore.openDefault(account);
            store.addSyncStatusListener(this);

            // To make this delete all contents in the Dropbox data store
            // uncomment this and comment out the table handle getters in the
            // sub-classes, then toggle the controller a few times
            // syncStore();
            // printStore();
            // clearStore();
            // printStore();

            // Init the hash for id creation
            this.md5Hash = MessageDigest.getInstance("MD5");
        } catch (DbxException e) {
            Log.d(TAG, "Dropbox error on openDefault()", e);
        } catch (NullPointerException npe) {
            Log.d(TAG, "No Dropbox account linked, store unavailable");
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "MD5 for hash creation not available");
        }
    }

    /**
     * Get the Dropbox account manager handle.
     *
     * @param context The context we live in.
     * @return The handle.
     */
    public static DbxAccountManager getAccountManager(Context context) {
        final String appKey = context.getResources().getString(R.string.dropbox_appkey);
        final String appSecret = context.getResources().getString(R.string.dropbox_appsecret);

        return DbxAccountManager.getInstance(context.getApplicationContext(), appKey, appSecret);
    }

    @Override
    public void onLinkedAccountChange(DbxAccountManager manager, DbxAccount account) {
        // Disable sync controller if Dropbox account is unlinked
        if (!account.isLinked())
            SyncManager.getInstance().setSyncMode(getImpl(), null);
    }

    @Override
    public void onDatastoreStatusChange(DbxDatastore datastore) {
        final DbxDatastoreStatus status = datastore.getSyncStatus();

        // If the data store has incoming data, trigger its download and apply
        // changes. Since we are only interested in the receiving part, calling
        // syncStore and running its call-backs is sufficient.
        if (status.hasIncoming) {
            Log.d(TAG, "Sync store triggered by data store status change");

            if (listener != null)
                listener.onSyncTriggered(getImpl());

            syncStore();
        }
    }

    @Override
    public boolean isRunning() {
        return syncRunning;
    }

    @Override
    protected void onDeactivate() {
        super.onDeactivate();

        accountManager.removeListener(this);

        if (store != null) {
            store.removeSyncStatusListener(this);
            store.close();
        }
    }

    /**
     * Sync the Dropbox data store. This will have no effect if a sync is
     * already under way. Therefore you can safely call this as often as you
     * like and simply react to the result in {@link #onSyncStoreComplete()} and
     * {@link #onSyncStoreFailed()}. While this method returns immediately,
     * {@link #onSyncStoreComplete()} will only run once all changes are stored
     * and the remote data is available locally, i.e. all network traffic is
     * finished.
     */
    protected synchronized final void syncStore() {
        if (!syncRunning) {
            syncRunning = true;

            new SyncStoreTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, (Void) null);
        }
    }

    /**
     * Hook for sub-classes to react on the successful completion of the Dropbox
     * data store sync.
     */
    protected void onSyncStoreComplete() {
        // pass, sub-classes should add actions here
    }

    /**
     * Hook for sub-classes to react to a sync failure.
     */
    protected void onSyncStoreFailed() {
        // pass, sub-classes should add actions here
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

    protected void printStore() {
        Set<DbxTable> tables = store.getTables();
        Log.d(TAG, "Tables in local store: " + tables.size());

        for (DbxTable table : tables) {
            Log.d(TAG, "Table: " + table.getId());

            try {
                for (DbxRecord dbxRecord : table.query())
                    Log.d(TAG, "Record: " + dbxRecord);
            } catch (DbxException e) {
                Log.d(TAG, "Exception while printing local store", e);
            }
        }
    }

    private void clearStore() throws DbxException {
        Set<DbxTable> tables = store.getTables();

        for (DbxTable table : tables)
            for (DbxRecord dbxRecord : table.query()) dbxRecord.deleteRecord();

        syncStore();
        Log.d(TAG, "Dropbox Datastore cleared.");
    }

    /**
     * Our async task triggering the actual sync machinery
     */
    private class SyncStoreTask extends AsyncTask<Void, Void, Void> {

        /**
         * The reason for failure if it occurs
         */
        private Throwable cause;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                store.sync();

                // We want the remote and local data stores to be equal before
                // we finish the task and call listeners
                DbxDatastoreStatus status = store.getSyncStatus();
                int sleepInterval = 250;
                while (status.isDownloading && !isCancelled())
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepInterval);

                        // Increase the timeout to back off a bit, but not too
                        // much (we want to pull at least once a minute and see
                        // what's going on (this is all local, of course))
                        sleepInterval *= 2;
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Interrupted while waiting for sync to come down", e);
                    } finally {
                        if (sleepInterval > TimeUnit.MINUTES.toMillis(1)) {
                            // We do want this to run forever, so we simply
                            // cancel the whole task after too long a wait. The
                            // next sync will hopefully fix things...
                            cancel(true);
                            Log.d(TAG, "Syncing datastore took too long!");
                        } else
                            status = store.getSyncStatus();
                    }
            } catch (DbxException | IllegalStateException | NullPointerException e) {
                this.cause = e;
                cancel(true);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            onSyncStoreComplete();
            syncRunning = false;

            if (listener != null)
                listener.onSyncCompleted(getImpl());
        }

        protected void onCancelled(Void result) {
            onSyncStoreFailed();
            syncRunning = false;

            if (listener != null)
                listener.onSyncFailed(getImpl(), cause);
        }
    }
}
