package com.teligati.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.teligati.app.data.DataMapper;
import com.teligati.app.data.SessionManager;
import com.teligati.app.data.database.AppDatabase;
import com.teligati.app.data.entity.ConversationEntity;
import com.teligati.app.data.entity.MessageEntity;
import com.teligati.app.data.entity.NotificationEntity;
import com.teligati.app.network.ApiClient;
import com.teligati.app.network.MessagingApiService;
import com.teligati.app.network.model.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class MessagingRepository {

    private static volatile MessagingRepository INSTANCE;

    private final MessagingApiService apiService;
    private final SessionManager sessionManager;
    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor;
    private final Executor postExecutor;

    private static final Map<Long, String> CONV_DOC_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Long> CONV_ID_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, String> NOTIF_DOC_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Long> NOTIF_ID_MAP = new ConcurrentHashMap<>();

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public MessagingRepository(MessagingApiService apiService, SessionManager sessionManager, AppDatabase db) {
        this(apiService, sessionManager, db, null, createDefaultPostExecutor());
    }

    public MessagingRepository(MessagingApiService apiService, SessionManager sessionManager, AppDatabase db, Executor postExecutor) {
        this(apiService, sessionManager, db, null, postExecutor);
    }

    public MessagingRepository(MessagingApiService apiService, SessionManager sessionManager, AppDatabase db, FirebaseFirestore firestore, Executor postExecutor) {
        this.apiService = apiService;
        this.sessionManager = sessionManager;
        this.db = db;
        this.firestore = firestore;
        this.executor = Executors.newFixedThreadPool(4);
        this.postExecutor = postExecutor != null ? postExecutor : createDefaultPostExecutor();
    }

    public static MessagingRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MessagingRepository.class) {
                if (INSTANCE == null) {
                    Context appCtx = context.getApplicationContext();
                    FirebaseFirestore firestore = null;
                    try {
                        firestore = FirebaseFirestore.getInstance();
                    } catch (Throwable ignored) {}
                    INSTANCE = new MessagingRepository(
                            ApiClient.getInstance(appCtx).getMessagingApiService(),
                            SessionManager.getInstance(appCtx),
                            AppDatabase.getInstance(appCtx),
                            firestore,
                            createDefaultPostExecutor()
                    );
                }
            }
        }
        return INSTANCE;
    }

    public static void setInstanceForTesting(MessagingRepository testInstance) {
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

    // =========================================================================
    // 1. CONVERSATIONS (FIRESTORE: /conversations/{conversationId})
    // =========================================================================

    public void getConversations(Callback<List<ConversationEntity>> callback) {
        executor.execute(() -> {
            // 1. Serve Room cached conversations first
            List<ConversationEntity> cached = db.communicationDao().getAllConversations();
            dispatchSuccess(callback, cached != null ? cached : new ArrayList<>());

            String myUid = resolveCurrentUid();
            if (firestore != null && myUid != null && !myUid.isEmpty()) {
                firestore.collection("conversations")
                        .whereArrayContains("participantUids", myUid)
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<ConversationEntity> entities = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    String docId = snap.getId();
                                    long localId = parseOrGenerateLongId(docId, data.get("localId"));
                                    CONV_DOC_MAP.put(localId, docId);
                                    CONV_ID_MAP.put(docId, localId);

                                    List<?> uids = (List<?>) data.get("participantUids");
                                    String otherUid = "";
                                    if (uids != null) {
                                        for (Object u : uids) {
                                            String uStr = String.valueOf(u);
                                            if (!myUid.equals(uStr)) {
                                                otherUid = uStr;
                                                break;
                                            }
                                        }
                                    }
                                    long otherParticipantId = parseOrGenerateLongId(otherUid, null);

                                    String otherName = "Participant";
                                    String otherEmail = "";
                                    String otherRole = "USER";
                                    String otherDept = "KUET";

                                    Object detailsObj = data.get("participantDetails");
                                    if (detailsObj instanceof Map) {
                                        Map<?, ?> detailsMap = (Map<?, ?>) detailsObj;
                                        Object otherDetails = detailsMap.get(otherUid);
                                        if (otherDetails instanceof Map) {
                                            Map<?, ?> oMap = (Map<?, ?>) otherDetails;
                                            otherName = getString(oMap, "name", otherName);
                                            otherEmail = getString(oMap, "email", otherEmail);
                                            otherRole = getString(oMap, "role", otherRole);
                                            otherDept = getString(oMap, "department", otherDept);
                                        }
                                    }

                                    String lastMessage = getString(data, "lastMessage", "");
                                    long lastMessageAt = getLong(data, "lastMessageTime", getLong(data, "updatedAt", System.currentTimeMillis()));

                                    long unreadCount = 0;
                                    Object unreadObj = data.get("unreadCounts");
                                    if (unreadObj instanceof Map) {
                                        Map<?, ?> unreadMap = (Map<?, ?>) unreadObj;
                                        unreadCount = getLong(unreadMap, myUid, 0L);
                                    }

                                    ConversationEntity entity = new ConversationEntity(
                                            localId, otherParticipantId, otherName, otherEmail,
                                            otherRole, otherDept, lastMessage, lastMessageAt, unreadCount
                                    );
                                    entities.add(entity);
                                }
                                if (!entities.isEmpty()) {
                                    db.communicationDao().insertConversations(entities);
                                    List<ConversationEntity> updated = db.communicationDao().getAllConversations();
                                    dispatchSuccess(callback, updated != null && !updated.isEmpty() ? updated : entities);
                                    return;
                                }
                            }
                            dispatchSuccess(callback, db.communicationDao().getAllConversations());
                        }))
                        .addOnFailureListener(e -> {
                            // Offline or network error - cache already dispatched
                        });
                return;
            }

            // Fallback for tests when firestore == null
            try {
                if (apiService != null) {
                    Response<ApiResponse<PagedResponse<ConversationNetworkDto>>> response =
                            apiService.getConversations(0, 50).execute();
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        List<ConversationNetworkDto> dtoList = response.body().getData().getContent();
                        List<ConversationEntity> entities = DataMapper.toConversationEntityList(dtoList);
                        if (!entities.isEmpty()) {
                            db.communicationDao().insertConversations(entities);
                            dispatchSuccess(callback, db.communicationDao().getAllConversations());
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void getOrCreateConversation(String otherUid, String otherName, String otherRole, String otherDept, String otherEmail, String initialMessage, Callback<ConversationEntity> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            if (myUid == null || myUid.isEmpty()) {
                myUid = sessionManager != null ? sessionManager.getUserId() : "user_1";
            }

            if (myUid.equals(otherUid)) {
                dispatchError(callback, "Cannot create conversation with yourself.");
                return;
            }

            long now = System.currentTimeMillis();
            String convDocId = (myUid.compareTo(otherUid) < 0) ? (myUid + "_" + otherUid) : (otherUid + "_" + myUid);
            long localId = parseOrGenerateLongId(convDocId, null);
            long otherParticipantId = parseOrGenerateLongId(otherUid, null);

            CONV_DOC_MAP.put(localId, convDocId);
            CONV_ID_MAP.put(convDocId, localId);

            if (firestore != null) {
                DocumentReference convRef = firestore.collection("conversations").document(convDocId);
                final String finalMyUid = myUid;
                convRef.get().addOnSuccessListener(snap -> executor.execute(() -> {
                    if (snap != null && snap.exists()) {
                        ConversationEntity entity = new ConversationEntity(
                                localId, otherParticipantId, otherName, otherEmail,
                                otherRole, otherDept,
                                getString(snap.getData(), "lastMessage", initialMessage != null ? initialMessage : ""),
                                getLong(snap.getData(), "lastMessageTime", now),
                                0
                        );
                        db.communicationDao().insertConversation(entity);
                        dispatchSuccess(callback, entity);
                    } else {
                        Map<String, Object> convData = new HashMap<>();
                        convData.put("id", convDocId);
                        convData.put("localId", localId);
                        convData.put("participantUids", Arrays.asList(finalMyUid, otherUid));

                        Map<String, Object> participantDetails = new HashMap<>();
                        Map<String, Object> myDetails = new HashMap<>();
                        myDetails.put("name", sessionManager != null ? sessionManager.getUserName() : "User");
                        myDetails.put("role", sessionManager != null ? sessionManager.getUserRole() : "STUDENT");
                        myDetails.put("department", sessionManager != null ? sessionManager.getUserDept() : "KUET");
                        myDetails.put("email", sessionManager != null ? sessionManager.getUserEmail() : "");
                        participantDetails.put(finalMyUid, myDetails);

                        Map<String, Object> otherDetails = new HashMap<>();
                        otherDetails.put("name", otherName != null ? otherName : "Participant");
                        otherDetails.put("role", otherRole != null ? otherRole : "USER");
                        otherDetails.put("department", otherDept != null ? otherDept : "KUET");
                        otherDetails.put("email", otherEmail != null ? otherEmail : "");
                        participantDetails.put(otherUid, otherDetails);

                        convData.put("participantDetails", participantDetails);
                        convData.put("lastMessage", initialMessage != null ? initialMessage : "");
                        convData.put("lastMessageTime", now);
                        convData.put("lastSenderUid", finalMyUid);

                        Map<String, Object> unread = new HashMap<>();
                        unread.put(finalMyUid, 0L);
                        unread.put(otherUid, (initialMessage != null && !initialMessage.isEmpty()) ? 1L : 0L);
                        convData.put("unreadCounts", unread);

                        convData.put("createdAt", now);
                        convData.put("updatedAt", now);

                        convRef.set(convData).addOnSuccessListener(aVoid -> executor.execute(() -> {
                            ConversationEntity newEntity = new ConversationEntity(
                                    localId, otherParticipantId, otherName, otherEmail,
                                    otherRole, otherDept, initialMessage != null ? initialMessage : "",
                                    now, 0
                            );
                            db.communicationDao().insertConversation(newEntity);

                            if (initialMessage != null && !initialMessage.trim().isEmpty()) {
                                sendMessage(localId, initialMessage, null);
                            }

                            dispatchSuccess(callback, newEntity);
                        })).addOnFailureListener(e -> executor.execute(() -> {
                            dispatchError(callback, e.getMessage() != null ? e.getMessage() : "Failed to create conversation");
                        }));
                    }
                })).addOnFailureListener(e -> executor.execute(() -> {
                    dispatchError(callback, e.getMessage() != null ? e.getMessage() : "Failed to retrieve conversation");
                }));
                return;
            }

            // Fallback for tests when firestore == null
            ConversationEntity fallback = new ConversationEntity(
                    localId, otherParticipantId, otherName, otherEmail,
                    otherRole, otherDept, initialMessage != null ? initialMessage : "", now, 0
            );
            db.communicationDao().insertConversation(fallback);
            dispatchSuccess(callback, fallback);
        });
    }

    public void getOrCreateConversation(long recipientId, String initialMessage, Callback<ConversationEntity> callback) {
        getOrCreateConversation(String.valueOf(recipientId), "Recipient #" + recipientId, "USER", "KUET", "", initialMessage, callback);
    }

    // =========================================================================
    // 2. MESSAGES (FIRESTORE: /conversations/{conversationId}/messages/{msgId})
    // =========================================================================

    public void getConversationMessages(long conversationId, Callback<List<MessageEntity>> callback) {
        executor.execute(() -> {
            List<MessageEntity> cached = db.communicationDao().getMessagesForConversation(conversationId);
            if (cached == null || cached.isEmpty()) {
                cached = db.communicationDao().getAllMessages();
            }
            dispatchSuccess(callback, cached != null ? cached : new ArrayList<>());

            if (firestore != null) {
                String convDocId = CONV_DOC_MAP.get(conversationId);
                if (convDocId != null) {
                    firestore.collection("conversations").document(convDocId)
                            .collection("messages")
                            .orderBy("timestamp", Query.Direction.ASCENDING)
                            .get()
                            .addOnSuccessListener(snapshots -> executor.execute(() -> {
                                if (snapshots != null && !snapshots.isEmpty()) {
                                    List<MessageEntity> list = new ArrayList<>();
                                    for (QueryDocumentSnapshot snap : snapshots) {
                                        Map<String, Object> data = snap.getData();
                                        long localMsgId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                                        String senderUid = getString(data, "senderUid", "");
                                        String senderName = getString(data, "senderName", "User");
                                        String content = getString(data, "content", "");
                                        long ts = getLong(data, "timestamp", System.currentTimeMillis());

                                        MessageEntity msg = new MessageEntity(
                                                localMsgId, conversationId, senderUid, senderName,
                                                "", "", content, ts, formatTimestamp(ts), true
                                        );
                                        list.add(msg);
                                    }
                                    if (!list.isEmpty()) {
                                        db.communicationDao().insertMessages(list);
                                        dispatchSuccess(callback, db.communicationDao().getMessagesForConversation(conversationId));
                                        return;
                                    }
                                }
                                dispatchSuccess(callback, db.communicationDao().getMessagesForConversation(conversationId));
                            }))
                            .addOnFailureListener(e -> {
                                // Offline fallback already dispatched
                            });
                    return;
                }
            }
        });
    }

    public ListenerRegistration listenToConversationMessages(long conversationId, Callback<List<MessageEntity>> callback) {
        if (firestore == null) return null;
        String convDocId = CONV_DOC_MAP.get(conversationId);
        if (convDocId == null) return null;

        return firestore.collection("conversations").document(convDocId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    executor.execute(() -> {
                        List<MessageEntity> list = new ArrayList<>();
                        for (QueryDocumentSnapshot snap : snapshots) {
                            Map<String, Object> data = snap.getData();
                            long localMsgId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                            String senderUid = getString(data, "senderUid", "");
                            String senderName = getString(data, "senderName", "User");
                            String content = getString(data, "content", "");
                            long ts = getLong(data, "timestamp", System.currentTimeMillis());

                            MessageEntity msg = new MessageEntity(
                                    localMsgId, conversationId, senderUid, senderName,
                                    "", "", content, ts, formatTimestamp(ts), true
                            );
                            list.add(msg);
                        }
                        if (!list.isEmpty()) {
                            db.communicationDao().insertMessages(list);
                        }
                        dispatchSuccess(callback, list);
                    });
                });
    }

    public void sendMessage(long conversationId, String content, Callback<MessageEntity> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            String myName = sessionManager != null ? sessionManager.getUserName() : "User";
            long now = System.currentTimeMillis();

            if (firestore != null) {
                String convDocId = CONV_DOC_MAP.get(conversationId);
                if (convDocId != null) {
                    DocumentReference msgRef = firestore.collection("conversations")
                            .document(convDocId)
                            .collection("messages")
                            .document();
                    String msgDocId = msgRef.getId();
                    long localMsgId = parseOrGenerateLongId(msgDocId, null);

                    Map<String, Object> msgData = new HashMap<>();
                    msgData.put("id", msgDocId);
                    msgData.put("localId", localMsgId);
                    msgData.put("conversationId", convDocId);
                    msgData.put("senderUid", myUid);
                    msgData.put("senderName", myName);
                    msgData.put("content", content);
                    msgData.put("timestamp", now);

                    msgRef.set(msgData).addOnSuccessListener(aVoid -> executor.execute(() -> {
                        MessageEntity entity = new MessageEntity(
                                localMsgId, conversationId, myUid, myName,
                                "", "", content, now, formatTimestamp(now), true
                        );
                        db.communicationDao().insertMessage(entity);

                        // Update conversation lastMessage & lastSenderUid (restricted keys only)
                        Map<String, Object> convUpdates = new HashMap<>();
                        convUpdates.put("lastMessage", content);
                        convUpdates.put("lastMessageTime", now);
                        convUpdates.put("lastSenderUid", myUid);
                        convUpdates.put("updatedAt", now);

                        firestore.collection("conversations").document(convDocId).update(convUpdates);

                        // Update local Room conversation
                        ConversationEntity conv = db.communicationDao().getConversationById(conversationId);
                        if (conv != null) {
                            conv.setLastMessageText(content);
                            conv.setLastMessageAt(now);
                            db.communicationDao().insertConversation(conv);

                            // Send direct message notification to other participant
                            String otherUid = String.valueOf(conv.getOtherParticipantId());
                            if (convDocId.contains("_")) {
                                String[] parts = convDocId.split("_");
                                if (parts.length == 2) {
                                    otherUid = myUid.equals(parts[0]) ? parts[1] : parts[0];
                                }
                            }
                            if (otherUid != null && !otherUid.isEmpty() && !otherUid.equals(myUid)) {
                                createDirectMessageNotification(otherUid, myName, content, convDocId);
                            }
                        }

                        dispatchSuccess(callback, entity);
                    })).addOnFailureListener(e -> executor.execute(() -> {
                        dispatchError(callback, e.getMessage() != null ? e.getMessage() : "Failed to send message to Firestore");
                    }));
                    return;
                }
            }

            // Fallback for tests when firestore == null
            MessageEntity localMsg = new MessageEntity(
                    0, conversationId, myUid != null ? myUid : "sender", myName,
                    "recipient", "Recipient", content, now, formatTimestamp(now), false
            );
            long id = db.communicationDao().insertMessage(localMsg);
            localMsg.setId(id);
            dispatchSuccess(callback, localMsg);
        });
    }

    public void markConversationAsRead(long conversationId, Callback<Void> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            db.communicationDao().markConversationMessagesRead(conversationId, myUid != null ? myUid : (sessionManager != null ? sessionManager.getUserId() : ""));
            dispatchSuccess(callback, null);
        });
    }

    // =========================================================================
    // 3. NOTIFICATIONS (FIRESTORE: /users/{uid}/notifications/{notifId})
    // =========================================================================

    public void syncNotifications(String userId, Callback<List<NotificationEntity>> callback) {
        executor.execute(() -> {
            // 1. Serve Room cached notifications first
            List<NotificationEntity> cached = db.communicationDao().getAllNotifications();
            dispatchSuccess(callback, cached != null ? cached : new ArrayList<>());

            String myUid = resolveCurrentUid();
            String effectiveUid = (myUid != null && !myUid.isEmpty()) ? myUid : userId;

            if (firestore != null && effectiveUid != null && !effectiveUid.isEmpty()) {
                firestore.collection("users")
                        .document(effectiveUid)
                        .collection("notifications")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<NotificationEntity> list = new ArrayList<>();
                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    long localId = parseOrGenerateLongId(snap.getId(), data.get("localId"));
                                    NOTIF_DOC_MAP.put(localId, snap.getId());
                                    NOTIF_ID_MAP.put(snap.getId(), localId);

                                    String title = getString(data, "title", "Notification");
                                    String msg = getString(data, "message", "");
                                    boolean read = getBoolean(data, "read", false);
                                    long ts = getLong(data, "timestamp", System.currentTimeMillis());

                                    NotificationEntity notif = new NotificationEntity(
                                            localId, effectiveUid, title, msg, ts, formatTimestamp(ts), read
                                    );
                                    list.add(notif);
                                }
                                if (!list.isEmpty()) {
                                    db.communicationDao().insertNotifications(list);
                                    List<NotificationEntity> updated = db.communicationDao().getAllNotifications();
                                    dispatchSuccess(callback, updated != null && !updated.isEmpty() ? updated : list);
                                    return;
                                }
                            }
                            dispatchSuccess(callback, db.communicationDao().getAllNotifications());
                        }))
                        .addOnFailureListener(e -> {
                            // Offline or network error - cache already dispatched
                        });
                return;
            }
        });
    }

    public void markNotificationAsRead(long id, Callback<Boolean> callback) {
        executor.execute(() -> {
            db.communicationDao().markNotificationAsRead(id);

            String myUid = resolveCurrentUid();
            if (firestore != null && myUid != null && !myUid.isEmpty()) {
                String docId = NOTIF_DOC_MAP.get(id);
                if (docId != null) {
                    firestore.collection("users")
                            .document(myUid)
                            .collection("notifications")
                            .document(docId)
                            .update("read", true)
                            .addOnCompleteListener(task -> {
                                dispatchSuccess(callback, true);
                            });
                    return;
                }
            }

            dispatchSuccess(callback, true);
        });
    }

    public void createDirectMessageNotification(String recipientUid, String senderName, String messageText, String convDocId) {
        createWorkflowNotification(
                recipientUid,
                "New Message from " + senderName,
                messageText.length() > 60 ? messageText.substring(0, 57) + "..." : messageText,
                "MESSAGE",
                convDocId,
                null
        );
    }

    public void createWorkflowNotification(String recipientUid, String title, String message, String type, String referenceId, Callback<Boolean> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            long now = System.currentTimeMillis();

            if (firestore != null && recipientUid != null && !recipientUid.isEmpty()) {
                DocumentReference notifRef = firestore.collection("users")
                        .document(recipientUid)
                        .collection("notifications")
                        .document();
                String notifDocId = notifRef.getId();
                long localId = parseOrGenerateLongId(notifDocId, null);
                NOTIF_DOC_MAP.put(localId, notifDocId);
                NOTIF_ID_MAP.put(notifDocId, localId);

                Map<String, Object> data = new HashMap<>();
                data.put("id", notifDocId);
                data.put("localId", localId);
                data.put("recipientUid", recipientUid);
                data.put("senderUid", myUid != null ? myUid : recipientUid);
                data.put("read", false);
                data.put("title", title != null ? title : "Notification");
                data.put("message", message != null ? message : "");
                data.put("timestamp", now);
                data.put("createdAt", now);

                if (type != null) data.put("type", type);
                if (referenceId != null) data.put("referenceId", referenceId);

                notifRef.set(data).addOnSuccessListener(aVoid -> executor.execute(() -> {
                    NotificationEntity entity = new NotificationEntity(
                            localId, recipientUid, title, message, now, formatTimestamp(now), false
                    );
                    db.communicationDao().insertNotification(entity);
                    if (callback != null) dispatchSuccess(callback, true);
                })).addOnFailureListener(e -> executor.execute(() -> {
                    // Cache locally even if network fails
                    NotificationEntity entity = new NotificationEntity(
                            0, recipientUid, title, message, now, formatTimestamp(now), false
                    );
                    db.communicationDao().insertNotification(entity);
                    if (callback != null) dispatchSuccess(callback, true);
                }));
                return;
            }

            NotificationEntity entity = new NotificationEntity(
                    0, recipientUid != null ? recipientUid : "user", title, message, now, formatTimestamp(now), false
            );
            db.communicationDao().insertNotification(entity);
            if (callback != null) dispatchSuccess(callback, true);
        });
    }

    public void getUnreadNotificationCount(String userId, Callback<Integer> callback) {
        syncNotifications(userId, new Callback<List<NotificationEntity>>() {
            @Override
            public void onSuccess(List<NotificationEntity> result) {
                int count = 0;
                if (result != null) {
                    for (NotificationEntity n : result) {
                        if (!n.isRead()) count++;
                    }
                }
                dispatchSuccess(callback, count);
            }

            @Override
            public void onError(String errorMessage) {
                dispatchSuccess(callback, 0);
            }
        });
    }

    // =========================================================================
    // 4. HELPER METHODS
    // =========================================================================

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
                if (docId.startsWith("conv_") || docId.startsWith("notif_") || docId.startsWith("msg_")) {
                    return Long.parseLong(docId.substring(docId.indexOf('_') + 1));
                }
                return Long.parseLong(docId);
            } catch (Exception ignored) {}
            return Math.abs(docId.hashCode()) + 1L;
        }
        return System.currentTimeMillis();
    }

    private static String formatTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Just now";
        }
    }

    private static String getString(Map<?, ?> data, String key, String defaultVal) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return defaultVal;
        return String.valueOf(data.get(key));
    }

    private static long getLong(Map<?, ?> data, String key, long defaultVal) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return defaultVal;
        Object v = data.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return defaultVal; }
    }

    private static boolean getBoolean(Map<?, ?> data, String key, boolean defaultVal) {
        if (data == null || !data.containsKey(key) || data.get(key) == null) return defaultVal;
        Object v = data.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(String.valueOf(v));
    }
}
