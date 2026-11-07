package com.teligati.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.database.AppDatabase;
import com.teligati.app.data.entity.AlumniProfileEntity;
import com.teligati.app.data.entity.StudentProfileEntity;
import com.teligati.app.data.entity.TeacherProfileEntity;
import com.teligati.app.data.entity.UserEntity;
import com.teligati.app.firebase.FirebaseHelper;
import com.teligati.app.network.ProfileApiService;
import com.teligati.app.network.model.AlumniDirectoryResponse;
import com.teligati.app.network.model.ApiError;
import com.teligati.app.network.model.ApiResponse;
import com.teligati.app.network.model.PagedResponse;
import com.teligati.app.network.model.ProfileResponse;
import com.teligati.app.network.model.ProfileUpdateRequest;
import com.teligati.app.network.model.StudentDirectoryResponse;
import com.teligati.app.network.model.TeacherDirectoryResponse;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

/**
 * ProfileRepository provides single-point-of-truth profile management for Wizards of Teligati.
 *
 * In Phase 2:
 * - Persistent profile data and directory operations are migrated to Firebase Firestore as the source of truth.
 * - Private profile fields live in users/{uid}.
 * - Sanitized public cards live in public_profiles/{uid} (zero leakage of private info).
 * - Room database acts as an offline cache.
 * - Spring Boot APIs are not called during normal runtime operations.
 */
public class ProfileRepository {
    private static final String TAG = "ProfileRepository";
    private static volatile ProfileRepository INSTANCE;

    private final FirebaseFirestore firestore;
    private final ProfileApiService apiService; // Preserved for backward test compatibility
    private final SessionManager sessionManager;
    private final AppDatabase db;
    private final ExecutorService executor;
    private final Executor postExecutor;
    private final Gson gson;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    /**
     * Primary constructor using Firebase Firestore as the source of truth.
     */
    public ProfileRepository(FirebaseFirestore firestore, SessionManager sessionManager, AppDatabase db) {
        this(firestore, null, sessionManager, db, createDefaultPostExecutor());
    }

    public ProfileRepository(FirebaseFirestore firestore, SessionManager sessionManager, AppDatabase db, Executor postExecutor) {
        this(firestore, null, sessionManager, db, postExecutor);
    }

    /**
     * Legacy constructor preserved for test harnesses that supply a Retrofit ProfileApiService.
     */
    public ProfileRepository(ProfileApiService apiService, SessionManager sessionManager, AppDatabase db) {
        this(null, apiService, sessionManager, db, createDefaultPostExecutor());
    }

    public ProfileRepository(ProfileApiService apiService, SessionManager sessionManager, AppDatabase db, Executor postExecutor) {
        this(null, apiService, sessionManager, db, postExecutor);
    }

    public ProfileRepository(FirebaseFirestore firestore, ProfileApiService apiService,
                             SessionManager sessionManager, AppDatabase db, Executor postExecutor) {
        this.firestore = firestore;
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.db = db;
        this.executor = Executors.newFixedThreadPool(4);
        this.postExecutor = postExecutor != null ? postExecutor : createDefaultPostExecutor();
        this.gson = new Gson();
    }

    private static Executor createDefaultPostExecutor() {
        try {
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null) {
                Handler handler = new Handler(mainLooper);
                return handler::post;
            }
        } catch (RuntimeException ignored) {
            // JVM Unit test environment without Android MainLooper
        }
        return Runnable::run;
    }

    public static ProfileRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ProfileRepository.class) {
                if (INSTANCE == null) {
                    Context appCtx = context.getApplicationContext();
                    FirebaseHelper.ensureFirebaseApp(appCtx);
                    FirebaseFirestore dbFirestore = null;
                    try {
                        dbFirestore = FirebaseFirestore.getInstance();
                    } catch (Throwable t) {
                        Log.w(TAG, "FirebaseFirestore instance unavailable: " + t.getMessage());
                    }
                    SessionManager sessionManager = SessionManager.getInstance(appCtx);
                    AppDatabase database = AppDatabase.getInstance(appCtx);
                    // Firestore is primary source of truth in Android runtime
                    INSTANCE = new ProfileRepository(dbFirestore, sessionManager, database);
                }
            }
        }
        return INSTANCE;
    }

    public static void setInstanceForTesting(ProfileRepository testRepo) {
        INSTANCE = testRepo;
    }

    // =========================================================================
    // 1. FETCH & UPDATE MY PROFILE (FIRESTORE SOURCE OF TRUTH)
    // =========================================================================

    public void fetchMyProfile(@NonNull Callback<ProfileResponse> callback) {
        if (firestore != null) {
            fetchMyProfileFromFirestore(callback);
            return;
        }

        // Test fallback to Retrofit MockWebServer if configured in legacy unit test
        if (apiService != null) {
            fetchMyProfileFromRetrofit(callback);
            return;
        }

        // Offline fallback: try reading from Room cache
        ProfileResponse cached = loadCachedProfileFromRoom();
        if (cached != null) {
            postResult(callback, cached);
        } else {
            postError(callback, "No data source available and no cached profile found.");
        }
    }

    private void fetchMyProfileFromFirestore(@NonNull Callback<ProfileResponse> callback) {
        String currentUid = resolveCurrentUid();
        if (currentUid == null || currentUid.isEmpty()) {
            ProfileResponse cached = loadCachedProfileFromRoom();
            if (cached != null) {
                postResult(callback, cached);
            } else {
                postError(callback, "User not authenticated with Firebase");
            }
            return;
        }

        final String targetUid = currentUid;
        firestore.collection("users").document(targetUid).get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists() && snap.getData() != null) {
                        ProfileResponse profile = mapFirestoreToProfileResponse(snap.getData(), targetUid);
                        syncProfileToRoom(profile);
                        postResult(callback, profile);
                    } else {
                        // Document doesn't exist in Firestore; fallback to Room
                        ProfileResponse cached = loadCachedProfileFromRoom();
                        if (cached != null) {
                            postResult(callback, cached);
                        } else {
                            postError(callback, "Profile not found in Firestore for UID: " + targetUid);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Network / offline error: graceful Room fallback
                    ProfileResponse cached = loadCachedProfileFromRoom();
                    if (cached != null) {
                        postResult(callback, cached);
                    } else {
                        postError(callback, e.getMessage() != null ? e.getMessage() : "Failed to load profile from Firestore");
                    }
                });
    }

    private void fetchMyProfileFromRetrofit(@NonNull Callback<ProfileResponse> callback) {
        executor.execute(() -> {
            try {
                Response<ApiResponse<ProfileResponse>> response = apiService.getMyProfile().execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ProfileResponse profile = response.body().getData();
                    syncProfileToRoom(profile);
                    postResult(callback, profile);
                } else {
                    String err = parseErrorMessage(response);
                    ProfileResponse cached = loadCachedProfileFromRoom();
                    if (cached != null) {
                        postResult(callback, cached);
                    } else {
                        postError(callback, err);
                    }
                }
            } catch (Exception e) {
                ProfileResponse cached = loadCachedProfileFromRoom();
                if (cached != null) {
                    postResult(callback, cached);
                } else {
                    postError(callback, e.getMessage() != null ? e.getMessage() : "Failed to load profile");
                }
            }
        });
    }

    public void updateMyProfile(@NonNull ProfileUpdateRequest request, @NonNull Callback<ProfileResponse> callback) {
        if (firestore != null) {
            updateMyProfileInFirestore(request, callback);
            return;
        }

        if (apiService != null) {
            updateMyProfileInRetrofit(request, callback);
            return;
        }

        postError(callback, "No persistent backend available to update profile.");
    }

    private void updateMyProfileInFirestore(@NonNull ProfileUpdateRequest request, @NonNull Callback<ProfileResponse> callback) {
        String currentUid = resolveCurrentUid();
        if (currentUid == null || currentUid.isEmpty()) {
            postError(callback, "Permission denied: only authenticated account owners can edit their own profile.");
            return;
        }

        final String uid = currentUid;

        // Fetch existing private profile to ensure immutable UID, email, and role are preserved
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    Map<String, Object> existing = (userDoc != null && userDoc.exists() && userDoc.getData() != null)
                            ? new HashMap<>(userDoc.getData())
                            : new HashMap<>();

                    // Maintain immutable identity fields per security rules
                    String email = existing.containsKey("email") && existing.get("email") != null
                            ? existing.get("email").toString().trim()
                            : sessionManager.getUserEmail();
                    if (email.isEmpty()) {
                        try {
                            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                            if (u != null && u.getEmail() != null) email = u.getEmail().trim();
                        } catch (Throwable ignored) {}
                    }

                    String role = existing.containsKey("role") && existing.get("role") != null
                            ? SessionManager.normalizeRole(existing.get("role").toString())
                            : sessionManager.getUserRole();

                    Map<String, Object> privateUpdates = new HashMap<>(existing);
                    privateUpdates.put("uid", uid);
                    privateUpdates.put("email", email);
                    privateUpdates.put("role", role);
                    privateUpdates.put("updatedAt", System.currentTimeMillis());

                    if (request.getName() != null && !request.getName().trim().isEmpty()) {
                        privateUpdates.put("name", request.getName().trim());
                    }
                    if (request.getDepartment() != null && !request.getDepartment().trim().isEmpty()) {
                        privateUpdates.put("department", request.getDepartment().trim());
                    }
                    if (request.getBatch() != null && !request.getBatch().trim().isEmpty()) {
                        privateUpdates.put("batch", request.getBatch().trim());
                    }
                    if (request.getCurrentCompany() != null) {
                        privateUpdates.put("company", request.getCurrentCompany().trim());
                        privateUpdates.put("currentCompany", request.getCurrentCompany().trim());
                    }
                    if (request.getCurrentPosition() != null) {
                        privateUpdates.put("position", request.getCurrentPosition().trim());
                        privateUpdates.put("currentPosition", request.getCurrentPosition().trim());
                    }
                    if (request.getCountry() != null) {
                        privateUpdates.put("country", request.getCountry().trim());
                    }
                    if (request.getCity() != null) {
                        privateUpdates.put("city", request.getCity().trim());
                    }
                    if (request.getDesignation() != null) {
                        privateUpdates.put("designation", request.getDesignation().trim());
                    }
                    if (request.getLabName() != null) {
                        privateUpdates.put("lab", request.getLabName().trim());
                        privateUpdates.put("labName", request.getLabName().trim());
                    }
                    if (request.getResearchFocus() != null) {
                        privateUpdates.put("researchArea", request.getResearchFocus().trim());
                        privateUpdates.put("researchFocus", request.getResearchFocus().trim());
                    }
                    if (request.getSkills() != null) {
                        privateUpdates.put("skills", request.getSkills().trim());
                    }
                    if (request.getProjects() != null) {
                        privateUpdates.put("projects", request.getProjects().trim());
                    }
                    if (request.getSpecialization() != null) {
                        privateUpdates.put("specialization", request.getSpecialization().trim());
                    }
                    if (request.getClubs() != null) {
                        privateUpdates.put("clubs", request.getClubs().trim());
                    }
                    if (request.getBio() != null) {
                        privateUpdates.put("bio", request.getBio().trim());
                    }
                    if (request.getGithubUrl() != null) {
                        privateUpdates.put("githubUrl", request.getGithubUrl().trim());
                    }
                    if (request.getLinkedinUrl() != null) {
                        privateUpdates.put("linkedinUrl", request.getLinkedinUrl().trim());
                    }
                    if (request.getCgpa() != null) {
                        privateUpdates.put("cgpa", request.getCgpa());
                    }
                    if (request.getIsAvailableForMentorship() != null) {
                        privateUpdates.put("isAvailableForMentorship", request.getIsAvailableForMentorship());
                    }
                    if (request.getIsActivelyHiring() != null) {
                        privateUpdates.put("isActivelyHiring", request.getIsActivelyHiring());
                    }

                    // Build strictly allowlisted public card (prevents private leakage into directory)
                    Map<String, Object> publicCard = FirebaseHelper.sanitizePublicProfile(uid, privateUpdates);

                    // Commit atomic batch to both users/{uid} and public_profiles/{uid}
                    WriteBatch batch = firestore.batch();
                    batch.set(firestore.collection("users").document(uid), privateUpdates, SetOptions.merge());
                    batch.set(firestore.collection("public_profiles").document(uid), publicCard, SetOptions.merge());

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                ProfileResponse updated = mapFirestoreToProfileResponse(privateUpdates, uid);
                                syncProfileToRoom(updated);
                                postResult(callback, updated);
                            })
                            .addOnFailureListener(e -> postError(callback, "Firestore update failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> postError(callback, "Failed to read user document: " + e.getMessage()));
    }

    private void updateMyProfileInRetrofit(@NonNull ProfileUpdateRequest request, @NonNull Callback<ProfileResponse> callback) {
        executor.execute(() -> {
            try {
                Response<ApiResponse<ProfileResponse>> response = apiService.updateMyProfile(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ProfileResponse updated = response.body().getData();
                    syncProfileToRoom(updated);
                    postResult(callback, updated);
                } else {
                    String err = parseErrorMessage(response);
                    postError(callback, err);
                }
            } catch (Exception e) {
                postError(callback, e.getMessage() != null ? e.getMessage() : "Failed to update profile");
            }
        });
    }

    public void getProfileById(long userId, @NonNull Callback<ProfileResponse> callback) {
        if (firestore != null) {
            String docId = String.valueOf(userId);
            String currentUid = resolveCurrentUid();
            String collection = (currentUid != null && currentUid.equals(docId)) ? "users" : "public_profiles";

            firestore.collection(collection).document(docId).get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null && snap.exists() && snap.getData() != null) {
                            ProfileResponse p = mapFirestoreToProfileResponse(snap.getData(), docId);
                            postResult(callback, p);
                        } else {
                            postError(callback, "Profile not found for ID: " + userId);
                        }
                    })
                    .addOnFailureListener(e -> postError(callback, e.getMessage()));
            return;
        }

        if (apiService != null) {
            executor.execute(() -> {
                try {
                    Response<ApiResponse<ProfileResponse>> response = apiService.getProfileById(userId).execute();
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        postResult(callback, response.body().getData());
                    } else {
                        postError(callback, parseErrorMessage(response));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage() != null ? e.getMessage() : "Failed to load profile");
                }
            });
            return;
        }

        postError(callback, "No data source available for getProfileById");
    }

    // =========================================================================
    // 2. DIRECTORY APIS (FIRESTORE SOURCE OF TRUTH + OPTIONAL ROOM CACHE)
    // =========================================================================

    public void getStudentDirectory(String name, String department, String batch, Integer page, Integer size,
                                    @NonNull Callback<PagedResponse<StudentDirectoryResponse>> callback) {
        if (firestore != null) {
            firestore.collection("public_profiles")
                    .whereEqualTo("role", "STUDENT")
                    .limit(100)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        List<StudentDirectoryResponse> list = new ArrayList<>();
                        long idCounter = 1L;
                        for (QueryDocumentSnapshot snap : snapshots) {
                            Map<String, Object> data = snap.getData();
                            StudentDirectoryResponse s = new StudentDirectoryResponse();
                            s.setUserId(idCounter++);
                            s.setName(getString(data, "name", "Student"));
                            s.setDepartment(getString(data, "department", "CSE"));
                            s.setBatch(getString(data, "batch", ""));
                            s.setRoll(""); // Private roll is not exposed in public directory
                            s.setVerified(true);

                            // In-memory filter
                            if (name != null && !name.trim().isEmpty() && !s.getName().toLowerCase().contains(name.trim().toLowerCase())) {
                                continue;
                            }
                            if (department != null && !department.trim().isEmpty() && !s.getDepartment().toLowerCase().contains(department.trim().toLowerCase())) {
                                continue;
                            }
                            if (batch != null && !batch.trim().isEmpty() && !s.getBatch().toLowerCase().contains(batch.trim().toLowerCase())) {
                                continue;
                            }
                            list.add(s);
                        }

                        PagedResponse<StudentDirectoryResponse> paged = new PagedResponse<>();
                        paged.setContent(list);
                        paged.setPage(page != null ? page : 0);
                        paged.setSize(size != null ? size : list.size());
                        paged.setTotalElements(list.size());
                        paged.setTotalPages(1);
                        paged.setLast(true);
                        postResult(callback, paged);
                    })
                    .addOnFailureListener(e -> postError(callback, "Failed to load student directory from Firestore: " + e.getMessage()));
            return;
        }

        if (apiService != null) {
            executor.execute(() -> {
                try {
                    Response<ApiResponse<PagedResponse<StudentDirectoryResponse>>> response =
                            apiService.getStudentDirectory(name, department, batch, page, size).execute();
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        postResult(callback, response.body().getData());
                    } else {
                        postError(callback, parseErrorMessage(response));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage() != null ? e.getMessage() : "Failed to load student directory");
                }
            });
            return;
        }

        postError(callback, "No data source available for student directory");
    }

    public void getAlumniDirectory(String name, String department, String batch, String company, String country,
                                   String position, Integer page, Integer size,
                                   @NonNull Callback<PagedResponse<AlumniDirectoryResponse>> callback) {
        if (firestore != null) {
            firestore.collection("public_profiles")
                    .whereEqualTo("role", "ALUMNI")
                    .limit(100)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        List<AlumniDirectoryResponse> list = new ArrayList<>();
                        long idCounter = 1L;
                        for (QueryDocumentSnapshot snap : snapshots) {
                            Map<String, Object> data = snap.getData();
                            AlumniDirectoryResponse a = new AlumniDirectoryResponse();
                            a.setUserId(idCounter++);
                            a.setName(getString(data, "name", "Alumni"));
                            a.setDepartment(getString(data, "department", "CSE"));
                            a.setBatch(getString(data, "batch", ""));
                            a.setCurrentCompany(getString(data, "company", ""));
                            a.setCurrentPosition(getString(data, "position", ""));
                            a.setCountry(getString(data, "country", ""));
                            a.setCity(getString(data, "city", ""));
                            a.setVerified(true);
                            a.setAvailableForMentorship(true);

                            // In-memory filters
                            if (name != null && !name.trim().isEmpty() && !a.getName().toLowerCase().contains(name.trim().toLowerCase())) {
                                continue;
                            }
                            if (department != null && !department.trim().isEmpty() && !a.getDepartment().toLowerCase().contains(department.trim().toLowerCase())) {
                                continue;
                            }
                            if (batch != null && !batch.trim().isEmpty() && !a.getBatch().toLowerCase().contains(batch.trim().toLowerCase())) {
                                continue;
                            }
                            if (company != null && !company.trim().isEmpty() && !a.getCurrentCompany().toLowerCase().contains(company.trim().toLowerCase())) {
                                continue;
                            }
                            if (country != null && !country.trim().isEmpty() && !a.getCountry().toLowerCase().contains(country.trim().toLowerCase())) {
                                continue;
                            }
                            if (position != null && !position.trim().isEmpty() && !a.getCurrentPosition().toLowerCase().contains(position.trim().toLowerCase())) {
                                continue;
                            }
                            list.add(a);
                        }

                        // Synchronize to local Room database for offline cache
                        syncAlumniDirectoryToRoom(list);

                        PagedResponse<AlumniDirectoryResponse> paged = new PagedResponse<>();
                        paged.setContent(list);
                        paged.setPage(page != null ? page : 0);
                        paged.setSize(size != null ? size : list.size());
                        paged.setTotalElements(list.size());
                        paged.setTotalPages(1);
                        paged.setLast(true);
                        postResult(callback, paged);
                    })
                    .addOnFailureListener(e -> {
                        // Offline fallback to Room database
                        PagedResponse<AlumniDirectoryResponse> cached = loadCachedAlumniFromRoom();
                        if (cached != null && !cached.getContent().isEmpty()) {
                            postResult(callback, cached);
                        } else {
                            postError(callback, "Failed to load alumni directory: " + e.getMessage());
                        }
                    });
            return;
        }

        if (apiService != null) {
            executor.execute(() -> {
                try {
                    Response<ApiResponse<PagedResponse<AlumniDirectoryResponse>>> response =
                            apiService.getAlumniDirectory(name, department, batch, company, country, position, page, size).execute();
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        PagedResponse<AlumniDirectoryResponse> paged = response.body().getData();
                        syncAlumniDirectoryToRoom(paged.getContent());
                        postResult(callback, paged);
                    } else {
                        PagedResponse<AlumniDirectoryResponse> cached = loadCachedAlumniFromRoom();
                        if (cached != null && !cached.getContent().isEmpty()) {
                            postResult(callback, cached);
                        } else {
                            postError(callback, parseErrorMessage(response));
                        }
                    }
                } catch (Exception e) {
                    PagedResponse<AlumniDirectoryResponse> cached = loadCachedAlumniFromRoom();
                    if (cached != null && !cached.getContent().isEmpty()) {
                        postResult(callback, cached);
                    } else {
                        postError(callback, e.getMessage() != null ? e.getMessage() : "Failed to load alumni directory");
                    }
                }
            });
            return;
        }

        // Room-only fallback
        PagedResponse<AlumniDirectoryResponse> cached = loadCachedAlumniFromRoom();
        if (cached != null) {
            postResult(callback, cached);
        } else {
            postError(callback, "No data source available for alumni directory");
        }
    }

    public void getTeacherDirectory(String name, String department, String designation, String researchFocus,
                                    Integer page, Integer size,
                                    @NonNull Callback<PagedResponse<TeacherDirectoryResponse>> callback) {
        if (firestore != null) {
            firestore.collection("public_profiles")
                    .whereEqualTo("role", "TEACHER")
                    .limit(100)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        List<TeacherDirectoryResponse> list = new ArrayList<>();
                        long idCounter = 1L;
                        for (QueryDocumentSnapshot snap : snapshots) {
                            Map<String, Object> data = snap.getData();
                            TeacherDirectoryResponse t = new TeacherDirectoryResponse();
                            t.setUid(snap.getId());
                            t.setUserId(idCounter++);
                            t.setName(getString(data, "name", "Faculty"));
                            t.setDepartment(getString(data, "department", "Dept of CSE"));
                            t.setDesignation(getString(data, "designation", "Faculty Member"));
                            t.setLabName(getString(data, "lab", ""));
                            t.setResearchFocus(getString(data, "researchArea", ""));
                            t.setVerified(true);

                            // In-memory filters
                            if (name != null && !name.trim().isEmpty() && !t.getName().toLowerCase().contains(name.trim().toLowerCase())) {
                                continue;
                            }
                            if (department != null && !department.trim().isEmpty() && !t.getDepartment().toLowerCase().contains(department.trim().toLowerCase())) {
                                continue;
                            }
                            if (designation != null && !designation.trim().isEmpty() && !t.getDesignation().toLowerCase().contains(designation.trim().toLowerCase())) {
                                continue;
                            }
                            if (researchFocus != null && !researchFocus.trim().isEmpty() && !t.getResearchFocus().toLowerCase().contains(researchFocus.trim().toLowerCase())) {
                                continue;
                            }
                            list.add(t);
                        }

                        PagedResponse<TeacherDirectoryResponse> paged = new PagedResponse<>();
                        paged.setContent(list);
                        paged.setPage(page != null ? page : 0);
                        paged.setSize(size != null ? size : list.size());
                        paged.setTotalElements(list.size());
                        paged.setTotalPages(1);
                        paged.setLast(true);
                        postResult(callback, paged);
                    })
                    .addOnFailureListener(e -> postError(callback, "Failed to load faculty directory from Firestore: " + e.getMessage()));
            return;
        }

        if (apiService != null) {
            executor.execute(() -> {
                try {
                    Response<ApiResponse<PagedResponse<TeacherDirectoryResponse>>> response =
                            apiService.getTeacherDirectory(name, department, designation, researchFocus, page, size).execute();
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        postResult(callback, response.body().getData());
                    } else {
                        postError(callback, parseErrorMessage(response));
                    }
                } catch (Exception e) {
                    postError(callback, e.getMessage() != null ? e.getMessage() : "Failed to load teacher directory");
                }
            });
            return;
        }

        postError(callback, "No data source available for teacher directory");
    }

    // =========================================================================
    // 3. DATA MAPPING & ROOM CACHING
    // =========================================================================

    public static ProfileResponse mapFirestoreToProfileResponse(Map<String, Object> data, String uid) {
        ProfileResponse p = new ProfileResponse();
        if (data == null) return p;

        p.setKuetId(getString(data, "roll", getString(data, "kuetId", uid)));
        p.setName(getString(data, "name", "KUET Member"));
        p.setEmail(getString(data, "email", ""));
        p.setRole(SessionManager.normalizeRole(getString(data, "role", "STUDENT")));
        p.setDepartment(getString(data, "department", "CSE"));
        p.setBatch(getString(data, "batch", ""));
        p.setCurrentCompany(getString(data, "company", getString(data, "currentCompany", "")));
        p.setCurrentPosition(getString(data, "position", getString(data, "currentPosition", "")));
        p.setCountry(getString(data, "country", ""));
        p.setCity(getString(data, "city", ""));
        p.setDesignation(getString(data, "designation", ""));
        p.setLabName(getString(data, "lab", getString(data, "labName", "")));
        p.setResearchFocus(getString(data, "researchArea", getString(data, "researchFocus", "")));
        p.setSkills(getString(data, "skills", ""));
        p.setProjects(getString(data, "projects", ""));
        p.setSpecialization(getString(data, "specialization", ""));
        p.setClubs(getString(data, "clubs", ""));
        p.setBio(getString(data, "bio", ""));
        p.setGithubUrl(getString(data, "githubUrl", ""));
        p.setLinkedinUrl(getString(data, "linkedinUrl", ""));
        p.setContactEmail(getString(data, "contactEmail", getString(data, "email", "")));

        if (data.containsKey("cgpa") && data.get("cgpa") != null) {
            try {
                p.setCgpa(Double.parseDouble(data.get("cgpa").toString()));
            } catch (Exception ignored) {
                p.setCgpa(3.84);
            }
        }
        if (data.containsKey("publicationsCount") && data.get("publicationsCount") != null) {
            try {
                p.setPublicationsCount(Integer.parseInt(data.get("publicationsCount").toString()));
            } catch (Exception ignored) {}
        }
        if (data.containsKey("isVerified")) {
            p.setVerified(Boolean.TRUE.equals(data.get("isVerified")));
        } else {
            p.setVerified(true);
        }
        p.setIsAvailableForMentorship(Boolean.TRUE.equals(data.get("isAvailableForMentorship")));
        p.setIsActivelyHiring(Boolean.TRUE.equals(data.get("isActivelyHiring")));

        return p;
    }

    private String resolveCurrentUid() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getUid() != null && !user.getUid().isEmpty()) {
                return user.getUid();
            }
        } catch (Throwable ignored) {}
        if (sessionManager != null) {
            String uid = sessionManager.getFirebaseUid();
            if (uid != null && !uid.isEmpty()) {
                return uid;
            }
        }
        return null;
    }

    private static String getString(Map<String, Object> map, String key, String defaultVal) {
        if (map != null && map.containsKey(key) && map.get(key) != null) {
            String val = map.get(key).toString().trim();
            if (!val.isEmpty()) return val;
        }
        return defaultVal;
    }

    private void syncProfileToRoom(ProfileResponse p) {
        if (p == null || db == null) return;

        executor.execute(() -> {
            try {
                String role = p.getRole() != null ? p.getRole() : sessionManager.getUserRole();
                String primaryId = p.getKuetId() != null ? p.getKuetId() : p.getEmail();
                if (primaryId == null || primaryId.isEmpty()) primaryId = sessionManager.getUserId();

                UserEntity existingUser = db.userDao().getUserById(primaryId);
                double availBal = existingUser != null ? existingUser.getAvailableBalance() : 18500.0;
                double escrowBal = existingUser != null ? existingUser.getEscrowBalance() : 3500.0;

                String rollOrBatch = p.getBatch() != null ? p.getBatch() : p.getKuetId();
                String desigOrCompany = p.getCurrentPosition() != null ? p.getCurrentPosition() : p.getCurrentCompany();

                UserEntity userEntity = new UserEntity(
                        primaryId,
                        role,
                        p.getName() != null ? p.getName() : "KUET Member",
                        p.getEmail() != null ? p.getEmail() : "",
                        p.getDepartment() != null ? p.getDepartment() : "CSE",
                        rollOrBatch != null ? rollOrBatch : "",
                        desigOrCompany != null ? desigOrCompany : "",
                        p.isVerified(),
                        availBal,
                        escrowBal
                );
                db.userDao().insertUser(userEntity);

                if ("ROLE_STUDENT".equalsIgnoreCase(role) || "STUDENT".equalsIgnoreCase(role)) {
                    String roll = p.getKuetId() != null ? p.getKuetId() : primaryId;
                    double cgpa = p.getCgpa() != null ? p.getCgpa() : 3.85;

                    StudentProfileEntity spe = new StudentProfileEntity(
                            roll,
                            p.getName(),
                            p.getDepartment(),
                            p.getBatch() != null ? p.getBatch() : "2023",
                            cgpa,
                            p.getSkills(),
                            p.getProjects(),
                            p.getGithubUrl(),
                            p.getLinkedinUrl(),
                            p.getSpecialization(),
                            p.getClubs(),
                            p.getBio()
                    );
                    db.profileDao().insertStudentProfile(spe);
                    if (sessionManager != null) {
                        sessionManager.updateUserDetails(p.getName(), p.getDepartment(), (float) cgpa);
                    }
                } else if ("ROLE_ALUMNI".equalsIgnoreCase(role) || "ALUMNI".equalsIgnoreCase(role)) {
                    AlumniProfileEntity existing = db.profileDao().getAlumniProfileByUserId(primaryId);
                    long entityId = existing != null ? existing.getId() : (p.getUserId() != null ? p.getUserId() : 0L);
                    AlumniProfileEntity ape = new AlumniProfileEntity(
                            entityId,
                            primaryId,
                            p.getName(),
                            p.getDepartment(),
                            p.getBatch(),
                            p.getCurrentCompany(),
                            p.getCurrentPosition(),
                            p.getCountry(),
                            p.getCity(),
                            p.isVerified(),
                            p.getIsAvailableForMentorship() != null && p.getIsAvailableForMentorship(),
                            p.getIsActivelyHiring() != null && p.getIsActivelyHiring(),
                            p.getSkills(),
                            p.getBio()
                    );
                    db.profileDao().insertAlumniProfile(ape);
                    if (sessionManager != null) {
                        sessionManager.updateUserDetails(p.getName(), p.getDepartment(), 0.0f);
                    }
                } else if ("ROLE_TEACHER".equalsIgnoreCase(role) || "TEACHER".equalsIgnoreCase(role) || "FACULTY".equalsIgnoreCase(role)) {
                    TeacherProfileEntity existing = db.profileDao().getTeacherProfileByUserId(primaryId);
                    long entityId = existing != null ? existing.getId() : (p.getUserId() != null ? p.getUserId() : 0L);
                    TeacherProfileEntity tpe = new TeacherProfileEntity(
                            entityId,
                            primaryId,
                            p.getName(),
                            p.getDesignation() != null ? p.getDesignation() : p.getCurrentPosition(),
                            p.getDepartment(),
                            p.getResearchFocus(),
                            p.getLabName(),
                            p.getPublicationsCount() != null ? p.getPublicationsCount() : 0,
                            p.getContactEmail() != null ? p.getContactEmail() : p.getEmail(),
                            p.getBio()
                    );
                    db.profileDao().insertTeacherProfile(tpe);
                    if (sessionManager != null) {
                        sessionManager.updateUserDetails(p.getName(), p.getDepartment(), 0.0f);
                    }
                }

                if (sessionManager != null) {
                    sessionManager.setVerified(p.isVerified());
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync profile to Room: " + e.getMessage());
            }
        });
    }

    private ProfileResponse loadCachedProfileFromRoom() {
        if (db == null || sessionManager == null) return null;
        try {
            String role = sessionManager.getUserRole();
            String userId = sessionManager.getUserId();

            ProfileResponse pr = new ProfileResponse();
            pr.setRole(role);
            pr.setVerified(sessionManager.isVerified());

            if ("STUDENT".equalsIgnoreCase(role) || "ROLE_STUDENT".equalsIgnoreCase(role)) {
                StudentProfileEntity s = db.profileDao().getStudentProfile(userId);
                if (s != null) {
                    pr.setKuetId(s.getRoll());
                    pr.setName(s.getName());
                    pr.setDepartment(s.getDepartment());
                    pr.setBatch(s.getBatch());
                    pr.setCgpa(s.getCgpa());
                    pr.setSkills(s.getSkills());
                    pr.setProjects(s.getProjects());
                    pr.setGithubUrl(s.getGithubUrl());
                    pr.setLinkedinUrl(s.getLinkedinUrl());
                    pr.setSpecialization(s.getSpecialization());
                    pr.setClubs(s.getClubs());
                    pr.setBio(s.getBio());
                    return pr;
                }
            } else if ("ALUMNI".equalsIgnoreCase(role) || "ROLE_ALUMNI".equalsIgnoreCase(role)) {
                AlumniProfileEntity a = db.profileDao().getAlumniProfileByUserId(userId);
                if (a != null) {
                    pr.setName(a.getName());
                    pr.setDepartment(a.getDepartment());
                    pr.setBatch(a.getBatch());
                    pr.setCurrentCompany(a.getCurrentCompany());
                    pr.setCurrentPosition(a.getCurrentPosition());
                    pr.setCountry(a.getCountry());
                    pr.setCity(a.getCity());
                    pr.setIsAvailableForMentorship(a.isAvailableForMentorship());
                    pr.setIsActivelyHiring(a.isActivelyHiring());
                    pr.setSkills(a.getSkills());
                    pr.setBio(a.getBio());
                    return pr;
                }
            } else if ("TEACHER".equalsIgnoreCase(role) || "FACULTY".equalsIgnoreCase(role) || "ROLE_TEACHER".equalsIgnoreCase(role)) {
                TeacherProfileEntity t = db.profileDao().getTeacherProfileByUserId(userId);
                if (t != null) {
                    pr.setName(t.getName());
                    pr.setDepartment(t.getDepartment());
                    pr.setDesignation(t.getDesignation());
                    pr.setResearchFocus(t.getResearchFocus());
                    pr.setLabName(t.getLabName());
                    pr.setPublicationsCount(t.getPublicationsCount());
                    pr.setContactEmail(t.getContactEmail());
                    pr.setBio(t.getBio());
                    return pr;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Room cache load failed: " + e.getMessage());
        }
        return null;
    }

    private void syncAlumniDirectoryToRoom(List<AlumniDirectoryResponse> remoteAlumni) {
        if (remoteAlumni == null || remoteAlumni.isEmpty() || db == null) return;
        executor.execute(() -> {
            try {
                List<AlumniProfileEntity> entities = new ArrayList<>();
                for (AlumniDirectoryResponse r : remoteAlumni) {
                    entities.add(new AlumniProfileEntity(
                            r.getUserId() != null ? r.getUserId() : 0L,
                            String.valueOf(r.getUserId()),
                            r.getName(),
                            r.getDepartment(),
                            r.getBatch(),
                            r.getCurrentCompany(),
                            r.getCurrentPosition(),
                            r.getCountry(),
                            r.getCity(),
                            r.isVerified(),
                            r.isAvailableForMentorship(),
                            r.isActivelyHiring(),
                            r.getSkills(),
                            r.getBio()
                    ));
                }
                db.profileDao().insertAlumniProfiles(entities);
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync alumni directory to Room: " + e.getMessage());
            }
        });
    }

    private PagedResponse<AlumniDirectoryResponse> loadCachedAlumniFromRoom() {
        if (db == null) return null;
        try {
            List<AlumniProfileEntity> entities = db.profileDao().getAllAlumniProfiles();
            if (entities == null || entities.isEmpty()) return null;

            List<AlumniDirectoryResponse> list = new ArrayList<>();
            for (AlumniProfileEntity e : entities) {
                AlumniDirectoryResponse adr = new AlumniDirectoryResponse();
                adr.setUserId(e.getId());
                adr.setName(e.getName());
                adr.setDepartment(e.getDepartment());
                adr.setBatch(e.getBatch());
                adr.setCurrentCompany(e.getCurrentCompany());
                adr.setCurrentPosition(e.getCurrentPosition());
                adr.setCountry(e.getCountry());
                adr.setCity(e.getCity());
                adr.setAvailableForMentorship(e.isAvailableForMentorship());
                adr.setActivelyHiring(e.isActivelyHiring());
                adr.setSkills(e.getSkills());
                adr.setBio(e.getBio());
                adr.setVerified(e.isVerified());
                list.add(adr);
            }

            PagedResponse<AlumniDirectoryResponse> pr = new PagedResponse<>();
            pr.setContent(list);
            pr.setPage(0);
            pr.setSize(list.size());
            pr.setTotalElements(list.size());
            pr.setTotalPages(1);
            pr.setLast(true);
            return pr;
        } catch (Exception e) {
            Log.w(TAG, "Failed to load cached alumni from Room: " + e.getMessage());
            return null;
        }
    }

    private <T> void postResult(Callback<T> callback, T result) {
        postExecutor.execute(() -> callback.onSuccess(result));
    }

    private <T> void postError(Callback<T> callback, String error) {
        postExecutor.execute(() -> callback.onError(error));
    }

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                Type type = new TypeToken<ApiResponse<Void>>() {}.getType();
                ApiResponse<Void> errorResp = gson.fromJson(errorBodyStr, type);
                if (errorResp != null && errorResp.getMessage() != null) {
                    return errorResp.getMessage();
                }
                ApiError apiError = gson.fromJson(errorBodyStr, ApiError.class);
                if (apiError != null && apiError.getMessage() != null) {
                    return apiError.getMessage();
                }
            }
        } catch (Exception ignored) {}
        return "Server error: HTTP " + response.code();
    }
}
