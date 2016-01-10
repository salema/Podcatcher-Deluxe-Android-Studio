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
package com.podcatcher.labs.sync.podcare;

import com.podcatcher.labs.sync.podcare.callbacks.OnConnectListener;
import com.podcatcher.labs.sync.podcare.callbacks.OnFailedListener;
import com.podcatcher.labs.sync.podcare.callbacks.OnGetEpisodesListener;
import com.podcatcher.labs.sync.podcare.callbacks.OnGetSubscriptionsListener;
import com.podcatcher.labs.sync.podcare.types.Item;
import com.podcatcher.labs.sync.podcare.types.Subscription;
import com.podcatcher.labs.sync.podcare.types.internal.ConnectResponse;
import com.podcatcher.labs.sync.podcare.types.internal.MessageResponse;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.List;

import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

import static android.os.Build.MODEL;
import static android.os.Build.VERSION.RELEASE;

/**
 * A communication wrapper for the Podcare service.
 *
 * <p>Use your API key to create an instance of the client. Then, arrange to receive
 * the connect key from Podcare (for example, via QR code scan). Use the key to connect
 * to the service, resulting in your connect id. This connect id is permanent and
 * needs to be supplied for every request made.</p>
 *
 * <p>Consult the <a href="http://doc.pod.care">Podcare documentation</a> for details.</p>
 */
public class PodcareClient {

    /**
     * Bearer string prefix
     */
    private static final String BEARER = "Bearer ";
    /**
     * Android OS name
     */
    private static final String ANDROID = "android";

    /**
     * Our Retrofit service handle
     */
    private final PodcareService service;
    /**
     * Our GSON object used to stringify json objects and lists
     */
    private final Gson gson = new Gson();

    /**
     * The Podcare API key
     */
    private final String apiKey;
    /**
     * The user agent string send with each request
     */
    private final String userAgent;

    /**
     * Interceptor to add custom User-Agent.
     */
    private class UserAgentInterceptor implements Interceptor {

        private static final String USER_AGENT = "User-Agent";

        @Override
        public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
            final Request originalRequest = chain.request();
            final Request requestWithUserAgent = originalRequest.newBuilder()
                    .removeHeader(USER_AGENT)
                    .addHeader(USER_AGENT, userAgent)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }

    /**
     * Create new service wrapper.
     *
     * @param apiKey Podcare API key used for all subsequent communication with the
     *               Podcare service. It is okay to give the key only (without bearer prefix).
     */
    public PodcareClient(@NonNull String apiKey) {
        this(apiKey, null, false);
    }

    /**
     * Create new service wrapper.
     *
     * @param apiKey    Podcare API key used for all subsequent communication with the
     *                  Podcare service. It is okay to give the key only (without bearer prefix).
     * @param userAgent User-Agent string to use with every request, give <code>null</code>
     *                  for default.
     */
    public PodcareClient(@NonNull String apiKey, @Nullable String userAgent) {
        this(apiKey, userAgent, false);
    }

    /**
     * Create new service wrapper.
     *
     * @param apiKey            Podcare API key used for all subsequent communication with the
     *                          Podcare service. It is okay to give the key only (without bearer prefix).
     * @param userAgent         User-Agent string to use with every request, give <code>null</code>
     *                          for default.
     * @param enableHttpLogging Whether the underlying okhttp client should log all details.
     */
    public PodcareClient(@NonNull String apiKey, @Nullable String userAgent, boolean enableHttpLogging) {
        // Make sure the API key has the correct format
        if (!apiKey.startsWith(BEARER))
            apiKey = BEARER + apiKey;

        this.apiKey = apiKey;
        this.userAgent = userAgent;

        // Configure OkHttp client
        final OkHttpClient client = new OkHttpClient();
        if (enableHttpLogging) {
            final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            client.interceptors().add(interceptor);
        }

        if (userAgent != null)
            client.networkInterceptors().add(new UserAgentInterceptor());

        // Create service handle
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PodcareService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        this.service = retrofit.create(PodcareService.class);
    }

    /**
     * Connect to the Podcare service using the connect key given. The key is usually
     * scanned from a QR code and is <em>not</em> the same as the connect id used for
     * all other requests.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectKey The key received from the Podcare service out of band.
     * @return The connect id assigned by the Podcare service, or <code>null</code> if failed.
     * @throws IOException On network problems.
     * @see #connectAsync(String, OnConnectListener)
     */
    @Nullable
    public String connect(@NonNull String connectKey) throws IOException {
        final Response<ConnectResponse> response =
                service.connect(apiKey, connectKey, MODEL, ANDROID, RELEASE).execute();

        return response.isSuccess() ? response.body().getConnectId() : null;
    }

    /**
     * Connect to the Podcare service using the connect key given. The key is usually
     * scanned from a QR code and is <em>not</em> the same as the connect id used for
     * all other requests.
     *
     * This method will return immediately.
     *
     * @param connectKey The key received from the Podcare service out of band.
     * @param listener   Call-back to alert when the response comes in.
     * @see #connect(String)
     */
    public void connectAsync(@NonNull String connectKey, @NonNull final OnConnectListener listener) {
        service.connect(apiKey, connectKey, MODEL, ANDROID, RELEASE).enqueue(new Callback<ConnectResponse>() {

            @Override
            public void onResponse(Response<ConnectResponse> response, Retrofit retrofit) {
                if (response.isSuccess())
                    listener.onConnect(response.body().getConnectId());
                else
                    listener.onConnectFailed(new PodcareException(response));
            }

            @Override
            public void onFailure(Throwable error) {
                listener.onConnectFailed(new PodcareException(error));
            }
        });
    }

    /**
     * Unlink application and Podcare service.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectId Connection to revoke.
     * @return If the connection was revoked successfully.
     * @throws IOException On network problems.
     * @see #disconnectAsync(String, OnFailedListener)
     */
    public boolean disconnect(@NonNull String connectId) throws IOException {
        return service.disconnect(apiKey, connectId).execute().isSuccess();
    }

    /**
     * Unlink application and Podcare service.
     *
     * This method will return immediately.
     *
     * @param connectId Connection to revoke.
     * @param listener  Call-back to alert when the response comes in.
     *                  Give <code>null</code> for "don't care".
     * @see #disconnect(String)
     */
    public void disconnectAsync(@NonNull String connectId, @Nullable final OnFailedListener listener) {
        service.disconnect(apiKey, connectId).enqueue(new Callback<MessageResponse>() {

            @Override
            public void onResponse(Response<MessageResponse> response, Retrofit retrofit) {
                if (response.isSuccess() && listener != null)
                    listener.onSuccess();
                else if (listener != null)
                    listener.onRequestFailed(new PodcareException(response));
            }

            @Override
            public void onFailure(Throwable error) {
                if (listener != null)
                    listener.onRequestFailed(new PodcareException(error));
            }
        });
    }

    /**
     * Get list of podcasts for this connection from Podcare.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectId Connection identifier.
     * @return The list of podcast, possibly empty or <code>null</code> on failure.
     * @throws IOException On network problems.
     * @see #getSubscriptionsAsync(String, OnGetSubscriptionsListener)
     */
    @Nullable
    public List<Subscription> getSubscriptions(@NonNull String connectId) throws IOException {
        return service.getSubscriptions(apiKey, connectId).execute().body();
    }

    /**
     * Get list of podcasts for this connection from Podcare.
     *
     * This method will return immediately.
     *
     * @param connectId Connection identifier.
     * @param listener  Call-back to alert when the response comes in.
     * @see #getSubscriptions(String)
     */
    public void getSubscriptionsAsync(@NonNull String connectId, @NonNull final OnGetSubscriptionsListener listener) {
        service.getSubscriptions(apiKey, connectId).enqueue(new Callback<List<Subscription>>() {

            @Override
            public void onResponse(Response<List<Subscription>> response, Retrofit retrofit) {
                if (response.isSuccess())
                    listener.onGetSubscriptions(response.body());
                else
                    listener.onGetSubscriptionsFailed(new PodcareException(response));
            }

            @Override
            public void onFailure(Throwable error) {
                listener.onGetSubscriptionsFailed(new PodcareException(error));
            }
        });
    }

    /**
     * Post updated podcast list to Podcare.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectId Connection identifier.
     * @param feeds     List of subscriptions to send, should not be empty.
     * @return If the list was posted successfully.
     * @throws IOException On network problems.
     * @see #addSubscriptionsAsync(String, List, OnFailedListener)
     */
    public boolean addSubscriptions(@NonNull String connectId, @NonNull List<Subscription> feeds) throws IOException {
        return service.addSubscriptions(apiKey, connectId, gson.toJson(feeds)).execute().isSuccess();
    }

    /**
     * Post updated podcast list to Podcare.
     *
     * This method will return immediately.
     *
     * @param connectId Connection identifier.
     * @param feeds     List of subscriptions to send, should not be empty.
     * @param listener  Call-back to alert when the response comes in.
     *                  Give <code>null</code> for "don't care".
     * @see #addSubscriptions(String, List)
     */
    public void addSubscriptionsAsync(@NonNull String connectId,
                                      @NonNull List<Subscription> feeds, @Nullable final OnFailedListener listener) {
        service.addSubscriptions(apiKey, connectId, gson.toJson(feeds)).enqueue(new Callback<MessageResponse>() {

            @Override
            public void onResponse(Response<MessageResponse> response, Retrofit retrofit) {
                if (response.isSuccess() && listener != null)
                    listener.onSuccess();
                else if (listener != null)
                    listener.onRequestFailed(new PodcareException(response));
            }

            @Override
            public void onFailure(Throwable error) {
                if (listener != null)
                    listener.onRequestFailed(new PodcareException(error));
            }
        });
    }

    /**
     * Change the state of a podcast on Podcare.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectId Connection identifier.
     * @param feed      Podcast to update (incl. its new state).
     * @return If the state was updated successfully.
     * @throws IOException On network problems.
     * @see #updateSubscriptionAsync(String, Subscription, OnFailedListener)
     * @see com.podcatcher.labs.sync.podcare.types.Subscription.State
     */
    public boolean updateSubscription(@NonNull String connectId, @NonNull Subscription feed) throws IOException {
        return service.updateSubscription(apiKey, connectId, feed.getFeed(), feed.getState()).execute().isSuccess();
    }

    /**
     * Change the state of a podcast on Podcare.
     *
     * This method will return immediately.
     *
     * @param connectId Connection identifier.
     * @param feed      Podcast to update (incl. its new state).
     * @param listener  Call-back to alert when the response comes in.
     *                  Give <code>null</code> for "don't care".
     * @see #updateSubscription(String, Subscription)
     * @see com.podcatcher.labs.sync.podcare.types.Subscription.State
     */
    public void updateSubscriptionAsync(@NonNull String connectId, @NonNull Subscription feed, @Nullable final OnFailedListener listener) {
        service.updateSubscription(apiKey, connectId, feed.getFeed(), feed.getState()).enqueue(new Callback<MessageResponse>() {

            @Override
            public void onResponse(Response<MessageResponse> response, Retrofit retrofit) {
                if (response.isSuccess() && listener != null)
                    listener.onSuccess();
                else if (listener != null)
                    listener.onRequestFailed(new PodcareException(response));
            }

            @Override
            public void onFailure(Throwable error) {
                if (listener != null)
                    listener.onRequestFailed(new PodcareException(error));
            }
        });
    }

    /**
     * Get <em>all</em> episode data present on Podcare for this connection.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectId Connection identifier.
     * @return The list of episodes, possibly empty or <code>null</code> on failure.
     * @throws IOException On network problems.
     * @see #getUpdatedEpisodesAsync(String, OnGetEpisodesListener)
     */
    @Nullable
    public List<Item> getUpdatedEpisodes(@NonNull String connectId) throws IOException {
        return service.getUpdatedEpisodes(apiKey, connectId, 0).execute().body();
    }

    /**
     * Get <em>all</em> episode data present on Podcare for this connection.
     *
     * This method will return immediately.
     *
     * @param connectId Connection identifier.
     * @param listener  Call-back to alert when the response comes in.
     * @see #getUpdatedEpisodes(String)
     */
    public void getUpdatedEpisodesAsync(@NonNull String connectId, @NonNull final OnGetEpisodesListener listener) {
        getUpdatedEpisodesAsync(connectId, 0, listener);
    }

    /**
     * Get all episode data present on Podcare for a specific podcast given by URL.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectId Connection identifier.
     * @param feedUrl   The subscription's URL.
     * @return The list of episodes, possibly empty or <code>null</code> on failure.
     * @throws IOException On network problems.
     * @see #getUpdatedEpisodesAsync(String, String, OnGetEpisodesListener) (String, String)
     */
    @Nullable
    public List<Item> getUpdatedEpisodes(@NonNull String connectId, @NonNull String feedUrl) throws IOException {
        return service.getUpdatedEpisodes(apiKey, connectId, feedUrl).execute().body();
    }

    /**
     * Get all episode data present on Podcare for a specific podcast given by URL.
     *
     * This method will return immediately.
     *
     * @param connectId Connection identifier.
     * @param feedUrl   The subscription's URL.
     * @param listener  Call-back to alert when the response comes in.
     * @see #getUpdatedEpisodes(String, String)
     */
    public void getUpdatedEpisodesAsync(@NonNull String connectId, @NonNull String feedUrl, @NonNull final OnGetEpisodesListener listener) {
        service.getUpdatedEpisodes(apiKey, connectId, feedUrl).enqueue(new Callback<List<Item>>() {

            @Override
            public void onResponse(Response<List<Item>> response, Retrofit retrofit) {
                if (response.isSuccess())
                    listener.onGetEpisodes(response.body());
                else
                    listener.onGetEpisodesFailed(new PodcareException(response));
            }

            @Override
            public void onFailure(Throwable error) {
                listener.onGetEpisodesFailed(new PodcareException(error));
            }
        });
    }

    /**
     * Get all episode data present on Podcare that came in after some point in time.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectId Connection identifier.
     * @param timestamp In seconds (not millis!) after 1.1.1970 00:00 UTC
     * @return The list of episodes, possibly empty or <code>null</code> on failure.
     * @throws IOException On network problems.
     * @see #getUpdatedEpisodesAsync(String, long, OnGetEpisodesListener)
     */
    @Nullable
    public List<Item> getUpdatedEpisodes(@NonNull String connectId, long timestamp) throws IOException {
        return service.getUpdatedEpisodes(apiKey, connectId, timestamp).execute().body();
    }

    /**
     * Get all episode data present on Podcare that came in after some point in time.
     *
     * This method will return immediately.
     *
     * @param connectId Connection identifier.
     * @param timestamp In seconds (not millis!) after 1.1.1970 00:00 UTC
     * @param listener  Call-back to alert when the response comes in.
     * @see #getUpdatedEpisodes(String, long)
     */
    public void getUpdatedEpisodesAsync(@NonNull String connectId, long timestamp, @NonNull final OnGetEpisodesListener listener) {
        service.getUpdatedEpisodes(apiKey, connectId, timestamp).enqueue(new Callback<List<Item>>() {

            @Override
            public void onResponse(Response<List<Item>> response, Retrofit retrofit) {
                if (response.isSuccess())
                    listener.onGetEpisodes(response.body());
                else
                    listener.onGetEpisodesFailed(new PodcareException(response));
            }

            @Override
            public void onFailure(Throwable error) {
                listener.onGetEpisodesFailed(new PodcareException(error));
            }
        });
    }

    /**
     * Send episode (change) information to Podcare.
     *
     * This method will block until the response from Podcare is received.
     *
     * @param connectId Connection identifier.
     * @param episodes  Episode data to send, should not be empty. All items need to
     *                  have all fields set.
     * @return If the episode data was received.
     * @throws IOException On network problems.
     * @see #updateEpisodesAsync(String, List, OnFailedListener)
     * @see com.podcatcher.labs.sync.podcare.types.Item
     */
    public boolean updateEpisodes(@NonNull String connectId, @NonNull List<Item> episodes) throws IOException {
        return service.updateEpisodes(apiKey, connectId, gson.toJson(episodes)).execute().isSuccess();
    }

    /**
     * Send episode (change) information to Podcare.
     *
     * This method will return immediately.
     *
     * @param connectId Connection identifier.
     * @param episodes  Episode data to send, should not be empty. All items need to
     *                  have all fields set.
     * @param listener  Call-back to alert when the response comes in.
     *                  Give <code>null</code> for "don't care".
     * @see #updateEpisodes(String, List)
     * @see com.podcatcher.labs.sync.podcare.types.Item
     */
    public void updateEpisodesAsync(@NonNull String connectId, @NonNull List<Item> episodes, @Nullable final OnFailedListener listener) {
        service.updateEpisodes(apiKey, connectId, gson.toJson(episodes)).enqueue(new Callback<MessageResponse>() {

            @Override
            public void onResponse(Response<MessageResponse> response, Retrofit retrofit) {
                if (response.isSuccess() && listener != null)
                    listener.onSuccess();
                else if (listener != null)
                    listener.onRequestFailed(new PodcareException(response));
            }

            @Override
            public void onFailure(Throwable error) {
                if (listener != null)
                    listener.onRequestFailed(new PodcareException(error));
            }
        });
    }
}
