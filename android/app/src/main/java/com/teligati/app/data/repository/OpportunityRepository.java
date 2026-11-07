package com.teligati.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.database.AppDatabase;
import com.teligati.app.data.entity.*;
import com.teligati.app.network.ApiClient;
import com.teligati.app.network.OpportunityApiService;
import com.teligati.app.network.ResearchApiService;
import com.teligati.app.network.model.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class OpportunityRepository {

    private static volatile OpportunityRepository INSTANCE;

    private final OpportunityApiService apiService;
    private final ResearchApiService researchApiService;
    private final SessionManager sessionManager;
    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor;
    private final Executor postExecutor;

    private static final Map<Long, String> PAPER_DOC_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, String> PROPOSAL_DOC_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, String> JOB_DOC_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, String> APPLICATION_DOC_MAP = new ConcurrentHashMap<>();

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public OpportunityRepository(OpportunityApiService apiService, SessionManager sessionManager, AppDatabase db) {
        this(apiService, null, sessionManager, db, null, createDefaultPostExecutor());
    }

    public OpportunityRepository(OpportunityApiService apiService, ResearchApiService researchApiService, SessionManager sessionManager, AppDatabase db) {
        this(apiService, researchApiService, sessionManager, db, null, createDefaultPostExecutor());
    }

    public OpportunityRepository(OpportunityApiService apiService, ResearchApiService researchApiService, SessionManager sessionManager, AppDatabase db, Executor postExecutor) {
        this(apiService, researchApiService, sessionManager, db, null, postExecutor);
    }

    public OpportunityRepository(OpportunityApiService apiService, ResearchApiService researchApiService, SessionManager sessionManager, AppDatabase db, FirebaseFirestore firestore, Executor postExecutor) {
        this.apiService = apiService;
        this.researchApiService = researchApiService;
        this.sessionManager = sessionManager;
        this.db = db;
        this.firestore = firestore;
        this.executor = Executors.newFixedThreadPool(4);
        this.postExecutor = postExecutor != null ? postExecutor : createDefaultPostExecutor();
    }

    private static Executor createDefaultPostExecutor() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            return handler::post;
        }
        return Runnable::run;
    }

    public static OpportunityRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (OpportunityRepository.class) {
                if (INSTANCE == null) {
                    ApiClient apiClient = ApiClient.getInstance(context);
                    SessionManager sessionManager = SessionManager.getInstance(context);
                    AppDatabase db = AppDatabase.getInstance(context);
                    FirebaseFirestore firestore = null;
                    try {
                        firestore = FirebaseFirestore.getInstance();
                    } catch (Throwable ignored) {}
                    INSTANCE = new OpportunityRepository(apiClient.getOpportunityApiService(), apiClient.getResearchApiService(), sessionManager, db, firestore, createDefaultPostExecutor());
                }
            }
        }
        return INSTANCE;
    }

    public static void setInstanceForTesting(OpportunityRepository testRepository) {
        INSTANCE = testRepository;
    }

    // ==========================================
    // 1. Assistant Jobs & Professional Jobs Sync
    // ==========================================

    public void syncAssistantJobs(Callback<List<JobPostingEntity>> callback) {
        syncJobsByCategory("ASSISTANT_WORK", callback);
    }

    public void syncProfessionalJobs(Callback<List<JobPostingEntity>> callback) {
        syncJobsByCategory("FULLTIME_JOBS", callback);
    }

    public void syncResearchOpportunities(Callback<List<JobPostingEntity>> callback) {
        syncJobsByCategory("RESEARCH_COLLABORATION", callback);
    }

    public void syncAlumniJobs(String posterId, Callback<List<JobPostingEntity>> callback) {
        executor.execute(() -> {
            List<JobPostingEntity> cached;
            if (posterId == null || posterId.isEmpty() || "ALL".equalsIgnoreCase(posterId)) {
                cached = db.jobDao().getAllJobs();
            } else {
                cached = db.jobDao().getJobsByPoster(posterId);
            }
            postResult(callback, cached != null ? cached : new ArrayList<>());

            if (firestore != null) {
                String uid = resolveCurrentUid();
                String effectivePosterUid = (uid != null && !uid.isEmpty()) ? uid : posterId;
                Query query = firestore.collection("jobs");
                if (effectivePosterUid != null && !effectivePosterUid.isEmpty() && !"ALL".equalsIgnoreCase(effectivePosterUid)) {
                    query = query.whereEqualTo("postedByUid", effectivePosterUid);
                }
                query.get().addOnSuccessListener(snapshots -> executor.execute(() -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<JobPostingEntity> entities = new ArrayList<>();
                        for (QueryDocumentSnapshot snap : snapshots) {
                            Map<String, Object> data = snap.getData();
                            long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                            JOB_DOC_MAP.put(localId, snap.getId());

                            String title = getString(data, "title", "Untitled Job");
                            String cat = getString(data, "category", "ASSISTANT_WORK");
                            String org = getString(data, "organization", "KUET");
                            String loc = getString(data, "location", "Remote");
                            String posterName = getString(data, "postedByName", "Alumni / Faculty");
                            String posterRole = getString(data, "postedByRole", "ALUMNI");
                            String comp = getString(data, "compensation", "Negotiable");
                            String dur = getString(data, "duration", "Flexible");
                            String desc = getString(data, "description", "");
                            String skills = getString(data, "skillsRequired", "");
                            Double minCgpa = getDouble(data, "minCgpa", 0.0);
                            String pUid = getString(data, "postedByUid", snap.getId());
                            long createdAt = getLong(data, "createdAt", System.currentTimeMillis());

                            JobPostingEntity entity = new JobPostingEntity(
                                    localId, title, cat, org, loc, posterName, posterRole,
                                    comp, dur, desc, skills, minCgpa, false, pUid, createdAt
                            );
                            entities.add(entity);
                        }
                        if (!entities.isEmpty()) {
                            db.jobDao().insertJobs(entities);
                            List<JobPostingEntity> updated;
                            if (effectivePosterUid == null || "ALL".equalsIgnoreCase(effectivePosterUid)) {
                                updated = db.jobDao().getAllJobs();
                            } else {
                                updated = db.jobDao().getJobsByPoster(effectivePosterUid);
                            }
                            postResult(callback, updated != null && !updated.isEmpty() ? updated : entities);
                            return;
                        }
                    }
                    List<JobPostingEntity> current = (effectivePosterUid == null || "ALL".equalsIgnoreCase(effectivePosterUid))
                            ? db.jobDao().getAllJobs() : db.jobDao().getJobsByPoster(effectivePosterUid);
                    postResult(callback, current != null ? current : new ArrayList<>());
                })).addOnFailureListener(e -> {
                    // Offline or network error - cached data already served
                });
                return;
            }
        });
    }

    private void syncJobsByCategory(String category, Callback<List<JobPostingEntity>> callback) {
        executor.execute(() -> {
            // 1. Serve cached Room data first for immediate UI responsiveness
            List<JobPostingEntity> cached = db.jobDao().getJobsByCategory(category);
            postResult(callback, cached != null ? cached : new ArrayList<>());

            // 2. Fetch fresh data from Firestore if available
            if (firestore != null) {
                Query query = firestore.collection("jobs");
                if (category != null && !category.isEmpty() && !"ALL".equalsIgnoreCase(category)) {
                    query = query.whereEqualTo("category", category);
                }
                query.get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<JobPostingEntity> entities = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                                    JOB_DOC_MAP.put(localId, snap.getId());

                                    String title = getString(data, "title", "Untitled Job");
                                    String cat = getString(data, "category", category != null ? category : "ASSISTANT_WORK");
                                    String org = getString(data, "organization", "KUET");
                                    String loc = getString(data, "location", "Remote");
                                    String posterName = getString(data, "postedByName", "Alumni / Faculty");
                                    String posterRole = getString(data, "postedByRole", "ALUMNI");
                                    String comp = getString(data, "compensation", "Negotiable");
                                    String dur = getString(data, "duration", "Flexible");
                                    String desc = getString(data, "description", "");
                                    String skills = getString(data, "skillsRequired", "");
                                    Double minCgpa = getDouble(data, "minCgpa", 0.0);
                                    String posterUid = getString(data, "postedByUid", snap.getId());
                                    long createdAt = getLong(data, "createdAt", System.currentTimeMillis());

                                    JobPostingEntity entity = new JobPostingEntity(
                                            localId, title, cat, org, loc, posterName, posterRole,
                                            comp, dur, desc, skills, minCgpa, false, posterUid, createdAt
                                    );
                                    entities.add(entity);
                                }
                                if (!entities.isEmpty()) {
                                    db.jobDao().insertJobs(entities);
                                    List<JobPostingEntity> updated = db.jobDao().getJobsByCategory(category);
                                    postResult(callback, updated != null && !updated.isEmpty() ? updated : entities);
                                    return;
                                }
                            }
                            postResult(callback, db.jobDao().getJobsByCategory(category));
                        }))
                        .addOnFailureListener(e -> {
                            // Offline or network error - cached data already served
                        });
                return;
            }

            // Fallback for tests when firestore == null
            try {
                if (apiService != null) {
                    Response<ApiResponse<PagedResponse<JobNetworkDto>>> response =
                            apiService.getJobs(category, "OPEN", null, 0, 50).execute();
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        PagedResponse<JobNetworkDto> paged = response.body().getData();
                        if (paged != null && paged.getContent() != null) {
                            List<JobPostingEntity> entities = new ArrayList<>();
                            for (JobNetworkDto dto : paged.getContent()) {
                                JobPostingEntity entity = DataMapper.toJobEntity(dto);
                                if (entity != null) entities.add(entity);
                            }
                            if (!entities.isEmpty()) {
                                db.jobDao().insertJobs(entities);
                                postResult(callback, db.jobDao().getJobsByCategory(category));
                                return;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void createJobPosting(JobPostingEntity job, Callback<Long> callback) {
        executor.execute(() -> {
            String uid = resolveCurrentUid();
            String role = resolveCurrentRole();

            // Authorization check based on Firestore security rules
            if (!"ALUMNI".equalsIgnoreCase(role) && !"TEACHER".equalsIgnoreCase(role)) {
                postError(callback, "Unauthorized: Only Alumni and Teachers can create job postings.");
                return;
            }

            long createdAt = job.getCreatedAt() > 0 ? job.getCreatedAt() : System.currentTimeMillis();

            if (firestore != null) {
                DocumentReference docRef = firestore.collection("jobs").document();
                String docId = docRef.getId();
                long localId = parseOrGenerateLongId(docId, null);
                JOB_DOC_MAP.put(localId, docId);

                Map<String, Object> data = new HashMap<>();
                data.put("id", docId);
                data.put("localId", localId);
                data.put("title", job.getTitle() != null ? job.getTitle() : "");
                data.put("category", job.getCategory() != null ? job.getCategory() : "ASSISTANT_WORK");
                data.put("organization", job.getOrganization() != null ? job.getOrganization() : "KUET");
                data.put("location", job.getLocation() != null ? job.getLocation() : "Remote");
                data.put("postedByName", job.getPostedByName() != null ? job.getPostedByName() : (sessionManager != null ? sessionManager.getUserName() : "Poster"));
                data.put("postedByRole", role);
                data.put("postedByUid", uid != null ? uid : job.getPostedByUserId());
                data.put("compensation", job.getCompensation() != null ? job.getCompensation() : "");
                data.put("duration", job.getDuration() != null ? job.getDuration() : "");
                data.put("description", job.getDescription() != null ? job.getDescription() : "");
                data.put("skillsRequired", job.getSkillsRequired() != null ? job.getSkillsRequired() : "");
                data.put("minCgpa", job.getMinCgpa() != null ? job.getMinCgpa() : 0.0);
                data.put("createdAt", createdAt);
                data.put("updatedAt", createdAt);

                docRef.set(data)
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            job.setId(localId);
                            job.setPostedByUserId(uid != null ? uid : job.getPostedByUserId());
                            job.setPosterRole(role);
                            long id = db.jobDao().insertJob(job);
                            postResult(callback, id);
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            String err = e.getMessage() != null ? e.getMessage() : "Failed to create job in Firestore";
                            postError(callback, err);
                        }));
                return;
            }

            // Fallback for tests when firestore == null
            long id = db.jobDao().insertJob(job);
            postResult(callback, id);
        });
    }

    public void updateJobPosting(JobPostingEntity job, Callback<Boolean> callback) {
        executor.execute(() -> {
            String uid = resolveCurrentUid();

            if (firestore != null) {
                String docId = JOB_DOC_MAP.get(job.getId());
                if (docId != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("title", job.getTitle());
                    updates.put("organization", job.getOrganization());
                    updates.put("location", job.getLocation());
                    updates.put("compensation", job.getCompensation());
                    updates.put("duration", job.getDuration());
                    updates.put("description", job.getDescription());
                    updates.put("skillsRequired", job.getSkillsRequired());
                    updates.put("minCgpa", job.getMinCgpa() != null ? job.getMinCgpa() : 0.0);
                    updates.put("updatedAt", System.currentTimeMillis());

                    firestore.collection("jobs").document(docId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> executor.execute(() -> {
                                db.jobDao().updateJob(job);
                                postResult(callback, true);
                            }))
                            .addOnFailureListener(e -> executor.execute(() -> {
                                String err = e.getMessage() != null ? e.getMessage() : "Failed to update job in Firestore";
                                postError(callback, err);
                            }));
                    return;
                } else if (uid != null) {
                    firestore.collection("jobs")
                            .whereEqualTo("postedByUid", uid)
                            .get()
                            .addOnSuccessListener(snaps -> executor.execute(() -> {
                                DocumentSnapshot targetDoc = null;
                                for (DocumentSnapshot snap : snaps.getDocuments()) {
                                    if (snap.getString("title") != null && snap.getString("title").equalsIgnoreCase(job.getTitle())) {
                                        targetDoc = snap;
                                        break;
                                    }
                                }
                                if (targetDoc != null) {
                                    JOB_DOC_MAP.put(job.getId(), targetDoc.getId());
                                    targetDoc.getReference().update(
                                            "title", job.getTitle(),
                                            "organization", job.getOrganization(),
                                            "compensation", job.getCompensation(),
                                            "duration", job.getDuration(),
                                            "description", job.getDescription(),
                                            "updatedAt", System.currentTimeMillis()
                                    ).addOnSuccessListener(aVoid -> executor.execute(() -> {
                                        db.jobDao().updateJob(job);
                                        postResult(callback, true);
                                    })).addOnFailureListener(e -> executor.execute(() -> {
                                        postError(callback, e.getMessage());
                                    }));
                                } else {
                                    db.jobDao().updateJob(job);
                                    postResult(callback, true);
                                }
                            }))
                            .addOnFailureListener(e -> executor.execute(() -> {
                                db.jobDao().updateJob(job);
                                postResult(callback, true);
                            }));
                    return;
                }
            }

            db.jobDao().updateJob(job);
            postResult(callback, true);
        });
    }

    public void deleteJobPosting(long id, Callback<Boolean> callback) {
        executor.execute(() -> {
            JobPostingEntity job = db.jobDao().getJobById(id);
            String uid = resolveCurrentUid();

            if (firestore != null) {
                String docId = JOB_DOC_MAP.get(id);
                if (docId != null) {
                    firestore.collection("jobs").document(docId).delete()
                            .addOnSuccessListener(aVoid -> executor.execute(() -> {
                                db.applicationDao().deleteApplicationsForJob(id);
                                db.jobDao().deleteJob(id);
                                JOB_DOC_MAP.remove(id);
                                postResult(callback, true);
                            }))
                            .addOnFailureListener(e -> executor.execute(() -> {
                                String err = e.getMessage() != null ? e.getMessage() : "Failed to delete job from Firestore";
                                postError(callback, err);
                            }));
                    return;
                } else if (job != null && job.getPostedByUserId() != null) {
                    String posterUid = (uid != null) ? uid : job.getPostedByUserId();
                    firestore.collection("jobs")
                            .whereEqualTo("postedByUid", posterUid)
                            .get()
                            .addOnSuccessListener(snaps -> executor.execute(() -> {
                                for (DocumentSnapshot snap : snaps.getDocuments()) {
                                    if (snap.getString("title") != null && snap.getString("title").equalsIgnoreCase(job.getTitle())) {
                                        snap.getReference().delete();
                                        break;
                                    }
                                }
                                db.applicationDao().deleteApplicationsForJob(id);
                                db.jobDao().deleteJob(id);
                                postResult(callback, true);
                            }))
                            .addOnFailureListener(e -> executor.execute(() -> {
                                db.applicationDao().deleteApplicationsForJob(id);
                                db.jobDao().deleteJob(id);
                                postResult(callback, true);
                            }));
                    return;
                }
            }

            db.applicationDao().deleteApplicationsForJob(id);
            db.jobDao().deleteJob(id);
            postResult(callback, true);
        });
    }

    // ==========================================
    // 2. Applications Sync & Submission
    // ==========================================

    public void applyForJob(long jobId, String studentRoll, String studentName, String studentDept,
                            double cgpa, String skills, String statement, Callback<Boolean> callback) {
        executor.execute(() -> {
            // 1. Prevent duplicate local applications
            int exists = db.applicationDao().checkApplicationExists(jobId, studentRoll);
            if (exists > 0) {
                postResult(callback, false);
                return;
            }

            JobPostingEntity job = db.jobDao().getJobById(jobId);
            String title = (job != null) ? job.getTitle() : "Position #" + jobId;
            String posterUid = (job != null && job.getPostedByUserId() != null) ? job.getPostedByUserId() : "";
            String uid = resolveCurrentUid();
            String applicantUid = (uid != null && !uid.isEmpty()) ? uid : studentRoll;

            // Security: Applicant cannot apply to their own job
            if (applicantUid != null && applicantUid.equals(posterUid)) {
                postError(callback, "Cannot apply to your own job posting.");
                return;
            }

            long appliedAt = System.currentTimeMillis();

            if (firestore != null) {
                DocumentReference docRef = firestore.collection("job_applications").document();
                String docId = docRef.getId();
                long localId = parseOrGenerateLongId(docId, null);
                APPLICATION_DOC_MAP.put(localId, docId);

                String jobDocId = JOB_DOC_MAP.get(jobId);
                if (jobDocId == null) {
                    jobDocId = String.valueOf(jobId);
                }

                Map<String, Object> data = new HashMap<>();
                data.put("id", docId);
                data.put("localId", localId);
                data.put("jobId", jobDocId);
                data.put("jobLocalId", jobId);
                data.put("jobTitle", title);
                data.put("posterUid", posterUid);
                data.put("applicantUid", applicantUid);
                data.put("applicantRoll", studentRoll);
                data.put("applicantName", studentName);
                data.put("applicantDept", studentDept);
                data.put("applicantCgpa", cgpa);
                data.put("applicantSkills", skills != null ? skills : "");
                data.put("statement", statement != null ? statement : "");
                data.put("resumeUrl", "");
                data.put("posterNotes", "");
                data.put("status", "APPLIED");
                data.put("appliedAt", appliedAt);
                data.put("createdAt", appliedAt);
                data.put("updatedAt", appliedAt);

                docRef.set(data)
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            JobApplicationEntity app = new JobApplicationEntity(
                                    localId, jobId, title, applicantUid, studentName, studentRoll, studentDept,
                                    cgpa, skills, statement, "APPLIED", appliedAt
                            );
                            db.applicationDao().insertJobApplication(app);
                            db.jobDao().setJobApplied(jobId, true);
                            postResult(callback, true);
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            String err = e.getMessage() != null ? e.getMessage() : "Failed to submit job application to Firestore";
                            postError(callback, err);
                        }));
                return;
            }

            // Fallback for tests when firestore == null
            JobApplicationEntity app = new JobApplicationEntity(
                    0, jobId, title, studentRoll, studentName, studentRoll, studentDept,
                    cgpa, skills, statement, "APPLIED", appliedAt
            );
            db.applicationDao().insertJobApplication(app);
            db.jobDao().setJobApplied(jobId, true);
            postResult(callback, true);
        });
    }

    public void syncAlumniJobApplications(String alumniId, String alumniName, Callback<List<JobApplicationEntity>> callback) {
        executor.execute(() -> {
            List<JobApplicationEntity> cached = db.applicationDao().getJobApplicationsForAlumni(alumniId, alumniName);
            postResult(callback, cached != null ? cached : new ArrayList<>());

            String uid = resolveCurrentUid();
            String effectivePosterUid = (uid != null && !uid.isEmpty()) ? uid : alumniId;

            if (firestore != null) {
                firestore.collection("job_applications")
                        .whereEqualTo("posterUid", effectivePosterUid)
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<JobApplicationEntity> list = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                                    APPLICATION_DOC_MAP.put(localId, snap.getId());

                                    long jobId = getLong(data, "jobLocalId", parseOrGenerateLongId(getString(data, "jobId", "0"), null));
                                    String jobTitle = getString(data, "jobTitle", "Job #" + jobId);
                                    String applicantUid = getString(data, "applicantUid", "");
                                    String applicantRoll = getString(data, "applicantRoll", applicantUid);
                                    String applicantName = getString(data, "applicantName", "Applicant");
                                    String applicantDept = getString(data, "applicantDept", "Engineering");
                                    double cgpa = getDouble(data, "applicantCgpa", 0.0);
                                    String skills = getString(data, "applicantSkills", "");
                                    String statement = getString(data, "statement", "");
                                    String status = getString(data, "status", "APPLIED");
                                    long appliedAt = getLong(data, "appliedAt", System.currentTimeMillis());

                                    JobApplicationEntity app = new JobApplicationEntity(
                                            localId, jobId, jobTitle, applicantUid, applicantName, applicantRoll,
                                            applicantDept, cgpa, skills, statement, status, appliedAt
                                    );
                                    list.add(app);
                                }
                                if (!list.isEmpty()) {
                                    db.applicationDao().insertJobApplications(list);
                                    List<JobApplicationEntity> updated = db.applicationDao().getJobApplicationsForAlumni(alumniId, alumniName);
                                    postResult(callback, updated != null && !updated.isEmpty() ? updated : list);
                                    return;
                                }
                            }
                            postResult(callback, db.applicationDao().getJobApplicationsForAlumni(alumniId, alumniName));
                        }))
                        .addOnFailureListener(e -> {
                            // Offline or network error - cached data already served
                        });
                return;
            }
        });
    }

    public void syncStudentJobApplications(String studentRoll, Callback<List<JobApplicationEntity>> callback) {
        executor.execute(() -> {
            List<JobApplicationEntity> cached = db.applicationDao().getApplicationsByStudent(studentRoll);
            postResult(callback, cached != null ? cached : new ArrayList<>());

            String uid = resolveCurrentUid();
            String effectiveApplicantUid = (uid != null && !uid.isEmpty()) ? uid : studentRoll;

            if (firestore != null) {
                firestore.collection("job_applications")
                        .whereEqualTo("applicantUid", effectiveApplicantUid)
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<JobApplicationEntity> list = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                                    APPLICATION_DOC_MAP.put(localId, snap.getId());

                                    long jobId = getLong(data, "jobLocalId", parseOrGenerateLongId(getString(data, "jobId", "0"), null));
                                    String jobTitle = getString(data, "jobTitle", "Job #" + jobId);
                                    String applicantUid = getString(data, "applicantUid", effectiveApplicantUid);
                                    String applicantRoll = getString(data, "applicantRoll", studentRoll);
                                    String applicantName = getString(data, "applicantName", "Student");
                                    String applicantDept = getString(data, "applicantDept", "Engineering");
                                    double cgpa = getDouble(data, "applicantCgpa", 0.0);
                                    String skills = getString(data, "applicantSkills", "");
                                    String statement = getString(data, "statement", "");
                                    String status = getString(data, "status", "APPLIED");
                                    long appliedAt = getLong(data, "appliedAt", System.currentTimeMillis());

                                    JobApplicationEntity app = new JobApplicationEntity(
                                            localId, jobId, jobTitle, applicantUid, applicantName, applicantRoll,
                                            applicantDept, cgpa, skills, statement, status, appliedAt
                                    );
                                    list.add(app);
                                }
                                if (!list.isEmpty()) {
                                    db.applicationDao().insertJobApplications(list);
                                    List<JobApplicationEntity> updated = db.applicationDao().getApplicationsByStudent(studentRoll);
                                    postResult(callback, updated != null && !updated.isEmpty() ? updated : list);
                                    return;
                                }
                            }
                            postResult(callback, db.applicationDao().getApplicationsByStudent(studentRoll));
                        }))
                        .addOnFailureListener(e -> {
                            // Offline or network error - cached data already served
                        });
                return;
            }
        });
    }

    public void getApplicationsForJob(long jobId, Callback<List<JobApplicationEntity>> callback) {
        executor.execute(() -> {
            List<JobApplicationEntity> cached = db.applicationDao().getApplicationsForJob(jobId);
            postResult(callback, cached != null ? cached : new ArrayList<>());

            if (firestore != null) {
                String uid = resolveCurrentUid();
                String jobDocId = JOB_DOC_MAP.get(jobId);
                Query query = firestore.collection("job_applications");
                if (uid != null && !uid.isEmpty()) {
                    query = query.whereEqualTo("posterUid", uid);
                }
                if (jobDocId != null) {
                    query = query.whereEqualTo("jobId", jobDocId);
                }

                query.get().addOnSuccessListener(snapshots -> executor.execute(() -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<JobApplicationEntity> list = new ArrayList<>();
                        for (QueryDocumentSnapshot snap : snapshots) {
                            Map<String, Object> data = snap.getData();
                            long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                            APPLICATION_DOC_MAP.put(localId, snap.getId());

                            String jobTitle = getString(data, "jobTitle", "Job #" + jobId);
                            String applicantUid = getString(data, "applicantUid", "");
                            String applicantRoll = getString(data, "applicantRoll", applicantUid);
                            String applicantName = getString(data, "applicantName", "Applicant");
                            String applicantDept = getString(data, "applicantDept", "Engineering");
                            double cgpa = getDouble(data, "applicantCgpa", 0.0);
                            String skills = getString(data, "applicantSkills", "");
                            String statement = getString(data, "statement", "");
                            String status = getString(data, "status", "APPLIED");
                            long appliedAt = getLong(data, "appliedAt", System.currentTimeMillis());

                            JobApplicationEntity app = new JobApplicationEntity(
                                    localId, jobId, jobTitle, applicantUid, applicantName, applicantRoll,
                                    applicantDept, cgpa, skills, statement, status, appliedAt
                            );
                            list.add(app);
                        }
                        if (!list.isEmpty()) {
                            db.applicationDao().insertJobApplications(list);
                            List<JobApplicationEntity> updated = db.applicationDao().getApplicationsForJob(jobId);
                            postResult(callback, updated != null && !updated.isEmpty() ? updated : list);
                            return;
                        }
                    }
                    postResult(callback, db.applicationDao().getApplicationsForJob(jobId));
                })).addOnFailureListener(e -> {
                    // Offline or network error - cached data already served
                });
                return;
            }
        });
    }

    public void updateJobApplicationStatus(long id, String status, Callback<Boolean> callback) {
        updateJobApplicationStatus(id, status, "Decision updated", callback);
    }

    public void updateJobApplicationStatus(long id, String status, String posterNotes, Callback<Boolean> callback) {
        executor.execute(() -> {
            String normalizedStatus = normalizeApplicationStatus(status);
            JobApplicationEntity app = db.applicationDao().getApplicationById(id);

            if (firestore != null) {
                String docId = APPLICATION_DOC_MAP.get(id);
                if (docId != null) {
                    executeFirestoreApplicationDecision(docId, id, normalizedStatus, posterNotes, callback);
                    return;
                } else if (app != null) {
                    String posterUid = resolveCurrentUid();
                    Query query = firestore.collection("job_applications");
                    if (posterUid != null && !posterUid.isEmpty()) {
                        query = query.whereEqualTo("posterUid", posterUid);
                    }
                    query.get().addOnSuccessListener(snaps -> executor.execute(() -> {
                        if (snaps != null && !snaps.isEmpty()) {
                            for (DocumentSnapshot snap : snaps.getDocuments()) {
                                if (snap.getString("jobTitle") != null && snap.getString("jobTitle").equalsIgnoreCase(app.getJobTitle())) {
                                    APPLICATION_DOC_MAP.put(id, snap.getId());
                                    executeFirestoreApplicationDecision(snap.getId(), id, normalizedStatus, posterNotes, callback);
                                    return;
                                }
                            }
                        }
                        db.applicationDao().updateJobApplicationStatus(id, normalizedStatus);
                        postResult(callback, true);
                    })).addOnFailureListener(e -> executor.execute(() -> {
                        db.applicationDao().updateJobApplicationStatus(id, normalizedStatus);
                        postResult(callback, true);
                    }));
                    return;
                }
            }

            db.applicationDao().updateJobApplicationStatus(id, normalizedStatus);
            postResult(callback, true);
        });
    }

    private void executeFirestoreApplicationDecision(String docId, long localId, String normalizedStatus, String posterNotes, Callback<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", normalizedStatus);
        updates.put("posterNotes", posterNotes != null ? posterNotes : "");
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("job_applications").document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> executor.execute(() -> {
                    db.applicationDao().updateJobApplicationStatus(localId, normalizedStatus);
                    JobApplicationEntity app = db.applicationDao().getApplicationById(localId);
                    if (app != null && app.getApplicantId() != null) {
                        sendApplicationDecisionNotification(app.getApplicantId(), docId, normalizedStatus, app.getJobTitle(), posterNotes);
                    }
                    postResult(callback, true);
                }))
                .addOnFailureListener(e -> executor.execute(() -> {
                    String err = e.getMessage() != null ? e.getMessage() : "Failed to update application decision in Firestore";
                    postError(callback, err);
                }));
    }

    private void sendApplicationDecisionNotification(String applicantUid, String appDocId, String status, String jobTitle, String notes) {
        if (firestore == null || applicantUid == null || applicantUid.isEmpty()) return;
        String posterUid = resolveCurrentUid();
        if (posterUid == null) return;
        long now = System.currentTimeMillis();

        Map<String, Object> notif = new HashMap<>();
        notif.put("recipientUid", applicantUid);
        notif.put("senderUid", posterUid);
        notif.put("read", false);
        notif.put("title", "Application Status: " + status);
        notif.put("message", "Your application for " + (jobTitle != null ? jobTitle : "position") + " is marked as " + status + (notes != null && !notes.isEmpty() ? " — " + notes : ""));
        notif.put("type", "APPLICATION_DECISION");
        notif.put("referenceId", appDocId);
        notif.put("timestamp", now);
        notif.put("createdAt", now);

        firestore.collection("users").document(applicantUid)
                .collection("notifications").document().set(notif);
    }

    public void withdrawJobApplication(long id, Callback<Boolean> callback) {
        executor.execute(() -> {
            JobApplicationEntity app = db.applicationDao().getApplicationById(id);

            if (firestore != null) {
                String docId = APPLICATION_DOC_MAP.get(id);
                if (docId != null) {
                    executeFirestoreWithdrawal(docId, id, callback);
                    return;
                } else if (app != null) {
                    String applicantUid = resolveCurrentUid();
                    Query query = firestore.collection("job_applications");
                    if (applicantUid != null && !applicantUid.isEmpty()) {
                        query = query.whereEqualTo("applicantUid", applicantUid);
                    }
                    query.get().addOnSuccessListener(snaps -> executor.execute(() -> {
                        if (snaps != null && !snaps.isEmpty()) {
                            for (DocumentSnapshot snap : snaps.getDocuments()) {
                                if (snap.getString("jobTitle") != null && snap.getString("jobTitle").equalsIgnoreCase(app.getJobTitle())) {
                                    APPLICATION_DOC_MAP.put(id, snap.getId());
                                    executeFirestoreWithdrawal(snap.getId(), id, callback);
                                    return;
                                }
                            }
                        }
                        db.applicationDao().updateJobApplicationStatus(id, "WITHDRAWN");
                        postResult(callback, true);
                    })).addOnFailureListener(e -> executor.execute(() -> {
                        db.applicationDao().updateJobApplicationStatus(id, "WITHDRAWN");
                        postResult(callback, true);
                    }));
                    return;
                }
            }

            db.applicationDao().updateJobApplicationStatus(id, "WITHDRAWN");
            postResult(callback, true);
        });
    }

    private void executeFirestoreWithdrawal(String docId, long localId, Callback<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "WITHDRAWN");
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("job_applications").document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> executor.execute(() -> {
                    db.applicationDao().updateJobApplicationStatus(localId, "WITHDRAWN");
                    postResult(callback, true);
                }))
                .addOnFailureListener(e -> executor.execute(() -> {
                    String err = e.getMessage() != null ? e.getMessage() : "Failed to withdraw application in Firestore";
                    postError(callback, err);
                }));
    }

    // ==========================================
    // 3. Research Opportunities (Projects) Sync
    // ==========================================

    public void syncResearchPapers(Callback<List<ResearchPaperEntity>> callback) {
        executor.execute(() -> {
            // 1. Immediately serve cached Room papers
            List<ResearchPaperEntity> cached = db.researchDao().getAllPapers();
            postResult(callback, cached != null ? cached : new ArrayList<>());

            // 2. Fetch from Firestore if available
            if (firestore != null) {
                firestore.collection("research_papers")
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<ResearchPaperEntity> entities = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                                    PAPER_DOC_MAP.put(localId, snap.getId());

                                    String title = getString(data, "title", "Untitled Paper");
                                    String type = getString(data, "type", "Research Paper");
                                    String desc = getString(data, "description", "");
                                    String tech = getString(data, "technologies", "Computer Science");
                                    String ownerName = getString(data, "ownerName", "Faculty");
                                    String ownerDept = getString(data, "ownerDept", "Dept of CSE");
                                    String ownerUid = getString(data, "ownerUid", snap.getId());
                                    int contributors = getInt(data, "contributorsCount", 1);
                                    boolean open = getBoolean(data, "isOpenForContribution", true);
                                    String github = getString(data, "githubUrl", "https://github.com/kuet-wizards");
                                    String doi = getString(data, "paperDoi", "");
                                    long createdAt = getLong(data, "createdAt", System.currentTimeMillis());

                                    ResearchPaperEntity entity = new ResearchPaperEntity(
                                            localId, title, type, desc, tech, ownerName, ownerDept,
                                            ownerUid, contributors, open, github, doi, createdAt
                                    );
                                    entities.add(entity);
                                }
                                if (!entities.isEmpty()) {
                                    db.researchDao().insertPapers(entities);
                                    postResult(callback, db.researchDao().getAllPapers());
                                    return;
                                }
                            }
                            postResult(callback, db.researchDao().getAllPapers());
                        }))
                        .addOnFailureListener(e -> {
                            // Offline or network error - cached data already served
                        });
                return;
            }

            // Fallback for tests when firestore == null
            try {
                if (researchApiService != null) {
                    Response<ApiResponse<PagedResponse<ResearchPaperNetworkDto>>> pResp =
                            researchApiService.getPapers(null, null, 0, 50).execute();
                    if (pResp.isSuccessful() && pResp.body() != null && pResp.body().isSuccess()) {
                        PagedResponse<ResearchPaperNetworkDto> pPaged = pResp.body().getData();
                        if (pPaged != null && pPaged.getContent() != null && !pPaged.getContent().isEmpty()) {
                            List<ResearchPaperEntity> entities = DataMapper.toPaperEntityListFromPapers(pPaged.getContent());
                            if (!entities.isEmpty()) {
                                db.researchDao().insertPapers(entities);
                                postResult(callback, db.researchDao().getAllPapers());
                                return;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void publishResearchPaper(ResearchPaperEntity paper, Callback<Long> callback) {
        executor.execute(() -> {
            String uid = resolveCurrentUid();
            String ownerUid = (uid != null && !uid.isEmpty()) ? uid : paper.getOwnerId();

            if (firestore != null) {
                DocumentReference docRef = firestore.collection("research_papers").document();
                String docId = docRef.getId();
                long localId = parseOrGenerateLongId(docId, null);
                PAPER_DOC_MAP.put(localId, docId);

                Map<String, Object> data = new HashMap<>();
                data.put("ownerUid", ownerUid);
                data.put("title", paper.getTitle() != null ? paper.getTitle() : "");
                data.put("description", paper.getDescription() != null ? paper.getDescription() : "");
                data.put("technologies", paper.getTechnologies() != null ? paper.getTechnologies() : "Computer Science");
                data.put("ownerName", paper.getOwnerName() != null ? paper.getOwnerName() : (sessionManager != null ? sessionManager.getUserName() : "Faculty"));
                data.put("ownerDept", paper.getOwnerDept() != null ? paper.getOwnerDept() : "Dept of CSE");
                data.put("paperDoi", paper.getPaperDoi() != null ? paper.getPaperDoi() : "");
                data.put("githubUrl", paper.getGithubUrl() != null ? paper.getGithubUrl() : "https://github.com/kuet-wizards");
                data.put("type", paper.getType() != null ? paper.getType() : "Research Paper");
                data.put("isOpenForContribution", paper.isOpenForContribution());
                data.put("contributorsCount", paper.getContributorsCount());
                data.put("createdAt", paper.getCreatedAt() > 0 ? paper.getCreatedAt() : System.currentTimeMillis());
                data.put("localId", localId);
                data.put("id", docId);

                docRef.set(data)
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            paper.setId(localId);
                            paper.setOwnerId(ownerUid);
                            db.researchDao().insertPaper(paper);
                            postResult(callback, localId);
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            long localFallbackId = db.researchDao().insertPaper(paper);
                            postResult(callback, localFallbackId);
                        }));
                return;
            }

            // Fallback for tests when firestore == null
            long id = db.researchDao().insertPaper(paper);
            postResult(callback, id);
        });
    }

    public void updateResearchPaper(ResearchPaperEntity paper, Callback<Boolean> callback) {
        executor.execute(() -> {
            String uid = resolveCurrentUid();
            String ownerUid = (uid != null && !uid.isEmpty()) ? uid : paper.getOwnerId();

            if (firestore != null) {
                String docId = PAPER_DOC_MAP.get(paper.getId());
                if (docId != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("title", paper.getTitle());
                    updates.put("description", paper.getDescription());
                    updates.put("technologies", paper.getTechnologies());
                    updates.put("ownerDept", paper.getOwnerDept());
                    updates.put("paperDoi", paper.getPaperDoi());
                    updates.put("githubUrl", paper.getGithubUrl());
                    updates.put("type", paper.getType());
                    updates.put("isOpenForContribution", paper.isOpenForContribution());
                    updates.put("updatedAt", System.currentTimeMillis());

                    firestore.collection("research_papers").document(docId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> executor.execute(() -> {
                                db.researchDao().updatePaper(paper);
                                postResult(callback, true);
                            }))
                            .addOnFailureListener(e -> executor.execute(() -> {
                                db.researchDao().updatePaper(paper);
                                postResult(callback, true);
                            }));
                    return;
                } else if (ownerUid != null) {
                    firestore.collection("research_papers")
                            .whereEqualTo("ownerUid", ownerUid)
                            .get()
                            .addOnSuccessListener(snaps -> executor.execute(() -> {
                                for (QueryDocumentSnapshot snap : snaps) {
                                    if (snap.getString("title") != null && snap.getString("title").equalsIgnoreCase(paper.getTitle())) {
                                        PAPER_DOC_MAP.put(paper.getId(), snap.getId());
                                        snap.getReference().update(
                                                "title", paper.getTitle(),
                                                "description", paper.getDescription(),
                                                "technologies", paper.getTechnologies(),
                                                "updatedAt", System.currentTimeMillis()
                                        );
                                        break;
                                    }
                                }
                                db.researchDao().updatePaper(paper);
                                postResult(callback, true);
                            }))
                            .addOnFailureListener(e -> executor.execute(() -> {
                                db.researchDao().updatePaper(paper);
                                postResult(callback, true);
                            }));
                    return;
                }
            }

            db.researchDao().updatePaper(paper);
            postResult(callback, true);
        });
    }

    public void deleteResearchPaper(long id, Callback<Boolean> callback) {
        executor.execute(() -> {
            ResearchPaperEntity paper = db.researchDao().getPaperById(id);

            if (firestore != null) {
                String docId = PAPER_DOC_MAP.get(id);
                if (docId != null) {
                    firestore.collection("research_papers").document(docId).delete()
                            .addOnCompleteListener(task -> executor.execute(() -> {
                                db.researchDao().deleteApplicationsForOpportunity(id);
                                db.researchDao().deletePaper(id);
                                PAPER_DOC_MAP.remove(id);
                                postResult(callback, task.isSuccessful());
                            }));
                    return;
                } else if (paper != null && paper.getOwnerId() != null) {
                    firestore.collection("research_papers")
                            .whereEqualTo("ownerUid", paper.getOwnerId())
                            .get()
                            .addOnSuccessListener(snaps -> executor.execute(() -> {
                                for (QueryDocumentSnapshot snap : snaps) {
                                    if (snap.getString("title") != null && snap.getString("title").equalsIgnoreCase(paper.getTitle())) {
                                        snap.getReference().delete();
                                        break;
                                    }
                                }
                                db.researchDao().deleteApplicationsForOpportunity(id);
                                db.researchDao().deletePaper(id);
                                postResult(callback, true);
                            }))
                            .addOnFailureListener(e -> executor.execute(() -> {
                                db.researchDao().deleteApplicationsForOpportunity(id);
                                db.researchDao().deletePaper(id);
                                postResult(callback, true);
                            }));
                    return;
                }
            }

            db.researchDao().deleteApplicationsForOpportunity(id);
            db.researchDao().deletePaper(id);
            postResult(callback, true);
        });
    }

    public void expressInterestInResearch(long oppId, String oppTitle, String studentRoll, String studentName,
                                          String dept, double cgpa, String skills, String note, Callback<Boolean> callback) {
        executor.execute(() -> {
            ApplicationCreateRequest req = new ApplicationCreateRequest("PROJECT", null, oppId, note);
            try {
                Response<ApiResponse<ApplicationNetworkDto>> response = null;
                if (researchApiService != null) {
                    try {
                        response = researchApiService.submitApplication(oppId, req).execute();
                    } catch (Exception ignored) {}
                }
                if (response == null || !response.isSuccessful()) {
                    response = apiService.submitApplication(req).execute();
                }

                if (response != null && response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    ApplicationNetworkDto dto = response.body().getData();
                    ResearchApplicationEntity entity = DataMapper.toResearchApplicationEntity(dto);
                    if (entity != null) db.researchDao().insertResearchApplication(entity);
                    postResult(callback, true);
                    return;
                }
            } catch (Exception ignored) {
            }

            int count = db.researchDao().checkResearchApplicationExists(oppId, studentRoll);
            if (count > 0) {
                postResult(callback, false);
                return;
            }

            ResearchApplicationEntity app = new ResearchApplicationEntity(
                    0, oppId, oppTitle, studentRoll, studentName, dept, cgpa, skills, note, "Applicant", System.currentTimeMillis()
            );
            db.researchDao().insertResearchApplication(app);
            postResult(callback, true);
        });
    }

    // ==========================================
    // 4. Research Proposals Sync
    // ==========================================

    public void syncResearchProposals(Callback<List<ResearchProposalEntity>> callback) {
        String uid = resolveCurrentUid();
        String role = sessionManager != null ? sessionManager.getUserRole() : "STUDENT";
        if ("TEACHER".equalsIgnoreCase(role) || "FACULTY".equalsIgnoreCase(role)) {
            getTeacherProposals(uid, null, callback);
        } else {
            syncMyResearchProposals(uid, null, callback);
        }
    }

    public void syncMyResearchProposals(String studentRoll, String studentEmail, Callback<List<ResearchProposalEntity>> callback) {
        executor.execute(() -> {
            List<ResearchProposalEntity> cached = db.researchDao().getProposalsByStudent(studentRoll, studentEmail);
            postResult(callback, cached != null ? cached : new ArrayList<>());

            String uid = resolveCurrentUid();
            String effectiveStudentUid = (uid != null && !uid.isEmpty()) ? uid : studentRoll;

            if (firestore != null) {
                firestore.collection("research_proposals")
                        .whereEqualTo("studentUid", effectiveStudentUid)
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<ResearchProposalEntity> entities = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                                    PROPOSAL_DOC_MAP.put(localId, snap.getId());

                                    ResearchProposalEntity entity = new ResearchProposalEntity(
                                            localId,
                                            getString(data, "studentRoll", studentRoll),
                                            getString(data, "studentName", "Student"),
                                            getString(data, "topic", "Untitled Topic"),
                                            getString(data, "proposalAbstract", ""),
                                            getString(data, "degreeType", "Undergraduate Thesis"),
                                            getString(data, "status", "PENDING"),
                                            getString(data, "facultyUid", ""),
                                            getString(data, "feedback", "Under review"),
                                            getLong(data, "submittedAt", System.currentTimeMillis())
                                    );
                                    entities.add(entity);
                                }
                                if (!entities.isEmpty()) {
                                    db.researchDao().insertProposals(entities);
                                    postResult(callback, db.researchDao().getProposalsByStudent(studentRoll, studentEmail));
                                    return;
                                }
                            }
                            postResult(callback, db.researchDao().getProposalsByStudent(studentRoll, studentEmail));
                        }))
                        .addOnFailureListener(e -> {
                            // Offline or network error
                        });
                return;
            }

            // Fallback for tests when firestore == null
            List<ResearchProposalEntity> fallback = db.researchDao().getProposalsByStudent(studentRoll, studentEmail);
            postResult(callback, fallback != null ? fallback : new ArrayList<>());
        });
    }

    public void getTeacherProposals(String teacherId, String teacherEmail, Callback<List<ResearchProposalEntity>> callback) {
        executor.execute(() -> {
            List<ResearchProposalEntity> cached = db.researchDao().getProposalsForTeacher(teacherId, teacherEmail, teacherId);
            postResult(callback, cached != null ? cached : new ArrayList<>());

            String uid = resolveCurrentUid();
            String effectiveFacultyUid = (uid != null && !uid.isEmpty()) ? uid : teacherId;

            if (firestore != null) {
                firestore.collection("research_proposals")
                        .whereEqualTo("facultyUid", effectiveFacultyUid)
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<ResearchProposalEntity> entities = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                                    PROPOSAL_DOC_MAP.put(localId, snap.getId());

                                    ResearchProposalEntity entity = new ResearchProposalEntity(
                                            localId,
                                            getString(data, "studentRoll", ""),
                                            getString(data, "studentName", "Student"),
                                            getString(data, "topic", "Untitled Topic"),
                                            getString(data, "proposalAbstract", ""),
                                            getString(data, "degreeType", "Undergraduate Thesis"),
                                            getString(data, "status", "PENDING"),
                                            getString(data, "facultyUid", effectiveFacultyUid),
                                            getString(data, "feedback", "Under review"),
                                            getLong(data, "submittedAt", System.currentTimeMillis())
                                    );
                                    entities.add(entity);
                                }
                                if (!entities.isEmpty()) {
                                    db.researchDao().insertProposals(entities);
                                    List<ResearchProposalEntity> updated = db.researchDao().getProposalsForTeacher(teacherId, teacherEmail, teacherId);
                                    postResult(callback, updated != null && !updated.isEmpty() ? updated : entities);
                                    return;
                                }
                            }
                            postResult(callback, db.researchDao().getProposalsForTeacher(teacherId, teacherEmail, teacherId));
                        }))
                        .addOnFailureListener(e -> {
                            // Offline or network error
                        });
                return;
            }

            postResult(callback, cached != null ? cached : new ArrayList<>());
        });
    }

    public void submitResearchProposal(String studentRoll, String studentName, String topic,
                                       String proposalAbstract, String degreeType, String facultyUid,
                                       Callback<Boolean> callback) {
        executor.execute(() -> {
            String uid = resolveCurrentUid();
            String studentUid = (uid != null && !uid.isEmpty()) ? uid : studentRoll;

            if (studentUid != null && studentUid.equals(facultyUid)) {
                postError(callback, "Cannot submit research proposal to yourself as faculty supervisor.");
                return;
            }

            long submittedAt = System.currentTimeMillis();

            if (firestore != null) {
                DocumentReference docRef = firestore.collection("research_proposals").document();
                String docId = docRef.getId();
                long localId = parseOrGenerateLongId(docId, null);
                PROPOSAL_DOC_MAP.put(localId, docId);

                Map<String, Object> data = new HashMap<>();
                data.put("studentUid", studentUid);
                data.put("facultyUid", facultyUid != null ? facultyUid : "");
                data.put("studentRoll", studentRoll != null ? studentRoll : "");
                data.put("studentName", studentName != null ? studentName : "");
                data.put("topic", topic != null ? topic : "");
                data.put("proposalAbstract", proposalAbstract != null ? proposalAbstract : "");
                data.put("degreeType", degreeType != null ? degreeType : "Undergraduate Thesis");
                data.put("status", "PENDING"); // Enforced exact uppercase for security rules
                data.put("feedback", "Under review");
                data.put("submittedAt", submittedAt);
                data.put("updatedAt", submittedAt);
                data.put("localId", localId);
                data.put("id", docId);

                docRef.set(data)
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            ResearchProposalEntity prop = new ResearchProposalEntity(
                                    localId, studentRoll, studentName, topic, proposalAbstract, degreeType,
                                    "PENDING", facultyUid, "Under review", submittedAt
                            );
                            db.researchDao().insertProposal(prop);
                            postResult(callback, true);
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            String err = e.getMessage() != null ? e.getMessage() : "Failed to submit proposal to Firestore";
                            postError(callback, err);
                        }));
                return;
            }

            // Fallback for tests when firestore == null
            ResearchProposalEntity prop = new ResearchProposalEntity(
                    0, studentRoll, studentName, topic, proposalAbstract, degreeType,
                    "PENDING", facultyUid, "Under review", submittedAt
            );
            db.researchDao().insertProposal(prop);
            postResult(callback, true);
        });
    }

    public void submitResearchProposal(String studentRoll, String studentName, String topic,
                                       String proposalAbstract, String degreeType, long facultyId,
                                       Callback<Boolean> callback) {
        submitResearchProposal(studentRoll, studentName, topic, proposalAbstract, degreeType, String.valueOf(facultyId), callback);
    }

    public void decideResearchProposal(long id, String status, String feedback, Callback<Boolean> callback) {
        executor.execute(() -> {
            String normalizedStatus = normalizeProposalStatus(status);
            ResearchProposalEntity prop = db.researchDao().getProposalById(id);

            if (firestore != null) {
                String docId = PROPOSAL_DOC_MAP.get(id);
                if (docId != null) {
                    executeFirestoreDecision(docId, id, normalizedStatus, feedback, callback);
                    return;
                } else if (prop != null) {
                    String facultyUid = resolveCurrentUid();
                    Query query = firestore.collection("research_proposals")
                            .whereEqualTo("topic", prop.getTopic());
                    if (facultyUid != null) {
                        query = query.whereEqualTo("facultyUid", facultyUid);
                    }
                    query.get().addOnSuccessListener(snaps -> executor.execute(() -> {
                        if (snaps != null && !snaps.isEmpty()) {
                            DocumentSnapshot snap = snaps.getDocuments().get(0);
                            PROPOSAL_DOC_MAP.put(id, snap.getId());
                            executeFirestoreDecision(snap.getId(), id, normalizedStatus, feedback, callback);
                        } else {
                            db.researchDao().updateProposalStatus(id, normalizedStatus, feedback);
                            postResult(callback, true);
                        }
                    })).addOnFailureListener(e -> executor.execute(() -> {
                        db.researchDao().updateProposalStatus(id, normalizedStatus, feedback);
                        postResult(callback, true);
                    }));
                    return;
                }
            }

            db.researchDao().updateProposalStatus(id, normalizedStatus, feedback);
            postResult(callback, true);
        });
    }

    private void executeFirestoreDecision(String docId, long localId, String normalizedStatus, String feedback, Callback<Boolean> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", normalizedStatus);
        updates.put("feedback", feedback != null ? feedback : "");
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("research_proposals").document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> executor.execute(() -> {
                    db.researchDao().updateProposalStatus(localId, normalizedStatus, feedback);
                    ResearchProposalEntity prop = db.researchDao().getProposalById(localId);
                    if (prop != null && prop.getStudentRoll() != null) {
                        sendProposalDecisionNotification(prop.getStudentRoll(), docId, normalizedStatus, prop.getTopic(), feedback);
                    }
                    postResult(callback, true);
                }))
                .addOnFailureListener(e -> executor.execute(() -> {
                    String err = e.getMessage() != null ? e.getMessage() : "Failed to record proposal decision in Firestore";
                    postError(callback, err);
                }));
    }

    private void sendProposalDecisionNotification(String studentUid, String propDocId, String status, String topic, String feedback) {
        if (firestore == null || studentUid == null || studentUid.isEmpty()) return;
        String facultyUid = resolveCurrentUid();
        if (facultyUid == null) return;
        long now = System.currentTimeMillis();

        Map<String, Object> notif = new HashMap<>();
        notif.put("recipientUid", studentUid);
        notif.put("senderUid", facultyUid);
        notif.put("read", false);
        notif.put("title", "Research Proposal: " + status);
        notif.put("message", "Your proposal for '" + (topic != null ? topic : "Research Proposal") + "' is marked as " + status + (feedback != null && !feedback.isEmpty() ? " — " + feedback : ""));
        notif.put("type", "PROPOSAL_DECISION");
        notif.put("referenceId", propDocId);
        notif.put("timestamp", now);
        notif.put("createdAt", now);

        firestore.collection("users").document(studentUid)
                .collection("notifications").document().set(notif);
    }

    private String normalizeProposalStatus(String status) {
        if (status == null) return "PENDING";
        String s = status.trim().toUpperCase(Locale.US);
        if (s.contains("ACCEPT")) return "ACCEPTED";
        if (s.contains("REVISION")) return "REVISION_REQUESTED";
        if (s.contains("REJECT") || s.contains("DECLINE")) return "REJECTED";
        if (s.contains("PEND")) return "PENDING";
        return s;
    }

    private String resolveCurrentRole() {
        if (sessionManager != null) {
            String role = sessionManager.getUserRole();
            if (role != null && !role.isEmpty()) {
                String upper = role.trim().toUpperCase(Locale.US);
                if (upper.contains("TEACHER") || upper.contains("FACULTY")) return "TEACHER";
                if (upper.contains("ALUMNI")) return "ALUMNI";
                if (upper.contains("STUDENT")) return "STUDENT";
                return upper;
            }
        }
        return "ALUMNI";
    }

    public static String normalizeApplicationStatus(String status) {
        if (status == null) return "PENDING";
        String s = status.trim().toUpperCase(Locale.US);
        if (s.contains("ACCEPT")) return "ACCEPTED";
        if (s.contains("SHORTLIST") || s.contains("REVIEW")) return "UNDER_REVIEW";
        if (s.contains("REJECT") || s.contains("DECLINE")) return "REJECTED";
        if (s.contains("WITHDRAW")) return "WITHDRAWN";
        if (s.contains("APPLI") || s.contains("PEND")) return "PENDING";
        return s;
    }

    private static Double getDouble(Map<String, Object> data, String key, Double defaultVal) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return defaultVal;
        Object v = data.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return defaultVal; }
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
            String legacyId = sessionManager.getUserId();
            if (legacyId != null && !legacyId.isEmpty()) {
                return legacyId;
            }
        }
        return null;
    }

    private static long parseOrGenerateLongId(String docId, Object localIdObj) {
        if (localIdObj instanceof Number) {
            return ((Number) localIdObj).longValue();
        }
        if (docId != null) {
            try {
                if (docId.startsWith("paper_") || docId.startsWith("prop_")) {
                    return Long.parseLong(docId.substring(docId.indexOf('_') + 1));
                }
                return Long.parseLong(docId);
            } catch (Exception ignored) {}
            return Math.abs(docId.hashCode()) + 1L;
        }
        return System.currentTimeMillis();
    }

    private static String getString(Map<String, Object> data, String key, String defaultVal) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return defaultVal;
        return String.valueOf(data.get(key));
    }

    private static int getInt(Map<String, Object> data, String key, int defaultVal) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return defaultVal;
        Object v = data.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return defaultVal; }
    }

    private static long getLong(Map<String, Object> data, String key, long defaultVal) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return defaultVal;
        Object v = data.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return defaultVal; }
    }

    private static boolean getBoolean(Map<String, Object> data, String key, boolean defaultVal) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return defaultVal;
        Object v = data.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    public void closeResearchOpportunity(long oppId, Callback<Boolean> callback) {
        executor.execute(() -> {
            if (researchApiService != null) {
                try {
                    researchApiService.closeOpportunity(oppId).execute();
                } catch (Exception ignored) {}
            }
            db.jobDao().deleteJob(oppId);
            postResult(callback, true);
        });
    }

    public void reviewResearchApplication(long appId, String status, String feedback, Callback<Boolean> callback) {
        executor.execute(() -> {
            String normalizedStatus = status != null ? status.toUpperCase().trim() : "PENDING";
            if ("DECLINED".equals(normalizedStatus) || "REJECT".equals(normalizedStatus)) {
                normalizedStatus = "REJECTED";
            } else if ("ACCEPT".equals(normalizedStatus)) {
                normalizedStatus = "ACCEPTED";
            } else if ("REVISION".equals(normalizedStatus) || "REVISION_REQUESTED".equals(normalizedStatus)) {
                normalizedStatus = "REVISION_REQUESTED";
            }

            if (researchApiService != null) {
                try {
                    researchApiService.reviewApplication(appId, new ApplicationStatusRequest(normalizedStatus, feedback)).execute();
                } catch (Exception ignored) {}
            }
            db.researchDao().updateResearchApplicationStatus(appId, normalizedStatus);
            postResult(callback, true);
        });
    }

    public void resubmitResearchApplication(long appId, String statement, String skills, Callback<Boolean> callback) {
        executor.execute(() -> {
            if (researchApiService != null) {
                try {
                    researchApiService.resubmitApplication(appId, new ApplicationCreateRequest("PROJECT", null, null, statement)).execute();
                } catch (Exception ignored) {}
            }
            db.researchDao().updateResearchApplicationStatus(appId, "PENDING");
            postResult(callback, true);
        });
    }

    public void withdrawResearchApplication(long appId, Callback<Boolean> callback) {
        executor.execute(() -> {
            if (researchApiService != null) {
                try {
                    researchApiService.withdrawApplication(appId).execute();
                } catch (Exception ignored) {}
            }
            db.researchDao().updateResearchApplicationStatus(appId, "WITHDRAWN");
            postResult(callback, true);
        });
    }

    private String parseErrorMessage(Response<?> response) {
        if (response == null) return "Unknown network error";
        try {
            if (response.errorBody() != null) {
                String errorBodyStr = response.errorBody().string();
                Gson gson = new Gson();
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

    private <T> void postResult(Callback<T> callback, T result) {
        if (callback != null) {
            postExecutor.execute(() -> callback.onSuccess(result));
        }
    }

    private <T> void postError(Callback<T> callback, String error) {
        if (callback != null) {
            postExecutor.execute(() -> callback.onError(error != null ? error : "Operation failed"));
        }
    }
}
