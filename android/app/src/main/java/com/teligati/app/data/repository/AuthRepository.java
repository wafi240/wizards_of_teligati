package com.teligati.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.database.AppDatabase;
import com.teligati.app.data.entity.AlumniProfileEntity;
import com.teligati.app.data.entity.StudentProfileEntity;
import com.teligati.app.data.entity.TeacherProfileEntity;
import com.teligati.app.data.entity.UserEntity;
import com.teligati.app.network.ApiClient;
import com.teligati.app.network.AuthApiService;
import com.teligati.app.network.model.AlumniRegisterRequest;
import com.teligati.app.network.model.ApiError;
import com.teligati.app.network.model.ApiResponse;
import com.teligati.app.network.model.FacultyRegisterRequest;
import com.teligati.app.network.model.LoginRequest;
import com.teligati.app.network.model.RefreshTokenRequest;
import com.teligati.app.network.model.StudentRegisterRequest;
import com.teligati.app.network.model.TokenResponse;
import com.teligati.app.network.model.UserSummaryResponse;
import com.teligati.app.network.model.VerificationStatusResponse;

import java.lang.reflect.Type;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class AuthRepository {
    private static volatile AuthRepository INSTANCE;

    private final AuthApiService apiService;
    private final SessionManager sessionManager;
    private final AppDatabase db;
    private final ExecutorService executor;
    private final Executor postExecutor;
    private final Gson gson;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public AuthRepository(AuthApiService apiService, SessionManager sessionManager, AppDatabase db) {
        this(apiService, sessionManager, db, createDefaultPostExecutor());
    }

    public AuthRepository(AuthApiService apiService, SessionManager sessionManager, AppDatabase db, Executor postExecutor) {
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.db = db;
        this.executor = Executors.newFixedThreadPool(2);
        this.postExecutor = postExecutor != null ? postExecutor : Runnable::run;
        this.gson = new Gson();
    }

    private static Executor createDefaultPostExecutor() {
        try {
            if (Looper.getMainLooper() != null) {
                Handler handler = new Handler(Looper.getMainLooper());
                return handler::post;
            }
        } catch (Throwable ignored) {}
        return Runnable::run;
    }

    public static AuthRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AuthRepository.class) {
                if (INSTANCE == null) {
                    Context appCtx = context.getApplicationContext();
                    INSTANCE = new AuthRepository(
                            ApiClient.getInstance(appCtx).getAuthApiService(),
                            SessionManager.getInstance(appCtx),
                            AppDatabase.getInstance(appCtx)
                    );
                }
            }
        }
        return INSTANCE;
    }

    public static void setInstanceForTesting(AuthRepository testRepo) {
        INSTANCE = testRepo;
    }

    private <T> void postSuccess(Callback<T> callback, T result) {
        if (callback != null) {
            postExecutor.execute(() -> callback.onSuccess(result));
        }
    }

    private <T> void postError(Callback<T> callback, String error) {
        if (callback != null) {
            postExecutor.execute(() -> callback.onError(error));
        }
    }

    public void login(String identifier, String password, Callback<TokenResponse> callback) {
        LoginRequest request = new LoginRequest(identifier, password);
        apiService.login(request).enqueue(new retrofit2.Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TokenResponse>> call,
                                   @NonNull Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    TokenResponse tokenResponse = response.body().getData();
                    sessionManager.saveAuthSession(tokenResponse);
                    syncUserWithRoom(tokenResponse, () -> postSuccess(callback, tokenResponse));
                } else {
                    postError(callback, parseErrorMessage(response, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TokenResponse>> call, @NonNull Throwable t) {
                postError(callback, parseErrorMessage(null, t));
            }
        });
    }

    public void registerStudent(StudentRegisterRequest request, Callback<TokenResponse> callback) {
        apiService.registerStudent(request).enqueue(new retrofit2.Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TokenResponse>> call,
                                   @NonNull Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    TokenResponse tokenResponse = response.body().getData();
                    sessionManager.saveAuthSession(tokenResponse);
                    syncUserWithRoom(tokenResponse, () -> postSuccess(callback, tokenResponse));
                } else {
                    postError(callback, parseErrorMessage(response, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TokenResponse>> call, @NonNull Throwable t) {
                postError(callback, parseErrorMessage(null, t));
            }
        });
    }

    public void registerAlumni(AlumniRegisterRequest request, Callback<TokenResponse> callback) {
        apiService.registerAlumni(request).enqueue(new retrofit2.Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TokenResponse>> call,
                                   @NonNull Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    TokenResponse tokenResponse = response.body().getData();
                    sessionManager.saveAuthSession(tokenResponse);
                    syncUserWithRoom(tokenResponse, () -> postSuccess(callback, tokenResponse));
                } else {
                    postError(callback, parseErrorMessage(response, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TokenResponse>> call, @NonNull Throwable t) {
                postError(callback, parseErrorMessage(null, t));
            }
        });
    }

    public void registerFaculty(FacultyRegisterRequest request, Callback<TokenResponse> callback) {
        apiService.registerFaculty(request).enqueue(new retrofit2.Callback<ApiResponse<TokenResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<TokenResponse>> call,
                                   @NonNull Response<ApiResponse<TokenResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    TokenResponse tokenResponse = response.body().getData();
                    sessionManager.saveAuthSession(tokenResponse);
                    syncUserWithRoom(tokenResponse, () -> postSuccess(callback, tokenResponse));
                } else {
                    postError(callback, parseErrorMessage(response, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<TokenResponse>> call, @NonNull Throwable t) {
                postError(callback, parseErrorMessage(null, t));
            }
        });
    }

    public void logout(Callback<Void> callback) {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {}
        String refreshToken = sessionManager.getRefreshToken();
        RefreshTokenRequest body = refreshToken != null ? new RefreshTokenRequest(refreshToken) : null;

        apiService.logout(body).enqueue(new retrofit2.Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                   @NonNull Response<ApiResponse<Void>> response) {
                sessionManager.clearSession();
                postSuccess(callback, null);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                // Clear session even if network failed
                sessionManager.clearSession();
                postSuccess(callback, null);
            }
        });
    }

    public void getCurrentUser(Callback<UserSummaryResponse> callback) {
        apiService.getCurrentUser().enqueue(new retrofit2.Callback<ApiResponse<UserSummaryResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<UserSummaryResponse>> call,
                                   @NonNull Response<ApiResponse<UserSummaryResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    UserSummaryResponse userSummary = response.body().getData();
                    sessionManager.setVerified(userSummary.isVerified());
                    postSuccess(callback, userSummary);
                } else {
                    postError(callback, parseErrorMessage(response, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<UserSummaryResponse>> call, @NonNull Throwable t) {
                postError(callback, parseErrorMessage(null, t));
            }
        });
    }

    public void getVerificationStatus(Callback<VerificationStatusResponse> callback) {
        apiService.getVerificationStatus().enqueue(new retrofit2.Callback<ApiResponse<VerificationStatusResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<VerificationStatusResponse>> call,
                                   @NonNull Response<ApiResponse<VerificationStatusResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    VerificationStatusResponse statusResponse = response.body().getData();
                    sessionManager.setVerified(statusResponse.isVerified());
                    postSuccess(callback, statusResponse);
                } else {
                    postError(callback, parseErrorMessage(response, null));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<VerificationStatusResponse>> call, @NonNull Throwable t) {
                postError(callback, parseErrorMessage(null, t));
            }
        });
    }

    public void syncUserWithRoom(TokenResponse tokenResponse, Runnable onComplete) {
        if (db == null || tokenResponse == null || tokenResponse.getUser() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        executor.execute(() -> {
            try {
                UserSummaryResponse summary = tokenResponse.getUser();
                String userId = (summary.getKuetId() != null && !summary.getKuetId().trim().isEmpty())
                        ? summary.getKuetId() : summary.getEmail();
                String normRole = SessionManager.normalizeRole(summary.getRole());

                UserEntity existingUser = db.userDao().getUserById(userId);
                double available = existingUser != null ? existingUser.getAvailableBalance() : 1000.0;
                double escrow = existingUser != null ? existingUser.getEscrowBalance() : 0.0;

                UserEntity updatedUser = new UserEntity(
                        userId,
                        normRole,
                        summary.getName() != null ? summary.getName() : "KUET User",
                        summary.getEmail() != null ? summary.getEmail() : "",
                        summary.getDepartment() != null ? summary.getDepartment() : "CSE",
                        summary.getKuetId() != null ? summary.getKuetId() : "",
                        "",
                        summary.isVerified(),
                        available,
                        escrow
                );
                db.userDao().insertUser(updatedUser);

                // Ensure role profiles exist
                if ("STUDENT".equalsIgnoreCase(normRole) && summary.getKuetId() != null) {
                    StudentProfileEntity existingProfile = db.profileDao().getStudentProfile(summary.getKuetId());
                    if (existingProfile == null) {
                        StudentProfileEntity newProfile = new StudentProfileEntity(
                                summary.getKuetId(),
                                summary.getName(),
                                summary.getDepartment(),
                                "2023",
                                3.84,
                                "Java, Android, Spring Boot",
                                "Wizards of Teligati",
                                "",
                                "",
                                "Software Engineering",
                                "KUET Cyber Gaming Club",
                                "KUET CSE Undergraduate Student"
                        );
                        db.profileDao().insertStudentProfile(newProfile);
                    }
                } else if ("ALUMNI".equalsIgnoreCase(normRole)) {
                    AlumniProfileEntity existingAlumni = db.profileDao().getAlumniProfileByUserId(userId);
                    if (existingAlumni == null) {
                        AlumniProfileEntity newAlumni = new AlumniProfileEntity(
                                0,
                                userId,
                                summary.getName(),
                                summary.getDepartment(),
                                "2015",
                                "KUET Tech Solutions",
                                "Senior Software Engineer",
                                "Bangladesh",
                                "Khulna",
                                summary.isVerified(),
                                true,
                                true,
                                "Cloud Architecture, Java, Distributed Systems",
                                "KUET Alumnus passionate about mentoring"
                        );
                        db.profileDao().insertAlumniProfile(newAlumni);
                    }
                } else if ("TEACHER".equalsIgnoreCase(normRole)) {
                    TeacherProfileEntity existingTeacher = db.profileDao().getTeacherProfileByUserId(userId);
                    if (existingTeacher == null) {
                        TeacherProfileEntity newTeacher = new TeacherProfileEntity(
                                0,
                                userId,
                                summary.getName(),
                                "Associate Professor",
                                summary.getDepartment(),
                                "Distributed Systems & Machine Learning",
                                "Data Engineering Lab",
                                12,
                                summary.getEmail(),
                                "Faculty member at Khulna University of Engineering & Technology"
                        );
                        db.profileDao().insertTeacherProfile(newTeacher);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (onComplete != null) {
                    postExecutor.execute(onComplete);
                }
            }
        });
    }

    private String parseErrorMessage(Response<?> response, Throwable t) {
        if (t != null) {
            String msg = t.getMessage() != null ? t.getMessage() : "Unable to reach backend server.";
            return "Connection error: " + msg + "\n\nTip: Ensure your phone and PC are connected to the same Wi-Fi network and backend is running on port 8080.";
        }

        if (response != null && response.errorBody() != null) {
            try {
                String errorBodyStr = response.errorBody().string();
                Type type = new TypeToken<ApiResponse<ApiError>>() {}.getType();
                ApiResponse<ApiError> errResp = gson.fromJson(errorBodyStr, type);

                if (errResp != null) {
                    if (errResp.getData() != null) {
                        ApiError errorData = errResp.getData();
                        if (errorData.getDetails() != null && !errorData.getDetails().isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            if (errResp.getMessage() != null && !errResp.getMessage().isEmpty()) {
                                sb.append(errResp.getMessage()).append(":\n");
                            }
                            for (String detail : errorData.getDetails()) {
                                sb.append("• ").append(detail).append("\n");
                            }
                            return sb.toString().trim();
                        }
                        if (errorData.getMessage() != null && !errorData.getMessage().isEmpty()) {
                            return errorData.getMessage();
                        }
                    }
                    if (errResp.getMessage() != null && !errResp.getMessage().isEmpty()) {
                        return errResp.getMessage();
                    }
                }
            } catch (Exception ignored) {
            }
            return "Authentication request failed (HTTP " + response.code() + ")";
        }

        return "An unexpected error occurred. Please try again.";
    }
}
