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

import com.podcatcher.labs.sync.podcare.types.Item;
import com.podcatcher.labs.sync.podcare.types.Subscription;
import com.podcatcher.labs.sync.podcare.types.internal.ConnectResponse;
import com.podcatcher.labs.sync.podcare.types.internal.MessageResponse;

import java.util.List;

import retrofit.Call;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Query;

/**
 * Retrofit service definition for the Podcare sync service.
 * Consult the <a href="http://doc.pod.care">documentation</a> for details.
 */
interface PodcareService {

    String BASE_URL = "https://pod.care/api/v1/";

    @FormUrlEncoded
    @PUT("appconnection")
    Call<ConnectResponse> connect(@Header("authorization") String apiKey,
                                  @Header("connect-key") String connectKey,
                                  @Field("device-model") String modelName,
                                  @Field("device-type") String osType,
                                  @Field("device-os") String osVersion);

    @DELETE("appconnection")
    Call<MessageResponse> disconnect(@Header("authorization") String apiKey,
                                     @Header("client-secret") String connectId);

    @GET("subscriptions")
    Call<List<Subscription>> getSubscriptions(@Header("authorization") String apiKey,
                                              @Header("client-secret") String connectId);

    @FormUrlEncoded
    @POST("subscriptions")
    Call<MessageResponse> addSubscriptions(@Header("authorization") String apiKey,
                                           @Header("client-secret") String connectId,
                                           @Field("feeds") String feeds);

    @FormUrlEncoded
    @PUT("subscriptions")
    Call<MessageResponse> updateSubscription(@Header("authorization") String apiKey,
                                             @Header("client-secret") String connectId,
                                             @Field("feed") String feedUrl,
                                             @Field("state") Subscription.State state);

    @GET("episodes")
    Call<List<Item>> getAllEpisodes(@Header("authorization") String apiKey,
                                    @Header("client-secret") String connectId);

    @GET("filtered-episodes")
    Call<List<Item>> getUpdatedEpisodes(@Header("authorization") String apiKey,
                                        @Header("client-secret") String connectId,
                                        @Query("feed") String feedUrl);

    @GET("filtered-episodes")
    Call<List<Item>> getUpdatedEpisodes(@Header("authorization") String apiKey,
                                        @Header("client-secret") String connectId,
                                        @Query("changes-since") long timestamp);

    @FormUrlEncoded
    @PUT("episodes")
    Call<MessageResponse> updateEpisodes(@Header("authorization") String apiKey,
                                         @Header("client-secret") String connectId,
                                         @Field("episodes") String episodes);
}
