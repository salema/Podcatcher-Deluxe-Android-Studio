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

import java.util.List;

import retrofit.Call;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Retrofit service definition for the gpodder.net sync service.
 * Consult the <a href="http://http://gpoddernet.readthedocs.org">documentation</a> for details.
 */
interface GpodderService {

    String BASE_URL = "https://gpodder.net/";

    @POST("api/2/auth/{username}/login.json")
    Call<Boolean> login(@Path("username") String user);

    @GET("subscriptions/{username}/{deviceid}.json")
    Call<List<String>> getSubscriptions(@Path("username") String user, @Path("deviceid") String deviceId);


    @PUT("subscriptions/{username}/{deviceid}.json")
    Call<String> putSubscriptions(@Path("username") String user, @Path("deviceid") String deviceId,
                                  @Body List<String> feedUrls);

    @GET("api/2/episodes/{username}.json")
    Call<GetEpisodeActionResponse> getEpisodeActions(@Path("username") String user, @Query("podcast") String feedUrl,
                                                     @Query("device") String deviceId, @Query("since") Long timestamp,
                                                     @Query("aggregated") Boolean aggregated);

    @POST("api/2/episodes/{username}.json")
    Call<PostEpisodeActionResponse> postEpisodeActions(@Path("username") String user, @Body List<EpisodeAction> actions);
}
