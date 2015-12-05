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
package com.podcatcher.labs.sync.gpodder;

import com.podcatcher.labs.sync.gpodder.types.EpisodeAction;
import com.podcatcher.labs.sync.gpodder.types.internal.GetEpisodeActionResponse;
import com.podcatcher.labs.sync.gpodder.types.internal.PostEpisodeActionResponse;

import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

import static android.util.Base64.NO_WRAP;
import static android.util.Base64.encodeToString;

/**
 * A communication wrapper for the gpodder.net service.
 *
 * <p>Consult the <a href="http://http://gpoddernet.readthedocs.org">documentation</a> for details.</p>
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

    @NonNull
    public List<String> getSubscriptions(@NonNull String deviceId) throws IOException {
        final Response<List<String>> response = service.getSubscriptions(username, deviceId).execute();

        return response.isSuccess() ? response.body() : new ArrayList<String>();
    }

    public boolean putSubscriptions(@NonNull String deviceId, @NonNull List<String> feedUrls) throws IOException {
        return service.putSubscriptions(username, deviceId, feedUrls).execute().isSuccess();
    }

    @NonNull
    public List<EpisodeAction> getEpisodeActions(long since) throws IOException {
        final Response<GetEpisodeActionResponse> response =
                service.getEpisodeActions(username, null, null, since, null).execute();

        if (response.isSuccess()) {
            // Gpodder sends results with latest first, we want to have this the other way around
            final List<EpisodeAction> actions = response.body().getActions();
            Collections.reverse(actions);

            return actions;
        } else
            return new ArrayList<>();
    }

    public long putEpisodeActions(@NonNull List<EpisodeAction> actions) throws IOException {
        final Response<PostEpisodeActionResponse> response =
                service.postEpisodeActions(username, actions).execute();

        return response.isSuccess() ? response.body().getTimestamp() : -1;
    }
}
