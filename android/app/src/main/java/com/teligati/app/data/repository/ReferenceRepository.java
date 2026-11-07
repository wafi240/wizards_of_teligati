package com.teligati.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.database.AppDatabase;
import com.teligati.app.data.entity.NotificationEntity;
import com.teligati.app.data.entity.ReferenceRequestEntity;
import com.teligati.app.network.ApiClient;
import com.teligati.app.network.ReferenceApiService;
import com.teligati.app.network.model.ReferenceCreateRequest;
import com.teligati.app.network.model.ReferenceDecisionNetworkRequest;
import com.teligati.app.network.model.ReferenceNetworkDto;
import com.teligati.app.network.model.RecommendationNetworkRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class ReferenceRepository {

    private static volatile ReferenceRepository INSTANCE;

    private final ReferenceApiService apiService;
    private final SessionManager sessionManager;
    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor;
    private final Executor postExecutor;

    private static final Map<Long, String> REF_DOC_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Long> REF_ID_MAP = new ConcurrentHashMap<>();
    private static final AtomicLong ID_GEN = new AtomicLong(1000L);

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public ReferenceRepository(ReferenceApiService apiService, SessionManager sessionManager, AppDatabase db) {
        this(apiService, sessionManager, db, null, createDefaultPostExecutor());
    }

    public ReferenceRepository(ReferenceApiService apiService, SessionManager sessionManager, AppDatabase db, Executor postExecutor) {
        this(apiService, sessionManager, db, null, postExecutor);
    }

    public ReferenceRepository(ReferenceApiService apiService, SessionManager sessionManager, AppDatabase db, FirebaseFirestore firestore, Executor postExecutor) {
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.db = db;
        this.firestore = firestore;
        this.executor = Executors.newFixedThreadPool(4);
        this.postExecutor = postExecutor != null ? postExecutor : createDefaultPostExecutor();
    }

    public static ReferenceRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ReferenceRepository.class) {
                if (INSTANCE == null) {
                    Context appCtx = context.getApplicationContext();
                    FirebaseFirestore fs = null;
                    try {
                        fs = FirebaseFirestore.getInstance();
                    } catch (Throwable ignored) {}

                    INSTANCE = new ReferenceRepository(
                            ApiClient.getInstance(appCtx).getReferenceApiService(),
                            SessionManager.getInstance(appCtx),
                            AppDatabase.getInstance(appCtx),
                            fs,
                            createDefaultPostExecutor()
                    );
                }
            }
        }
        return INSTANCE;
    }

    public static void setInstanceForTesting(ReferenceRepository testInstance) {
        INSTANCE = testInstance;
    }

    private static Executor createDefaultPostExecutor() {
        if (Looper.myLooper() != null) {
            Handler handler = new Handler(Looper.getMainLooper());
            return handler::post;
        }
        return Runnable::run;
    }

    private <T> void dispatchSuccess(Callback<T> callback, T result) {
        if (callback != null) {
            postExecutor.execute(() -> callback.onSuccess(result));
        }
    }

    private <T> void dispatchError(Callback<T> callback, String error) {
        if (callback != null) {
            postExecutor.execute(() -> callback.onError(error));
        }
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
            return sessionManager.getUserId();
        }
        return "student_1";
    }

    private long parseOrGenerateLongId(String docId, Long existingId) {
        if (existingId != null && existingId > 0) {
            REF_DOC_MAP.put(existingId, docId);
            REF_ID_MAP.put(docId, existingId);
            return existingId;
        }
        if (REF_ID_MAP.containsKey(docId)) {
            return REF_ID_MAP.get(docId);
        }
        long newId = ID_GEN.incrementAndGet();
        REF_DOC_MAP.put(newId, docId);
        REF_ID_MAP.put(docId, newId);
        return newId;
    }

    // 1. Submit Reference Request (Student -> Alumni Referee)
    public void submitReferenceRequest(ReferenceCreateRequest request, Callback<ReferenceNetworkDto> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            String myName = sessionManager != null ? sessionManager.getUserName() : "Student";
            String myRoll = sessionManager != null ? sessionManager.getUserId() : myUid;
            String myDept = sessionManager != null ? sessionManager.getUserDept() : "CSE";

            String alumniUid = request.getRecipientUid() != null && !request.getRecipientUid().isEmpty()
                    ? request.getRecipientUid()
                    : (request.getRecipientId() != null && request.getRecipientId() > 0 ? String.valueOf(request.getRecipientId()) : "alumni_1");

            long now = System.currentTimeMillis();

            if (firestore != null) {
                DocumentReference docRef = firestore.collection("reference_requests").document();
                String docId = docRef.getId();
                long localId = parseOrGenerateLongId(docId, null);

                Map<String, Object> data = new HashMap<>();
                data.put("studentUid", myUid);
                data.put("studentName", myName);
                data.put("studentRoll", myRoll);
                data.put("studentDept", myDept);
                data.put("alumniUid", alumniUid);
                data.put("alumniName", "Alumni Referee");
                data.put("targetCompany", request.getTargetInstitution() != null ? request.getTargetInstitution() : "Target Institution");
                data.put("targetInstitution", request.getTargetInstitution() != null ? request.getTargetInstitution() : "Target Institution");
                data.put("targetProgram", request.getTargetProgram() != null ? request.getTargetProgram() : "");
                data.put("purpose", request.getPurpose() != null ? request.getPurpose() : "JOB_REFERRAL");
                data.put("message", request.getPersonalMessage() != null ? request.getPersonalMessage() : "Please recommend me.");
                data.put("personalMessage", request.getPersonalMessage() != null ? request.getPersonalMessage() : "Please recommend me.");
                data.put("deadline", request.getDeadline() != null ? request.getDeadline() : "");
                data.put("status", "PENDING");
                data.put("createdAt", now);
                data.put("requestedAt", now);
                data.put("updatedAt", now);

                docRef.set(data).addOnSuccessListener(aVoid -> executor.execute(() -> {
                    // Cache in Room
                    ReferenceRequestEntity entity = new ReferenceRequestEntity(
                            localId, myRoll, myName, myDept,
                            request.getTargetInstitution(), request.getPersonalMessage(), "PENDING",
                            alumniUid, now, "ROLE_ALUMNI", "Alumni Referee",
                            request.getPurpose(), request.getTargetProgram(), request.getDeadline(), null, null
                    );
                    db.referenceRequestDao().insertRequest(entity);

                    ReferenceNetworkDto dto = new ReferenceNetworkDto();
                    dto.setId(localId);
                    dto.setStudentRoll(myRoll);
                    dto.setStudentName(myName);
                    dto.setStudentDept(myDept);
                    dto.setTargetInstitution(request.getTargetInstitution());
                    dto.setPersonalMessage(request.getPersonalMessage());
                    dto.setStatus("PENDING");
                    dto.setRecipientId(localId);

                    dispatchSuccess(callback, dto);
                })).addOnFailureListener(e -> executor.execute(() -> {
                    // Fall back to Room local insert
                    ReferenceRequestEntity localEntity = new ReferenceRequestEntity(
                            localId, myRoll, myName, myDept,
                            request.getTargetInstitution(), request.getPersonalMessage(), "PENDING",
                            alumniUid, now, "ROLE_ALUMNI", "Alumni Referee",
                            request.getPurpose(), request.getTargetProgram(), request.getDeadline(), null, null
                    );
                    db.referenceRequestDao().insertRequest(localEntity);

                    ReferenceNetworkDto fallbackDto = new ReferenceNetworkDto();
                    fallbackDto.setId(localId);
                    fallbackDto.setTargetInstitution(request.getTargetInstitution());
                    fallbackDto.setStatus("PENDING");
                    fallbackDto.setPersonalMessage(request.getPersonalMessage());
                    dispatchSuccess(callback, fallbackDto);
                }));
            } else {
                long localId = ID_GEN.incrementAndGet();
                ReferenceRequestEntity localEntity = new ReferenceRequestEntity(
                        localId, myRoll, myName, myDept,
                        request.getTargetInstitution(), request.getPersonalMessage(), "PENDING",
                        alumniUid, now, "ROLE_ALUMNI", "Alumni Referee",
                        request.getPurpose(), request.getTargetProgram(), request.getDeadline(), null, null
                );
                db.referenceRequestDao().insertRequest(localEntity);

                ReferenceNetworkDto fallbackDto = new ReferenceNetworkDto();
                fallbackDto.setId(localId);
                fallbackDto.setTargetInstitution(request.getTargetInstitution());
                fallbackDto.setStatus("PENDING");
                fallbackDto.setPersonalMessage(request.getPersonalMessage());
                dispatchSuccess(callback, fallbackDto);
            }
        });
    }

    // 2. Get Sent Requests (Student)
    public void getSentRequests(Callback<List<ReferenceRequestEntity>> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            String myRoll = sessionManager != null ? sessionManager.getUserId() : myUid;

            // Serve Room cache immediately
            List<ReferenceRequestEntity> cached = db.referenceRequestDao().getRequestsByStudent(myRoll);
            if (cached != null && !cached.isEmpty()) {
                dispatchSuccess(callback, cached);
            }

            if (firestore != null) {
                firestore.collection("reference_requests")
                        .whereEqualTo("studentUid", myUid)
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<ReferenceRequestEntity> entities = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    String docId = snap.getId();
                                    long localId = parseOrGenerateLongId(docId, null);

                                    ReferenceRequestEntity entity = parseReferenceRequestEntity(docId, localId, data);
                                    entities.add(entity);
                                }
                                if (!entities.isEmpty()) {
                                    db.referenceRequestDao().insertRequests(entities);
                                    dispatchSuccess(callback, entities);
                                    return;
                                }
                            }
                            List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsByStudent(myRoll);
                            dispatchSuccess(callback, list != null ? list : new ArrayList<>());
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsByStudent(myRoll);
                            dispatchSuccess(callback, list != null ? list : new ArrayList<>());
                        }));
            } else {
                List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsByStudent(myRoll);
                dispatchSuccess(callback, list != null ? list : new ArrayList<>());
            }
        });
    }

    // 3. Get Incoming Requests (Referee: Alumni)
    public void getIncomingRequests(String status, Callback<List<ReferenceRequestEntity>> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            String myName = sessionManager != null ? sessionManager.getUserName() : "Alumni";

            // Serve Room cache immediately
            List<ReferenceRequestEntity> cached = db.referenceRequestDao().getRequestsForAlumni(myUid, myName);
            if (cached == null || cached.isEmpty()) {
                cached = db.referenceRequestDao().getAllRequests();
            }
            if (cached != null && !cached.isEmpty()) {
                dispatchSuccess(callback, cached);
            }

            if (firestore != null) {
                Query q = firestore.collection("reference_requests").whereEqualTo("alumniUid", myUid);
                if (status != null && !status.isEmpty()) {
                    q = q.whereEqualTo("status", status);
                }

                q.get().addOnSuccessListener(snapshots -> executor.execute(() -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<ReferenceRequestEntity> entities = new ArrayList<>();
                        for (QueryDocumentSnapshot snap : snapshots) {
                            Map<String, Object> data = snap.getData();
                            String docId = snap.getId();
                            long localId = parseOrGenerateLongId(docId, null);

                            ReferenceRequestEntity entity = parseReferenceRequestEntity(docId, localId, data);
                            entities.add(entity);
                        }
                        if (!entities.isEmpty()) {
                            db.referenceRequestDao().insertRequests(entities);
                            dispatchSuccess(callback, entities);
                            return;
                        }
                    }
                    List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsForAlumni(myUid, myName);
                    if (list == null || list.isEmpty()) {
                        list = db.referenceRequestDao().getAllRequests();
                    }
                    dispatchSuccess(callback, list != null ? list : new ArrayList<>());
                })).addOnFailureListener(e -> executor.execute(() -> {
                    List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsForAlumni(myUid, myName);
                    if (list == null || list.isEmpty()) {
                        list = db.referenceRequestDao().getAllRequests();
                    }
                    dispatchSuccess(callback, list != null ? list : new ArrayList<>());
                }));
            } else {
                List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsForAlumni(myUid, myName);
                if (list == null || list.isEmpty()) {
                    list = db.referenceRequestDao().getAllRequests();
                }
                dispatchSuccess(callback, list != null ? list : new ArrayList<>());
            }
        });
    }

    // 4. Accept / Decline Decision (Referee)
    public void decideRequest(long id, String decision, String notes, Callback<ReferenceNetworkDto> callback) {
        executor.execute(() -> {
            String docId = REF_DOC_MAP.get(id);
            String upper = decision != null ? decision.toUpperCase().trim() : "REJECTED";
            final String normalized = upper.contains("ACCEPT") || upper.contains("ENDORSE") ? "ACCEPTED" : "REJECTED";
            long now = System.currentTimeMillis();

            if (firestore != null && docId != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", normalized);
                updates.put("responseNotes", notes != null ? notes : "Decision updated by referee");
                updates.put("updatedAt", now);

                firestore.collection("reference_requests").document(docId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            db.referenceRequestDao().updateRequestStatus(id, normalized);
                            ReferenceRequestEntity req = db.referenceRequestDao().getRequestById(id);

                            if (req != null) {
                                sendReferenceDecisionNotification(req.getStudentRoll(), docId, "Reference Request: " + normalized,
                                        "Your reference request for " + req.getTargetCompany() + " was marked as: " + normalized);
                            }

                            ReferenceNetworkDto dto = new ReferenceNetworkDto();
                            dto.setId(id);
                            dto.setStatus(normalized);
                            dto.setResponseNotes(notes);
                            dispatchSuccess(callback, dto);
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            db.referenceRequestDao().updateRequestStatus(id, normalized);
                            ReferenceNetworkDto fallback = new ReferenceNetworkDto();
                            fallback.setId(id);
                            fallback.setStatus(normalized);
                            dispatchSuccess(callback, fallback);
                        }));
            } else {
                db.referenceRequestDao().updateRequestStatus(id, normalized);
                ReferenceRequestEntity req = db.referenceRequestDao().getRequestById(id);
                if (req != null) {
                    db.communicationDao().insertNotification(new NotificationEntity(
                            0, req.getStudentRoll(), "Reference Request: " + normalized,
                            "Your reference request for " + req.getTargetCompany() + " was marked as: " + normalized,
                            now, "Just now", false
                    ));
                }
                ReferenceNetworkDto fallback = new ReferenceNetworkDto();
                fallback.setId(id);
                fallback.setStatus(normalized);
                dispatchSuccess(callback, fallback);
            }
        });
    }

    // 5. Submit Completed Recommendation Letter
    public void completeRecommendation(long id, String recommendationText, String notes, Callback<ReferenceNetworkDto> callback) {
        executor.execute(() -> {
            String docId = REF_DOC_MAP.get(id);
            long now = System.currentTimeMillis();

            if (firestore != null && docId != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "COMPLETED");
                updates.put("recommendationText", recommendationText);
                updates.put("responseNotes", notes != null ? notes : "Official recommendation submitted");
                updates.put("updatedAt", now);

                firestore.collection("reference_requests").document(docId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            db.referenceRequestDao().updateRequestStatus(id, "COMPLETED");
                            ReferenceRequestEntity req = db.referenceRequestDao().getRequestById(id);
                            if (req != null) {
                                req.setRecommendationText(recommendationText);
                                req.setStatus("COMPLETED");
                                db.referenceRequestDao().updateRequest(req);

                                sendReferenceDecisionNotification(req.getStudentRoll(), docId, "Recommendation Completed!",
                                        "Your recommendation letter for " + req.getTargetCompany() + " is now ready.");
                            }

                            ReferenceNetworkDto dto = new ReferenceNetworkDto();
                            dto.setId(id);
                            dto.setStatus("COMPLETED");
                            dto.setRecommendationText(recommendationText);
                            dispatchSuccess(callback, dto);
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            db.referenceRequestDao().updateRequestStatus(id, "COMPLETED");
                            ReferenceRequestEntity req = db.referenceRequestDao().getRequestById(id);
                            if (req != null) {
                                req.setRecommendationText(recommendationText);
                                req.setStatus("COMPLETED");
                                db.referenceRequestDao().updateRequest(req);
                            }
                            ReferenceNetworkDto fallback = new ReferenceNetworkDto();
                            fallback.setId(id);
                            fallback.setStatus("COMPLETED");
                            fallback.setRecommendationText(recommendationText);
                            dispatchSuccess(callback, fallback);
                        }));
            } else {
                db.referenceRequestDao().updateRequestStatus(id, "COMPLETED");
                ReferenceRequestEntity req = db.referenceRequestDao().getRequestById(id);
                if (req != null) {
                    req.setRecommendationText(recommendationText);
                    req.setStatus("COMPLETED");
                    db.referenceRequestDao().updateRequest(req);
                    db.communicationDao().insertNotification(new NotificationEntity(
                            0, req.getStudentRoll(), "Recommendation Completed!",
                            "Your recommendation letter for " + req.getTargetCompany() + " is now ready.",
                            now, "Just now", false
                    ));
                }
                ReferenceNetworkDto fallback = new ReferenceNetworkDto();
                fallback.setId(id);
                fallback.setStatus("COMPLETED");
                fallback.setRecommendationText(recommendationText);
                dispatchSuccess(callback, fallback);
            }
        });
    }

    // 6. Cancel Request (Student)
    public void cancelRequest(long id, Callback<ReferenceNetworkDto> callback) {
        executor.execute(() -> {
            String docId = REF_DOC_MAP.get(id);
            long now = System.currentTimeMillis();

            if (firestore != null && docId != null) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "CANCELLED");
                updates.put("updatedAt", now);

                firestore.collection("reference_requests").document(docId)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> executor.execute(() -> {
                            db.referenceRequestDao().updateRequestStatus(id, "CANCELLED");
                            ReferenceNetworkDto dto = new ReferenceNetworkDto();
                            dto.setId(id);
                            dto.setStatus("CANCELLED");
                            dispatchSuccess(callback, dto);
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            db.referenceRequestDao().updateRequestStatus(id, "CANCELLED");
                            ReferenceNetworkDto fallback = new ReferenceNetworkDto();
                            fallback.setId(id);
                            fallback.setStatus("CANCELLED");
                            dispatchSuccess(callback, fallback);
                        }));
            } else {
                db.referenceRequestDao().updateRequestStatus(id, "CANCELLED");
                ReferenceNetworkDto fallback = new ReferenceNetworkDto();
                fallback.setId(id);
                fallback.setStatus("CANCELLED");
                dispatchSuccess(callback, fallback);
            }
        });
    }

    private void sendReferenceDecisionNotification(String studentUid, String refDocId, String title, String message) {
        if (firestore == null || studentUid == null || studentUid.isEmpty() || refDocId == null) return;
        String myUid = resolveCurrentUid();
        String myName = sessionManager != null ? sessionManager.getUserName() : "Referee";

        Map<String, Object> notif = new HashMap<>();
        notif.put("recipientUid", studentUid);
        notif.put("senderUid", myUid);
        notif.put("senderName", myName);
        notif.put("title", title);
        notif.put("message", message);
        notif.put("type", "REFERENCE_DECISION");
        notif.put("referenceId", refDocId);
        notif.put("read", false);
        notif.put("createdAt", System.currentTimeMillis());

        firestore.collection("users").document(studentUid)
                .collection("notifications")
                .add(notif)
                .addOnFailureListener(ignored -> {});
    }

    private ReferenceRequestEntity parseReferenceRequestEntity(String docId, long localId, Map<String, Object> data) {
        String studentRoll = data.get("studentRoll") != null ? String.valueOf(data.get("studentRoll")) : (data.get("studentUid") != null ? String.valueOf(data.get("studentUid")) : "");
        String studentName = data.get("studentName") != null ? String.valueOf(data.get("studentName")) : "Student";
        String studentDept = data.get("studentDept") != null ? String.valueOf(data.get("studentDept")) : "CSE";
        String targetCompany = data.get("targetCompany") != null ? String.valueOf(data.get("targetCompany")) : (data.get("targetInstitution") != null ? String.valueOf(data.get("targetInstitution")) : "Target");
        String message = data.get("message") != null ? String.valueOf(data.get("message")) : (data.get("personalMessage") != null ? String.valueOf(data.get("personalMessage")) : "");
        String status = data.get("status") != null ? String.valueOf(data.get("status")) : "PENDING";
        String alumniUid = data.get("alumniUid") != null ? String.valueOf(data.get("alumniUid")) : "alumni_1";
        String alumniName = data.get("alumniName") != null ? String.valueOf(data.get("alumniName")) : "Alumni Referee";
        String purpose = data.get("purpose") != null ? String.valueOf(data.get("purpose")) : "JOB_REFERRAL";
        String targetProgram = data.get("targetProgram") != null ? String.valueOf(data.get("targetProgram")) : "";
        String deadline = data.get("deadline") != null ? String.valueOf(data.get("deadline")) : "";
        String recommendationText = data.get("recommendationText") != null ? String.valueOf(data.get("recommendationText")) : null;
        String responseNotes = data.get("responseNotes") != null ? String.valueOf(data.get("responseNotes")) : null;

        long reqAt = System.currentTimeMillis();
        if (data.get("requestedAt") instanceof Number) {
            reqAt = ((Number) data.get("requestedAt")).longValue();
        } else if (data.get("createdAt") instanceof Number) {
            reqAt = ((Number) data.get("createdAt")).longValue();
        }

        return new ReferenceRequestEntity(
                localId, studentRoll, studentName, studentDept, targetCompany, message, status,
                alumniUid, reqAt, "ROLE_ALUMNI", alumniName, purpose, targetProgram, deadline,
                recommendationText, responseNotes
        );
    }
}
