package com.dragontek.mygpoclient.api;

import android.util.Log;

import com.dragontek.mygpoclient.simple.SimpleClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This is the API client that implements both the Simple and Advanced API of
 * gpodder.net. See the {@link SimpleClient} class for a smaller class that only
 * implements the Simple API.
 *
 * @author jmondragon
 * @version 2.0
 * @see SimpleClient
 */
public class MygPodderClient extends SimpleClient {
    /**
     * This is the API client that implements both the Simple and Advanced API
     * of gpodder.net. See the {@link SimpleClient} class for a smaller class
     * that only implements the Simple API.
     *
     * @param username
     * @param password
     * @param hostname
     */
    public MygPodderClient(String username, String password, String hostname) {
        super(username, password, hostname);
    }

    public MygPodderClient(String username, String password) {
        super(username, password);
    }

    public MygPodderClient(String username) {
        super(username);
    }

    public Set<String> getSubscriptions(PodcastDevice device)
            throws IOException, AuthenticationException {
        return super.getSubscriptions(device.id);
    }

    public boolean putSubscriptions(PodcastDevice device, List<String> urls)
            throws IOException, AuthenticationException {
        return super.putSubscriptions(device.id, urls);
    }

    /**
     * Update the subscription list for a given device.
     * <p/>
     * Returns a {@link UpdateResult} object that contains a list of (sanitized)
     * URLs and a "since" value that can be used for future calls to.
     * <p/>
     * For every (old_url, new_url) tuple in the updated_urls list of the
     * resulting object, the client should rewrite the URL in its subscription
     * list so that new_url is used instead of old_url.
     *
     * @param deviceId the id of the device to be updated
     * @param add      a set of urls to be added to the device
     * @param remove   a set of urls to be removed from the device
     * @return a {@link UpdateResult} object that contains a list of (sanitized)
     * URLs and a "since" value that can be used for future calls to
     * {@link #pullSubscriptions(String, long)}.
     * @throws IOException
     */
    public UpdateResult updateSubscriptions(String deviceId, Set<String> add,
                                            Set<String> remove) throws IOException {
        String uri = _locator.addRemoveSubscriptionsUri(deviceId);
        SubscriptionChanges changes = new SubscriptionChanges(add, remove);
        StringEntity data = new StringEntity(_gson.toJson(changes));
        return _gson.fromJson(_client.POST(uri, data), UpdateResult.class);
    }

    /**
     * Downloads subscriptions since the time of the last update
     *
     * @param deviceId the id of the device to be updated
     * @param since    should be a timestamp in seconds that has been retrieved
     *                 previously by a call to {@link #updateSubscriptions} or
     *                 {@link #pullSubscriptions}.
     * @return a {@link SubscriptionChanges} object with two lists (one for
     * added and one for removed podcast URLs) and a "since" value that
     * can be used for future calls to this method.
     * @throws IOException
     * @throws AuthenticationException
     */
    public SubscriptionChanges pullSubscriptions(String deviceId, long since)
            throws IOException, AuthenticationException {
        String uri = _locator.subscriptionUpdatesUri(deviceId, since);
        try {
            return _gson.fromJson(_client.GET(uri), SubscriptionChanges.class);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                throw e;
        }

    }

    /**
     * Uploads a {@link List} of {@link EpisodeAction} objects to the server
     *
     * @param actions a {@link List} of {@link EpisodeAction} objects
     * @return a timestamp that can be used for retrieving changes.
     * @throws IOException
     * @throws AuthenticationException
     */
    public long uploadEpisodeActions(List<EpisodeAction> actions)
            throws IOException, AuthenticationException {
        JsonParser parser = new JsonParser();
        StringEntity data = new StringEntity(_gson.toJson(actions));
        try {
            JsonObject response = (JsonObject) parser.parse(_client.POST(
                    _locator.uploadEpisodeActionsUri(), data));

            JsonArray updates = response.getAsJsonArray("update_urls");
            Iterator<JsonElement> it = updates.iterator();
            while (it.hasNext()) {
                JsonArray element = it.next().getAsJsonArray();

                Log.d("UPDATE_URLS", element.get(0) + " to " + element.get(1));
            }

            return response.get("timestamp").getAsLong();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                throw e;
        }
    }

    /**
     * Downloads a {@link List} of {@link EpisodeAction} objects from the server
     *
     * @param since
     * @param podcast
     * @param deviceId
     * @return a {@link EpisodeActionChanges} object with the list of new
     * actions and a "since" timestamp that can be used for future calls
     * to this method when retrieving episodes.
     * @throws IOException
     * @throws AuthenticationException if the user is not authenticated
     */
    public EpisodeActionChanges downloadEpisodeActions(long since,
                                                       String podcast, String deviceId) throws IOException,
            AuthenticationException {
        try {
            return _gson.fromJson(_client.GET(_locator
                            .downloadEpisodeActionsUri(since, podcast, deviceId)),
                    EpisodeActionChanges.class
            );
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                throw e;
        }
    }

    public EpisodeActionChanges downloadEpisodeActions(long since,
                                                       String deviceId) throws IOException, AuthenticationException {
        return downloadEpisodeActions(since, null, deviceId);
    }

    public EpisodeActionChanges downloadEpisodeActions(long since)
            throws IOException, AuthenticationException {
        return downloadEpisodeActions(since, null, null);
    }

    /**
     * Update the description of a device on the server
     * <p/>
     * This changes the caption and/or type of a given device on the server. If
     * the device does not exist, it is created with the given settings. The
     * parameters caption and type are both optional and when set to a value
     * other than None will be used to update the device settings.
     *
     * @param deviceId
     * @param caption
     * @param type
     * @return true if the request succeeded, false otherwise.
     * @throws IOException
     * @throws AuthenticationException
     */
    public boolean updateDeviceSettings(String deviceId, String caption,
                                        String type) throws IOException, AuthenticationException {
        PodcastDevice device = new PodcastDevice(deviceId, caption, type);
        StringEntity data = new StringEntity(_gson.toJson(device), "UTF-8");
        try {
            _client.POST(_locator.deviceSettingsUri(deviceId), data);
            return true;
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                return false;
        }
    }

    /**
     * Returns a {@link List} of this user's {@link PodcastDevice} objects
     * <p/>
     * The resulting list can be used to display a selection list to the user or
     * to determine device IDs to pull the subscription list from.
     *
     * @return a {@link List} of this user's {@link PodcastDevice} objects
     * @throws IOException
     * @throws AuthenticationException
     */
    public List<PodcastDevice> getDevices() throws IOException,
            AuthenticationException {
        String uri = _locator.deviceListUri();
        Type collectionType = new TypeToken<ArrayList<PodcastDevice>>() {
        }.getType();
        try {
            return _gson.fromJson(_client.GET(uri), collectionType);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                throw e;
        }
    }

    public DeviceSync getDeviceSync() throws IOException, AuthenticationException {
        String uri = _locator.deviceSynchronizationUri();
        try {
            return _gson.fromJson(_client.GET(uri), DeviceSync.class);
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 401)
                throw new AuthenticationException(
                        "Unable to authenticate user with Gpodder.net", e);
            else
                throw e;
        }

    }
}
