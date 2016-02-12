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

import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit service definition for the gpodder.net sync service.
 * Consult the <a href="http://gpoddernet.readthedocs.org">documentation</a> for details.
 */
interface GpodderService {

    String BASE_URL = "https://gpodder.net/";

    String TIME_STAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @POST("api/2/auth/{username}/login.json")
    Call<Void> login(@Path("username") String user);

    @GET("subscriptions/{username}.json")
    Call<Set<Subscription>> getSubscriptions(@Path("username") String user);

    @GET("subscriptions/{username}/{deviceid}.json")
    Call<Set<String>> getSubscriptions(@Path("username") String user, @Path("deviceid") String deviceId);


    @PUT("subscriptions/{username}/{deviceid}.json")
    Call<Void> putSubscriptions(@Path("username") String user, @Path("deviceid") String deviceId,
                                  @Body Set<String> feedUrls);

    @POST("api/2/subscriptions/{username}/{deviceid}.json")
    Call<TimestampResponse> putSubscriptionChanges(@Path("username") String user, @Path("deviceid") String deviceId,
                                                   @Body Map<String, Set<String>> changes);

    @GET("api/2/episodes/{username}.json")
    Call<EpisodeActionResponse> getEpisodeActions(@Path("username") String user, @Query("podcast") String feedUrl,
                                                     @Query("device") String deviceId, @Query("since") Long timestamp,
                                                     @Query("aggregated") Boolean aggregated);

    @POST("api/2/episodes/{username}.json")
    Call<TimestampResponse> postEpisodeActions(@Path("username") String user, @Body List<EpisodeAction> actions);
}
