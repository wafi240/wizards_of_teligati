package com.teligati.app.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.teligati.app.BuildConfig;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.repository.AuthRepository;
import com.teligati.app.data.repository.MessagingRepository;
import com.teligati.app.data.repository.OpportunityRepository;
import com.teligati.app.data.repository.ProfileRepository;
import com.teligati.app.data.repository.ReferenceRepository;
import com.teligati.app.data.repository.WalletRepository;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final long TIMEOUT_SECONDS = 15;
    public static final String PREF_NETWORK_CONFIG = "wizards_network_config";
    public static final String KEY_CUSTOM_BASE_URL = "custom_base_url";

    private static volatile ApiClient instance;
    private final String baseUrl;
    private final AuthApiService authApiService;
    private final ProfileApiService profileApiService;
    private final OpportunityApiService opportunityApiService;
    private final WalletApiService walletApiService;
    private final ResearchApiService researchApiService;
    private final ReferenceApiService referenceApiService;
    private final MessagingApiService messagingApiService;
    private final OkHttpClient okHttpClient;
    private final Gson gson;

    private ApiClient(Context context, String baseUrl) {
        String resolved = (baseUrl != null && !baseUrl.trim().isEmpty())
                ? baseUrl.trim()
                : resolveBaseUrl(context);
        this.baseUrl = resolved.endsWith("/") ? resolved : resolved + "/";
        Log.i(TAG, "Initializing ApiClient with BASE_URL: " + this.baseUrl);
        SessionManager sessionManager = SessionManager.getInstance(context);

        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.redactHeader("Authorization");
        loggingInterceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.HEADERS
                : HttpLoggingInterceptor.Level.NONE);

        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(sessionManager, this.baseUrl))
                .authenticator(new TokenAuthenticator(sessionManager, this.baseUrl))
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(this.baseUrl)
                .client(this.okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(this.gson))
                .build();

        this.authApiService = retrofit.create(AuthApiService.class);
        this.profileApiService = retrofit.create(ProfileApiService.class);
        this.opportunityApiService = retrofit.create(OpportunityApiService.class);
        this.walletApiService = retrofit.create(WalletApiService.class);
        this.researchApiService = retrofit.create(ResearchApiService.class);
        this.referenceApiService = retrofit.create(ReferenceApiService.class);
        this.messagingApiService = retrofit.create(MessagingApiService.class);
    }

    public static ApiClient getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    Context appCtx = context != null ? context.getApplicationContext() : null;
                    String activeUrl = resolveBaseUrl(appCtx);
                    instance = new ApiClient(appCtx, activeUrl);
                }
            }
        }
        return instance;
    }

    /**
     * Determines whether the app is executing within an Android Emulator.
     */
    public static boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

    /**
     * Resolves the active backend BASE_URL:
     * 1. Checks user-configured custom URL in SharedPreferences.
     * 2. Auto-selects EMULATOR_BASE_URL (http://10.0.2.2:8080/) if running on Emulator.
     * 3. Auto-selects PHYSICAL_BASE_URL (http://192.168.0.64:8080/) if running on Physical Device.
     */
    public static String resolveBaseUrl(Context context) {
        if (context != null) {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NETWORK_CONFIG, Context.MODE_PRIVATE);
                String custom = prefs.getString(KEY_CUSTOM_BASE_URL, null);
                if (custom != null && !custom.trim().isEmpty()) {
                    String trimmed = custom.trim();
                    return trimmed.endsWith("/") ? trimmed : trimmed + "/";
                }
            } catch (Throwable ignored) {}
        }
        return isEmulator() ? BuildConfig.EMULATOR_BASE_URL : BuildConfig.PHYSICAL_BASE_URL;
    }

    /**
     * Dynamically updates the backend server URL at runtime and resets repository singletons.
     */
    public static synchronized void updateBaseUrl(Context context, String newBaseUrl) {
        Context appCtx = context != null ? context.getApplicationContext() : null;
        if (appCtx != null) {
            SharedPreferences prefs = appCtx.getSharedPreferences(PREF_NETWORK_CONFIG, Context.MODE_PRIVATE);
            if (newBaseUrl != null && !newBaseUrl.trim().isEmpty()) {
                String trimmed = newBaseUrl.trim();
                if (!trimmed.endsWith("/")) {
                    trimmed += "/";
                }
                prefs.edit().putString(KEY_CUSTOM_BASE_URL, trimmed).apply();
                instance = new ApiClient(appCtx, trimmed);
            } else {
                prefs.edit().remove(KEY_CUSTOM_BASE_URL).apply();
                instance = new ApiClient(appCtx, resolveBaseUrl(appCtx));
            }
        }

        // Reset singleton repositories to bind to the updated ApiClient
        AuthRepository.setInstanceForTesting(null);
        ProfileRepository.setInstanceForTesting(null);
        OpportunityRepository.setInstanceForTesting(null);
        MessagingRepository.setInstanceForTesting(null);
        ReferenceRepository.setInstanceForTesting(null);
        WalletRepository.setInstanceForTesting(null);
    }

    /**
     * Testing / custom configuration constructor.
     */
    public static ApiClient createCustom(Context context, String customBaseUrl) {
        return new ApiClient(context, customBaseUrl);
    }

    public static void setInstanceForTesting(ApiClient testClient) {
        instance = testClient;
    }

    public AuthApiService getAuthApiService() {
        return authApiService;
    }

    public ProfileApiService getProfileApiService() {
        return profileApiService;
    }

    public OpportunityApiService getOpportunityApiService() {
        return opportunityApiService;
    }

    public WalletApiService getWalletApiService() {
        return walletApiService;
    }

    public ResearchApiService getResearchApiService() {
        return researchApiService;
    }

    public ReferenceApiService getReferenceApiService() {
        return referenceApiService;
    }

    public MessagingApiService getMessagingApiService() {
        return messagingApiService;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public Gson getGson() {
        return gson;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
