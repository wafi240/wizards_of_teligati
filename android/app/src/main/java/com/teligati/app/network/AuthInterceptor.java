package com.teligati.app.network;

import androidx.annotation.NonNull;

import com.teligati.app.data.SessionManager;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final SessionManager sessionManager;
    private final String backendHost;
    private final int backendPort;

    public AuthInterceptor(SessionManager sessionManager) {
        this(sessionManager, null);
    }

    public AuthInterceptor(SessionManager sessionManager, String baseUrl) {
        this.sessionManager = sessionManager;
        if (baseUrl != null) {
            HttpUrl parsed = HttpUrl.parse(baseUrl);
            this.backendHost = parsed != null ? parsed.host() : null;
            this.backendPort = parsed != null ? parsed.port() : -1;
        } else {
            this.backendHost = null;
            this.backendPort = -1;
        }
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", "WizardsOfTeligati-Android/1.0");

        // Attach Authorization header ONLY if the request origin matches the configured backend
        if (isBackendOrigin(original.url())) {
            String accessToken = sessionManager != null ? sessionManager.getAccessToken() : null;
            if (accessToken != null && !accessToken.trim().isEmpty() && original.header("Authorization") == null) {
                builder.header("Authorization", "Bearer " + accessToken);
            }
        }

        return chain.proceed(builder.build());
    }

    private boolean isBackendOrigin(HttpUrl url) {
        if (backendHost == null) {
            return true;
        }
        return backendHost.equalsIgnoreCase(url.host())
                && (backendPort == -1 || backendPort == url.port());
    }
}
