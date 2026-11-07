package com.teligati.app.network;

import com.teligati.app.network.model.*;
import retrofit2.Call;
import retrofit2.http.*;

public interface OpportunityApiService {

    // ==========================================
    // 1. Jobs & Assistant Jobs
    // ==========================================

    @GET("api/v1/jobs")
    Call<ApiResponse<PagedResponse<JobNetworkDto>>> getJobs(
            @Query("category") String category,
            @Query("status") String status,
            @Query("search") String search,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/jobs/{id}")
    Call<ApiResponse<JobNetworkDto>> getJobById(@Path("id") long id);

    @POST("api/v1/jobs")
    Call<ApiResponse<JobNetworkDto>> createJob(@Body JobCreateRequest request);

    @PUT("api/v1/jobs/{id}")
    Call<ApiResponse<JobNetworkDto>> updateJob(@Path("id") long id, @Body JobCreateRequest request);

    @DELETE("api/v1/jobs/{id}")
    Call<ApiResponse<Void>> deleteJob(@Path("id") long id);

    @GET("api/v1/assistant-jobs")
    Call<ApiResponse<PagedResponse<JobNetworkDto>>> getAssistantJobs(
            @Query("status") String status,
            @Query("search") String search,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @POST("api/v1/assistant-jobs")
    Call<ApiResponse<JobNetworkDto>> createAssistantJob(@Body JobCreateRequest request);

    // ==========================================
    // 2. Research Opportunities & Projects
    // ==========================================

    @GET("api/v1/research-opportunities")
    Call<ApiResponse<PagedResponse<ProjectNetworkDto>>> getResearchOpportunities(
            @Query("status") String status,
            @Query("type") String type,
            @Query("search") String search,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/research-opportunities/{id}")
    Call<ApiResponse<ProjectNetworkDto>> getResearchOpportunityById(@Path("id") long id);

    @POST("api/v1/research-opportunities")
    Call<ApiResponse<ProjectNetworkDto>> createResearchOpportunity(@Body ProjectCreateRequest request);

    @PUT("api/v1/research-opportunities/{id}")
    Call<ApiResponse<ProjectNetworkDto>> updateResearchOpportunity(@Path("id") long id, @Body ProjectCreateRequest request);

    @DELETE("api/v1/research-opportunities/{id}")
    Call<ApiResponse<Void>> deleteResearchOpportunity(@Path("id") long id);

    // ==========================================
    // 3. Research Proposals
    // ==========================================

    @POST("api/v1/research-opportunities/proposals")
    Call<ApiResponse<ResearchProposalNetworkDto>> submitProposal(@Body ResearchProposalCreateRequest request);

    @GET("api/v1/research-opportunities/proposals/me")
    Call<ApiResponse<PagedResponse<ResearchProposalNetworkDto>>> getMyProposals(
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/research-opportunities/proposals/{id}")
    Call<ApiResponse<ResearchProposalNetworkDto>> getProposalById(@Path("id") long id);

    @PATCH("api/v1/research-opportunities/proposals/{id}/decision")
    Call<ApiResponse<ResearchProposalNetworkDto>> decideProposal(
            @Path("id") long id,
            @Body ProposalDecisionRequest decision
    );

    // ==========================================
    // 4. Applications Domain
    // ==========================================

    @POST("api/v1/applications")
    Call<ApiResponse<ApplicationNetworkDto>> submitApplication(@Body ApplicationCreateRequest request);

    @GET("api/v1/applications/me")
    Call<ApiResponse<PagedResponse<ApplicationNetworkDto>>> getMyApplications(
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/applications/received")
    Call<ApiResponse<PagedResponse<ApplicationNetworkDto>>> getReceivedApplications(
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/applications/jobs/{jobId}")
    Call<ApiResponse<PagedResponse<ApplicationNetworkDto>>> getApplicationsForJob(
            @Path("jobId") long jobId,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/applications/projects/{projectId}")
    Call<ApiResponse<PagedResponse<ApplicationNetworkDto>>> getApplicationsForProject(
            @Path("projectId") long projectId,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/applications/{id}")
    Call<ApiResponse<ApplicationNetworkDto>> getApplicationById(@Path("id") long id);

    @PATCH("api/v1/applications/{id}/status")
    Call<ApiResponse<ApplicationNetworkDto>> updateApplicationStatus(
            @Path("id") long id,
            @Body ApplicationStatusRequest request
    );
}
