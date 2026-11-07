package com.teligati.app.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teligati.app.data.SessionManager;
import com.teligati.app.network.model.ApiResponse;
import com.teligati.app.network.model.RefreshTokenRequest;
import com.teligati.app.network.model.TokenResponse;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.Authenticator;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class TokenAuthenticator implements Authenticator {
    private static final String TAG = "TokenAuthenticator";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final SessionManager sessionManager;
    private final String baseUrl;
    private final String backendHost;
    private final int backendPort;
    private final OkHttpClient refreshClient;
    private final Gson gson;

    public TokenAuthenticator(SessionManager sessionManager, String baseUrl) {
        this.sessionManager = sessionManager;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        HttpUrl parsed = HttpUrl.parse(this.baseUrl);
        this.backendHost = parsed != null ? parsed.host() : null;
        this.backendPort = parsed != null ? parsed.port() : -1;
        // Clean OkHttpClient without authenticator to prevent recursive 401 loops
        this.refreshClient = new OkHttpClient.Builder().build();
        this.gson = new Gson();
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        // Prevent infinite loops: only 1 retry allowed
        if (responseCount(response) >= 2) {
            return null;
        }

        // Only handle 401s that originated from the configured backend origin
        HttpUrl reqUrl = response.request().url();
        if (backendHost != null && (!backendHost.equalsIgnoreCase(reqUrl.host())
                || (backendPort != -1 && backendPort != reqUrl.port()))) {
            return null;
        }

        String path = reqUrl.encodedPath();
        // Do not attempt refresh on auth endpoints (login, refresh, logout, register)
        if (path.contains("/auth/refresh") || path.contains("/auth/login")
                || path.contains("/auth/logout") || path.contains("/auth/register")) {
            return null;
        }

        String failedAuthHeader = response.request().header("Authorization");

        // Synchronize on this authenticator to prevent concurrent token refresh race conditions
        synchronized (this) {
            String currentAccessToken = sessionManager.getAccessToken();
            String currentBearer = currentAccessToken != null ? "Bearer " + currentAccessToken : null;

            // If another thread already successfully refreshed the access token, retry immediately
            if (currentBearer != null && !currentBearer.equals(failedAuthHeader)) {
                return response.request().newBuilder()
                        .header("Authorization", currentBearer)
                        .build();
            }

            // Otherwise, perform the refresh call synchronously
            String refreshToken = sessionManager.getRefreshToken();
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                sessionManager.clearSession();
                return null;
            }

            RefreshTokenRequest refreshPayload = new RefreshTokenRequest(refreshToken);
            String jsonBody = gson.toJson(refreshPayload);

            Request refreshRequest = new Request.Builder()
                    .url(baseUrl + "api/v1/auth/refresh")
                    .post(RequestBody.create(jsonBody, JSON))
                    .header("Accept", "application/json")
                    .header("User-Agent", "WizardsOfTeligati-Android/1.0")
                    .build();

            try (Response refreshResponse = refreshClient.newCall(refreshRequest).execute()) {
                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    String responseString = refreshResponse.body().string();
                    Type type = new TypeToken<ApiResponse<TokenResponse>>() {}.getType();
                    ApiResponse<TokenResponse> apiResponse = gson.fromJson(responseString, type);

                    if (apiResponse != null && apiResponse.getData() != null) {
                        TokenResponse newTokens = apiResponse.getData();
                        // Atomically persist new tokens; returns false if session was logged out in parallel
                        boolean updated = sessionManager.updateTokens(
                                newTokens.getAccessToken(),
                                newTokens.getRefreshToken()
                        );

                        if (!updated) {
                            // Logout occurred while refresh was in flight; do not resurrect old session
                            return null;
                        }

                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + newTokens.getAccessToken())
                                .build();
                    }
                }

                // If refresh failed (400 token reuse, 401 expired, etc.), clear session
                sessionManager.clearSession();
                return null;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
