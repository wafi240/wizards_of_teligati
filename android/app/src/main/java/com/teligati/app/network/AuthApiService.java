package com.teligati.app.network;

import com.teligati.app.network.model.AlumniRegisterRequest;
import com.teligati.app.network.model.ApiResponse;
import com.teligati.app.network.model.FacultyRegisterRequest;
import com.teligati.app.network.model.LoginRequest;
import com.teligati.app.network.model.RefreshTokenRequest;
import com.teligati.app.network.model.StudentRegisterRequest;
import com.teligati.app.network.model.TokenResponse;
import com.teligati.app.network.model.UserSummaryResponse;
import com.teligati.app.network.model.VerificationStatusResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApiService {

    @POST("api/v1/auth/register/student")
    Call<ApiResponse<TokenResponse>> registerStudent(@Body StudentRegisterRequest request);

    @POST("api/v1/auth/register/alumni")
    Call<ApiResponse<TokenResponse>> registerAlumni(@Body AlumniRegisterRequest request);

    @POST("api/v1/auth/register/faculty")
    Call<ApiResponse<TokenResponse>> registerFaculty(@Body FacultyRegisterRequest request);

    @POST("api/v1/auth/login")
    Call<ApiResponse<TokenResponse>> login(@Body LoginRequest request);

    @POST("api/v1/auth/refresh")
    Call<ApiResponse<TokenResponse>> refreshToken(@Body RefreshTokenRequest request);

    @POST("api/v1/auth/logout")
    Call<ApiResponse<Void>> logout(@Body RefreshTokenRequest request);

    @GET("api/v1/auth/me")
    Call<ApiResponse<UserSummaryResponse>> getCurrentUser();

    @GET("api/v1/auth/verification-status")
    Call<ApiResponse<VerificationStatusResponse>> getVerificationStatus();

    @GET("api/v1/auth/student/restricted-feature")
    Call<ApiResponse<String>> checkRestrictedStudentFeature();
}
