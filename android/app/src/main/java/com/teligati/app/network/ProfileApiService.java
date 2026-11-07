package com.teligati.app.network;

import com.teligati.app.network.model.AlumniDirectoryResponse;
import com.teligati.app.network.model.ApiResponse;
import com.teligati.app.network.model.PagedResponse;
import com.teligati.app.network.model.ProfileResponse;
import com.teligati.app.network.model.ProfileUpdateRequest;
import com.teligati.app.network.model.StudentDirectoryResponse;
import com.teligati.app.network.model.TeacherDirectoryResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProfileApiService {

    @GET("api/v1/profiles/me")
    Call<ApiResponse<ProfileResponse>> getMyProfile();

    @PUT("api/v1/profiles/me")
    Call<ApiResponse<ProfileResponse>> updateMyProfile(@Body ProfileUpdateRequest request);

    @GET("api/v1/profiles/{userId}")
    Call<ApiResponse<ProfileResponse>> getProfileById(@Path("userId") long userId);

    @GET("api/v1/directory/students")
    Call<ApiResponse<PagedResponse<StudentDirectoryResponse>>> getStudentDirectory(
            @Query("name") String name,
            @Query("department") String department,
            @Query("batch") String batch,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/directory/alumni")
    Call<ApiResponse<PagedResponse<AlumniDirectoryResponse>>> getAlumniDirectory(
            @Query("name") String name,
            @Query("department") String department,
            @Query("batch") String batch,
            @Query("company") String company,
            @Query("country") String country,
            @Query("position") String position,
            @Query("page") Integer page,
            @Query("size") Integer size
    );

    @GET("api/v1/directory/teachers")
    Call<ApiResponse<PagedResponse<TeacherDirectoryResponse>>> getTeacherDirectory(
            @Query("name") String name,
            @Query("department") String department,
            @Query("designation") String designation,
            @Query("researchFocus") String researchFocus,
            @Query("page") Integer page,
            @Query("size") Integer size
    );
}
