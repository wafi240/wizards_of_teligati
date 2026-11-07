package com.teligati.app.network;

import com.teligati.app.network.model.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface ReferenceApiService {

    @POST("api/v1/references")
    Call<ApiResponse<ReferenceNetworkDto>> createRequest(@Body ReferenceCreateRequest request);

    @GET("api/v1/references/sent")
    Call<ApiResponse<PagedResponse<ReferenceNetworkDto>>> getSentRequests(
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/references/incoming")
    Call<ApiResponse<PagedResponse<ReferenceNetworkDto>>> getIncomingRequests(
            @Query("status") String status,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/references/{id}")
    Call<ApiResponse<ReferenceNetworkDto>> getRequestById(@Path("id") long id);

    @PATCH("api/v1/references/{id}/decision")
    Call<ApiResponse<ReferenceNetworkDto>> decideRequest(
            @Path("id") long id,
            @Body ReferenceDecisionNetworkRequest request
    );

    @PATCH("api/v1/references/{id}/recommendation")
    Call<ApiResponse<ReferenceNetworkDto>> completeRecommendation(
            @Path("id") long id,
            @Body RecommendationNetworkRequest request
    );

    @PATCH("api/v1/references/{id}/cancel")
    Call<ApiResponse<ReferenceNetworkDto>> cancelRequest(@Path("id") long id);
}
