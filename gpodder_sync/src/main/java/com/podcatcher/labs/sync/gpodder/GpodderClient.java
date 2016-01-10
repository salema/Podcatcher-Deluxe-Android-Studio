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
package com.podcatcher.labs.sync.gpodder;

import com.podcatcher.labs.sync.gpodder.types.EpisodeAction;
import com.podcatcher.labs.sync.gpodder.types.Subscription;
import com.podcatcher.labs.sync.gpodder.types.internal.EpisodeActionResponse;
import com.podcatcher.labs.sync.gpodder.types.internal.TimestampResponse;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

import static android.util.Base64.NO_WRAP;
import static android.util.Base64.encodeToString;

/**
 * A communication wrapper for the gpodder.net service.
 *
 * <p>Consult the <a href="http://gpoddernet.readthedocs.org">documentation</a> for details.</p>
 */
public class GpodderClient {

    /**
     * String template for user credentials
     */
    private static final String CREDENTIALS_TEMPLATE = "%1$s:%2$s";
    /**
     * Basic HTTP authentication string template
     */
    private static final String AUTH_TEMPLATE = "Basic %1$s";

    /**
     * Our Retrofit service handle
     */
    private final GpodderService service;

    /**
     * The username used for queries
     */
    private final String username;
    /**
     * The auth to send with each request
     */
    private final String authorization;

    /**
     * Interceptor to add basic auth.
     */
    private class AuthInterceptor implements Interceptor {

        private static final String AUTHORIZATION = "Authorization";

        @Override
        public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
            final Request originalRequest = chain.request();
            final Request requestWithAuth = originalRequest.newBuilder()
                    .removeHeader(AUTHORIZATION)
                    .addHeader(AUTHORIZATION, authorization)
                    .build();
            return chain.proceed(requestWithAuth);
        }
    }

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
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public GpodderClient(@NonNull String username, @NonNull String password,
                         @Nullable String userAgent, boolean enableHttpLogging) {

        // Configure OkHttp client
        final OkHttpClient client = new OkHttpClient();
        if (enableHttpLogging) {
            final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            client.interceptors().add(interceptor);
        }

        this.userAgent = userAgent;
        if (userAgent != null)
            client.networkInterceptors().add(new UserAgentInterceptor());

        this.username = username;
        final String credentials = String.format(CREDENTIALS_TEMPLATE, username, password);
        this.authorization = String.format(AUTH_TEMPLATE, encodeToString(credentials.getBytes(), NO_WRAP));
        client.networkInterceptors().add(new AuthInterceptor());

        // Create service handle
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(GpodderService.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        this.service = retrofit.create(GpodderService.class);
    }

    /**
     * @return If gpodder.net was successfully contacted and the user credentials are valid.
     */
    public boolean authenticate() {
        try {
            return service.login(username).execute().isSuccess();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get all podcast subscriptions for all devices of the current user.
     *
     * @return The list of podcasts, might be empty.
     * @throws IOException On network problems.
     */
    @NonNull
    public Set<Subscription> getSubscriptions() throws IOException {
        final Response<Set<Subscription>> response = service.getSubscriptions(username).execute();

        return response.isSuccess() ? response.body() : new HashSet<Subscription>();
    }

    /**
     * Get all podcast subscriptions for given device.
     *
     * @param deviceId The device to check.
     * @return The list of podcast feed URLs gpodder has synced to the device.
     * @throws IOException On network problems.
     */
    @NonNull
    public Set<String> getSubscriptions(@NonNull String deviceId) throws IOException {
        final Response<Set<String>> response = service.getSubscriptions(username, deviceId).execute();

        return response.isSuccess() ? response.body() : new HashSet<String>();
    }

    /**
     * Tell gpodder.net to use given feed URL list as subscription list for given device.
     *
     * @param deviceId Device identifier to set podcast list for.
     * @param feedUrls The set of feed URLs.
     * @return If the gpodder accepted the list. Will return <code>true</code>
     * without even contacting the server if the list is empty.
     * @throws IOException On network problems.
     */
    public boolean putSubscriptions(@NonNull String deviceId, @NonNull Set<String> feedUrls) throws IOException {
        return feedUrls.isEmpty() || service.putSubscriptions(username, deviceId, feedUrls).execute().isSuccess();
    }

    /**
     * Tell gpodder.net about the changes that occured on the local list of subscriptions.
     *
     * @param deviceId  Device identifier to send changes for.
     * @param additions URLs of feeds that are new in the podcast list.
     * @param deletions URLs of feeds that have been removed from the podcast list.
     * @return If the gpodder accepted the lists. Will return <code>true</code>
     * without even contacting the server if both lists are empty.
     * @throws IOException On network problems.
     */
    public boolean putSubscriptionChanges(@NonNull String deviceId, @NonNull Set<String> additions,
                                          @NonNull Set<String> deletions) throws IOException {
        if (additions.isEmpty() && deletions.isEmpty())
            return true;
        else {
            final Map<String, Set<String>> changes = new HashMap<>();
            changes.put("add", additions);
            changes.put("remove", deletions);

            return service.putSubscriptionChanges(username, deviceId, changes).execute().isSuccess();
        }
    }

    /**
     * Pull episode actions from the gpodder server.
     *
     * @param actions (Empty) list to place actions in.
     *                Any actions placed will be sorted first (oldest) to latest (newest).
     * @param since   The time stamp (in millis since 1.1.1970 00:00:00 UTC) after which to look
     *                for episode actions. Give zero for all changes.
     * @return The time stamp send in return by the server (in millis since 1.1.1970 00:00:00 UTC)
     * or -1 if the HTTP status code indicates failure.
     * @throws IOException On network problems.
     */
    public long getEpisodeActions(@NonNull List<EpisodeAction> actions, long since) throws IOException {
        final Response<EpisodeActionResponse> response =
                service.getEpisodeActions(username, null, null, since, null).execute();

        if (response.isSuccess()) {
            actions.addAll(response.body().getActions());

            // Gpodder sends results with latest first, we want to have this the other way around
            final DateFormat formatter = getTimeStampFormatter();
            Collections.reverse(actions);
            Collections.sort(actions, new Comparator<EpisodeAction>() {

                @Override
                public int compare(EpisodeAction lhs, EpisodeAction rhs) {
                    try {
                        return formatter.parse(lhs.getTimestamp()).compareTo(formatter.parse(rhs.getTimestamp()));
                    } catch (ParseException | NullPointerException e) {
                        return 0;
                    }
                }
            });

            return response.body().getTimestamp();
        } else
            return -1;
    }

    /**
     * Upload episode actions that already occurred locally to the gpodder.net server.
     *
     * @param actions The list of actions to upload.
     * @return The time stamp send in return by the server (in millis since 1.1.1970 00:00:00 UTC)
     * or -1 if the action list was not accepted (i.e. the HTTP status code indicates failure).
     * @throws IOException On network problems.
     */
    public long putEpisodeActions(@NonNull List<EpisodeAction> actions) throws IOException {
        final Response<TimestampResponse> response =
                service.postEpisodeActions(username, actions).execute();

        return response.isSuccess() ? response.body().getTimestamp() : -1;
    }

    /**
     * @return A date formatter that can be used to create time stamps
     * understood by gpodder.net
     * @see EpisodeAction#getTimestamp()
     */
    @NonNull
    public DateFormat getTimeStampFormatter() {
        final TimeZone zone = TimeZone.getTimeZone("UTC");
        final DateFormat format = new SimpleDateFormat(GpodderService.TIME_STAMP_FORMAT, Locale.US);
        format.setTimeZone(zone);

        return format;
    }
}
