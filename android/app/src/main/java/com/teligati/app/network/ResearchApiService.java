package com.teligati.app.network;

import com.teligati.app.network.model.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface ResearchApiService {

    // ==========================================
    // 1. Research Opportunities
    // ==========================================

    @GET("api/v1/research/opportunities")
    Call<ApiResponse<PagedResponse<ProjectNetworkDto>>> getOpportunities(
            @Query("category") String category,
            @Query("status") String status,
            @Query("search") String search,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/research/opportunities/{id}")
    Call<ApiResponse<ProjectNetworkDto>> getOpportunityById(@Path("id") long id);

    @POST("api/v1/research/opportunities")
    Call<ApiResponse<ProjectNetworkDto>> createOpportunity(@Body ProjectCreateRequest request);

    @PUT("api/v1/research/opportunities/{id}")
    Call<ApiResponse<ProjectNetworkDto>> updateOpportunity(@Path("id") long id, @Body ProjectCreateRequest request);

    @DELETE("api/v1/research/opportunities/{id}")
    Call<ApiResponse<Void>> deleteOpportunity(@Path("id") long id);

    @PATCH("api/v1/research/opportunities/{id}/close")
    Call<ApiResponse<ProjectNetworkDto>> closeOpportunity(@Path("id") long id);

    // ==========================================
    // 2. Research Applications
    // ==========================================

    @POST("api/v1/research/opportunities/{id}/applications")
    Call<ApiResponse<ApplicationNetworkDto>> submitApplication(
            @Path("id") long opportunityId,
            @Body ApplicationCreateRequest request
    );

    @GET("api/v1/research/applications/me")
    Call<ApiResponse<PagedResponse<ApplicationNetworkDto>>> getMyApplications(
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/research/opportunities/{id}/applications")
    Call<ApiResponse<PagedResponse<ApplicationNetworkDto>>> getApplicationsForOpportunity(
            @Path("id") long opportunityId,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/research/applications/{id}")
    Call<ApiResponse<ApplicationNetworkDto>> getApplicationById(@Path("id") long id);

    @PATCH("api/v1/research/applications/{id}/review")
    Call<ApiResponse<ApplicationNetworkDto>> reviewApplication(
            @Path("id") long id,
            @Body ApplicationStatusRequest request
    );

    @PATCH("api/v1/research/applications/{id}/withdraw")
    Call<ApiResponse<ApplicationNetworkDto>> withdrawApplication(@Path("id") long id);

    @PATCH("api/v1/research/applications/{id}/resubmit")
    Call<ApiResponse<ApplicationNetworkDto>> resubmitApplication(
            @Path("id") long id,
            @Body ApplicationCreateRequest request
    );

    // ==========================================
    // 3. Research Papers
    // ==========================================

    @GET("api/v1/research/papers")
    Call<ApiResponse<PagedResponse<ResearchPaperNetworkDto>>> getPapers(
            @Query("field") String field,
            @Query("search") String search,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/research/papers/{id}")
    Call<ApiResponse<ResearchPaperNetworkDto>> getPaperById(@Path("id") long id);

    @POST("api/v1/research/papers")
    Call<ApiResponse<ResearchPaperNetworkDto>> publishPaper(@Body ResearchPaperCreateRequest request);

    @PUT("api/v1/research/papers/{id}")
    Call<ApiResponse<ResearchPaperNetworkDto>> updatePaper(@Path("id") long id, @Body ResearchPaperCreateRequest request);

    @DELETE("api/v1/research/papers/{id}")
    Call<ApiResponse<Void>> deletePaper(@Path("id") long id);
}
