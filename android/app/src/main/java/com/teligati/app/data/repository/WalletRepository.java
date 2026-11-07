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
import com.teligati.app.data.entity.TransactionEntity;
import com.teligati.app.data.entity.UserEntity;
import com.teligati.app.network.ApiClient;
import com.teligati.app.network.WalletApiService;
import com.teligati.app.network.model.ApiResponse;
import com.teligati.app.network.model.EscrowCreateRequest;
import com.teligati.app.network.model.EscrowNetworkDto;
import com.teligati.app.network.model.TopUpRequest;
import com.teligati.app.network.model.TransactionNetworkDto;
import com.teligati.app.network.model.WalletNetworkDto;
import com.teligati.app.network.model.WithdrawRequest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class WalletRepository {

    public static final String DEMO_DISCLAIMER = "DEMO WALLET - NO REAL MONEY INVOLVED";
    public static final String DEMO_NOTICE = "DEMO MODE - All balances and transactions are simulated.";

    private static volatile WalletRepository INSTANCE;

    private final WalletApiService apiService;
    private final SessionManager sessionManager;
    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final ExecutorService executor;
    private final Executor postExecutor;

    private static final Map<Long, String> ESCROW_DOC_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Long> ESCROW_ID_MAP = new ConcurrentHashMap<>();
    private static final AtomicLong ESCROW_ID_GEN = new AtomicLong(5000L);

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String errorMessage);
    }

    public WalletRepository(WalletApiService apiService, SessionManager sessionManager, AppDatabase db) {
        this(apiService, sessionManager, db, null, createDefaultPostExecutor());
    }

    public WalletRepository(WalletApiService apiService, SessionManager sessionManager, AppDatabase db, Executor postExecutor) {
        this(apiService, sessionManager, db, null, postExecutor);
    }

    public WalletRepository(WalletApiService apiService, SessionManager sessionManager, AppDatabase db, FirebaseFirestore firestore, Executor postExecutor) {
        this.apiService = apiService;
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

    public static WalletRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (WalletRepository.class) {
                if (INSTANCE == null) {
                    Context appCtx = context.getApplicationContext();
                    FirebaseFirestore fs = null;
                    try {
                        fs = FirebaseFirestore.getInstance();
                    } catch (Throwable ignored) {}

                    INSTANCE = new WalletRepository(
                            ApiClient.getInstance(appCtx).getWalletApiService(),
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

    public static void setInstanceForTesting(WalletRepository repository) {
        INSTANCE = repository;
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

    private long parseOrGenerateEscrowId(String docId, Long existingId) {
        if (existingId != null && existingId > 0) {
            ESCROW_DOC_MAP.put(existingId, docId);
            ESCROW_ID_MAP.put(docId, existingId);
            return existingId;
        }
        if (ESCROW_ID_MAP.containsKey(docId)) {
            return ESCROW_ID_MAP.get(docId);
        }
        long newId = ESCROW_ID_GEN.incrementAndGet();
        ESCROW_DOC_MAP.put(newId, docId);
        ESCROW_ID_MAP.put(docId, newId);
        return newId;
    }

    // =========================================================================
    // 1. DEMO WALLET (FIRESTORE: users/{uid}/demo_wallet/main)
    // =========================================================================

    public void syncWallet(Callback<WalletNetworkDto> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            long now = System.currentTimeMillis();

            if (firestore != null) {
                DocumentReference walletRef = firestore.collection("users")
                        .document(myUid)
                        .collection("demo_wallet")
                        .document("main");

                walletRef.get().addOnSuccessListener(snap -> executor.execute(() -> {
                    if (snap != null && snap.exists()) {
                        double avail = 0.0;
                        double escrow = 0.0;
                        String currency = "DEMO_CREDITS";

                        if (snap.get("availableBalance") instanceof Number) {
                            avail = ((Number) snap.get("availableBalance")).doubleValue();
                        }
                        if (snap.get("escrowBalance") instanceof Number) {
                            escrow = ((Number) snap.get("escrowBalance")).doubleValue();
                        }
                        if (snap.getString("currency") != null) {
                            currency = snap.getString("currency");
                        }

                        updateLocalBalance(avail, escrow);

                        WalletNetworkDto dto = new WalletNetworkDto(
                                1L, 1L, avail, escrow, currency,
                                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                                DEMO_DISCLAIMER, DEMO_NOTICE
                        );
                        postSuccess(callback, dto);
                    } else {
                        // Provision initial demo wallet (grant 10,000 DEMO_CREDITS)
                        Map<String, Object> initial = new HashMap<>();
                        initial.put("availableBalance", 10000.0);
                        initial.put("escrowBalance", 0.0);
                        initial.put("currency", "DEMO_CREDITS");
                        initial.put("updatedAt", now);
                        initial.put("disclaimer", DEMO_DISCLAIMER);
                        initial.put("demoNotice", DEMO_NOTICE);

                        walletRef.set(initial).addOnSuccessListener(aVoid -> executor.execute(() -> {
                            updateLocalBalance(10000.0, 0.0);

                            WalletNetworkDto dto = new WalletNetworkDto(
                                    1L, 1L, 10000.0, 0.0, "DEMO_CREDITS",
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                                    DEMO_DISCLAIMER, DEMO_NOTICE
                            );
                            postSuccess(callback, dto);
                        })).addOnFailureListener(e -> executor.execute(() -> {
                            fallbackLocalWallet(callback);
                        }));
                    }
                })).addOnFailureListener(e -> executor.execute(() -> {
                    fallbackLocalWallet(callback);
                }));
            } else {
                fallbackLocalWallet(callback);
            }
        });
    }

    private void fallbackLocalWallet(Callback<WalletNetworkDto> callback) {
        String userId = sessionManager != null ? sessionManager.getUserId() : "student_1";
        UserEntity user = db.userDao().getUserById(userId);
        double avail = user != null ? user.getAvailableBalance() : 10000.0;
        double escrow = user != null ? user.getEscrowBalance() : 0.0;

        WalletNetworkDto dto = new WalletNetworkDto(
                1L, 1L, avail, escrow, "DEMO_CREDITS",
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                DEMO_DISCLAIMER, DEMO_NOTICE
        );
        postSuccess(callback, dto);
    }

    public void updateBalance(double available, double escrow, Callback<WalletNetworkDto> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            long now = System.currentTimeMillis();

            if (available < 0 || escrow < 0) {
                postError(callback, "Balance cannot be negative.");
                return;
            }

            updateLocalBalance(available, escrow);

            if (firestore != null) {
                DocumentReference walletRef = firestore.collection("users")
                        .document(myUid)
                        .collection("demo_wallet")
                        .document("main");

                Map<String, Object> updates = new HashMap<>();
                updates.put("availableBalance", available);
                updates.put("escrowBalance", escrow);
                updates.put("updatedAt", now);

                walletRef.update(updates).addOnSuccessListener(aVoid -> {
                    WalletNetworkDto dto = new WalletNetworkDto(
                            1L, 1L, available, escrow, "DEMO_CREDITS",
                            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                            DEMO_DISCLAIMER, DEMO_NOTICE
                    );
                    postSuccess(callback, dto);
                }).addOnFailureListener(e -> {
                    WalletNetworkDto dto = new WalletNetworkDto(
                            1L, 1L, available, escrow, "DEMO_CREDITS",
                            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                            DEMO_DISCLAIMER, DEMO_NOTICE
                    );
                    postSuccess(callback, dto);
                });
            } else {
                WalletNetworkDto dto = new WalletNetworkDto(
                        1L, 1L, available, escrow, "DEMO_CREDITS",
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                        DEMO_DISCLAIMER, DEMO_NOTICE
                );
                postSuccess(callback, dto);
            }
        });
    }

    public void topUp(double amount, String paymentMethod, String note, Callback<WalletNetworkDto> callback) {
        executor.execute(() -> {
            if (amount <= 0) {
                postError(callback, "Top-up amount must be greater than zero.");
                return;
            }

            String myUid = resolveCurrentUid();
            long now = System.currentTimeMillis();

            if (firestore != null) {
                DocumentReference walletRef = firestore.collection("users")
                        .document(myUid)
                        .collection("demo_wallet")
                        .document("main");

                walletRef.get().addOnSuccessListener(snap -> executor.execute(() -> {
                    double currentAvail = 10000.0;
                    double currentEscrow = 0.0;
                    if (snap != null && snap.exists()) {
                        if (snap.get("availableBalance") instanceof Number) {
                            currentAvail = ((Number) snap.get("availableBalance")).doubleValue();
                        }
                        if (snap.get("escrowBalance") instanceof Number) {
                            currentEscrow = ((Number) snap.get("escrowBalance")).doubleValue();
                        }
                    }

                    double newAvail = currentAvail + amount;
                    if (newAvail > 100000.0) {
                        postError(callback, "Demo wallet balance limit exceeded (Maximum ৳100,000 DEMO_CREDITS).");
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("availableBalance", newAvail);
                    updates.put("updatedAt", now);

                    double finalEscrow = currentEscrow;
                    walletRef.update(updates).addOnSuccessListener(aVoid -> executor.execute(() -> {
                        updateLocalBalance(newAvail, finalEscrow);

                        // Immutable transaction record
                        recordFirestoreTransaction(myUid, amount, "TOPUP",
                                "Simulated Top-Up (" + paymentMethod + ")", "CREDIT", "COMPLETED", note);

                        WalletNetworkDto dto = new WalletNetworkDto(
                                1L, 1L, newAvail, finalEscrow, "DEMO_CREDITS",
                                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                                DEMO_DISCLAIMER, DEMO_NOTICE
                        );
                        postSuccess(callback, dto);
                    })).addOnFailureListener(e -> executor.execute(() -> {
                        postError(callback, "Failed to update wallet: " + e.getMessage());
                    }));
                })).addOnFailureListener(e -> executor.execute(() -> {
                    postError(callback, "Failed to read wallet: " + e.getMessage());
                }));
            } else {
                String userId = sessionManager != null ? sessionManager.getUserId() : "student_1";
                UserEntity user = db.userDao().getUserById(userId);
                double current = user != null ? user.getAvailableBalance() : 10000.0;
                double newAvail = current + amount;
                updateLocalBalance(newAvail, user != null ? user.getEscrowBalance() : 0.0);

                db.communicationDao().insertTransaction(new TransactionEntity(
                        "TX-" + now, "Simulated Top-Up (" + paymentMethod + ")", "CREDIT",
                        amount, "Just now", "Completed", userId
                ));

                WalletNetworkDto dto = new WalletNetworkDto(
                        1L, 1L, newAvail, user != null ? user.getEscrowBalance() : 0.0, "DEMO_CREDITS",
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                        DEMO_DISCLAIMER, DEMO_NOTICE
                );
                postSuccess(callback, dto);
            }
        });
    }

    public void withdraw(double amount, String destinationAccount, String note, Callback<WalletNetworkDto> callback) {
        executor.execute(() -> {
            if (amount <= 0) {
                postError(callback, "Withdrawal amount must be greater than zero.");
                return;
            }

            String myUid = resolveCurrentUid();
            long now = System.currentTimeMillis();

            if (firestore != null) {
                DocumentReference walletRef = firestore.collection("users")
                        .document(myUid)
                        .collection("demo_wallet")
                        .document("main");

                walletRef.get().addOnSuccessListener(snap -> executor.execute(() -> {
                    double currentAvail = 0.0;
                    double currentEscrow = 0.0;
                    if (snap != null && snap.exists()) {
                        if (snap.get("availableBalance") instanceof Number) {
                            currentAvail = ((Number) snap.get("availableBalance")).doubleValue();
                        }
                        if (snap.get("escrowBalance") instanceof Number) {
                            currentEscrow = ((Number) snap.get("escrowBalance")).doubleValue();
                        }
                    }

                    if (currentAvail < amount) {
                        postError(callback, "Insufficient available demo balance.");
                        return;
                    }

                    double newAvail = currentAvail - amount;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("availableBalance", newAvail);
                    updates.put("updatedAt", now);

                    double finalEscrow = currentEscrow;
                    walletRef.update(updates).addOnSuccessListener(aVoid -> executor.execute(() -> {
                        updateLocalBalance(newAvail, finalEscrow);

                        // Immutable transaction record
                        recordFirestoreTransaction(myUid, amount, "WITHDRAWAL",
                                "Simulated Withdrawal (" + destinationAccount + ")", "DEBIT", "COMPLETED", note);

                        WalletNetworkDto dto = new WalletNetworkDto(
                                1L, 1L, newAvail, finalEscrow, "DEMO_CREDITS",
                                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                                DEMO_DISCLAIMER, DEMO_NOTICE
                        );
                        postSuccess(callback, dto);
                    })).addOnFailureListener(e -> executor.execute(() -> {
                        postError(callback, "Failed to update wallet: " + e.getMessage());
                    }));
                })).addOnFailureListener(e -> executor.execute(() -> {
                    postError(callback, "Failed to read wallet: " + e.getMessage());
                }));
            } else {
                String userId = sessionManager != null ? sessionManager.getUserId() : "student_1";
                UserEntity user = db.userDao().getUserById(userId);
                double current = user != null ? user.getAvailableBalance() : 10000.0;
                if (current < amount) {
                    postError(callback, "Insufficient available demo balance.");
                    return;
                }
                double newAvail = current - amount;
                updateLocalBalance(newAvail, user != null ? user.getEscrowBalance() : 0.0);

                db.communicationDao().insertTransaction(new TransactionEntity(
                        "TX-" + now, "Simulated Withdrawal (" + destinationAccount + ")", "DEBIT",
                        amount, "Just now", "Completed", userId
                ));

                WalletNetworkDto dto = new WalletNetworkDto(
                        1L, 1L, newAvail, user != null ? user.getEscrowBalance() : 0.0, "DEMO_CREDITS",
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date()),
                        DEMO_DISCLAIMER, DEMO_NOTICE
                );
                postSuccess(callback, dto);
            }
        });
    }

    // =========================================================================
    // 2. DEMO TRANSACTIONS (FIRESTORE: demo_transactions/{txId})
    // =========================================================================

    public void syncTransactions(Callback<List<TransactionNetworkDto>> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();
            String userId = sessionManager != null ? sessionManager.getUserId() : myUid;

            // Serve Room cache first
            List<TransactionEntity> cached = db.communicationDao().getAllTransactions();
            if (cached != null && !cached.isEmpty()) {
                List<TransactionNetworkDto> dtoList = new ArrayList<>();
                for (TransactionEntity e : cached) {
                    dtoList.add(new TransactionNetworkDto(
                            1L, e.getId(), e.getTitle(), e.getCategory(), e.getAmount(),
                            e.getCategory(), e.getStatus(), e.getDate(), e.getDate(),
                            1L, null, DEMO_DISCLAIMER, DEMO_NOTICE
                    ));
                }
                postSuccess(callback, dtoList);
            }

            if (firestore != null) {
                firestore.collection("demo_transactions")
                        .whereEqualTo("userId", myUid)
                        .get()
                        .addOnSuccessListener(snapshots -> executor.execute(() -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                List<TransactionNetworkDto> dtoList = new ArrayList<>();
                                List<TransactionEntity> entities = new ArrayList<>();

                                for (QueryDocumentSnapshot snap : snapshots) {
                                    Map<String, Object> data = snap.getData();
                                    String docId = snap.getId();

                                    String title = data.get("title") != null ? String.valueOf(data.get("title")) : "Demo Transaction";
                                    String cat = data.get("category") != null ? String.valueOf(data.get("category")) : "SIMULATION";
                                    String status = data.get("status") != null ? String.valueOf(data.get("status")) : "COMPLETED";
                                    double amount = 0.0;
                                    if (data.get("amount") instanceof Number) {
                                        amount = ((Number) data.get("amount")).doubleValue();
                                    }
                                    long ts = System.currentTimeMillis();
                                    if (data.get("timestamp") instanceof Number) {
                                        ts = ((Number) data.get("timestamp")).longValue();
                                    }
                                    String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(ts));

                                    TransactionEntity entity = new TransactionEntity(docId, title, cat, amount, dateStr, status, userId);
                                    entities.add(entity);

                                    dtoList.add(new TransactionNetworkDto(
                                            1L, docId, title, cat, amount, cat, status, dateStr, dateStr,
                                            1L, null, DEMO_DISCLAIMER, DEMO_NOTICE
                                    ));
                                }

                                if (!entities.isEmpty()) {
                                    db.communicationDao().insertTransactions(entities);
                                    postSuccess(callback, dtoList);
                                    return;
                                }
                            }
                            postSuccess(callback, new ArrayList<>());
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            fallbackLocalTransactions(callback);
                        }));
            } else {
                fallbackLocalTransactions(callback);
            }
        });
    }

    private void fallbackLocalTransactions(Callback<List<TransactionNetworkDto>> callback) {
        List<TransactionEntity> list = db.communicationDao().getAllTransactions();
        List<TransactionNetworkDto> dtoList = new ArrayList<>();
        if (list != null) {
            for (TransactionEntity e : list) {
                dtoList.add(new TransactionNetworkDto(
                        1L, e.getId(), e.getTitle(), e.getCategory(), e.getAmount(),
                        e.getCategory(), e.getStatus(), e.getDate(), e.getDate(),
                        1L, null, DEMO_DISCLAIMER, DEMO_NOTICE
                ));
            }
        }
        postSuccess(callback, dtoList);
    }

    private void recordFirestoreTransaction(String userId, double amount, String type,
                                           String title, String category, String status, String note) {
        long now = System.currentTimeMillis();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now));

        if (firestore != null) {
            DocumentReference txRef = firestore.collection("demo_transactions").document();
            String txId = txRef.getId();

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("amount", amount);
            data.put("type", type);
            data.put("title", title);
            data.put("category", category);
            data.put("status", status);
            data.put("timestamp", now);
            data.put("disclaimer", DEMO_DISCLAIMER);
            if (note != null && !note.isEmpty()) {
                data.put("note", note);
            }

            txRef.set(data).addOnSuccessListener(aVoid -> {
                db.communicationDao().insertTransaction(new TransactionEntity(
                        txId, title, category, amount, dateStr, status, userId
                ));
            }).addOnFailureListener(e -> {
                db.communicationDao().insertTransaction(new TransactionEntity(
                        "TX-" + now, title, category, amount, dateStr, status, userId
                ));
            });
        } else {
            db.communicationDao().insertTransaction(new TransactionEntity(
                    "TX-" + now, title, category, amount, dateStr, status, userId
            ));
        }
    }

    // =========================================================================
    // 3. DEMO ESCROWS (FIRESTORE: demo_escrows/{escrowId})
    // =========================================================================

    public void createEscrow(String title, String description, double amount, long payeeId, Long jobId, Callback<EscrowNetworkDto> callback) {
        executor.execute(() -> {
            if (amount <= 0) {
                postError(callback, "Escrow amount must be greater than zero.");
                return;
            }

            String myUid = resolveCurrentUid();
            String payeeUid = String.valueOf(payeeId);
            long now = System.currentTimeMillis();

            if (firestore != null) {
                DocumentReference walletRef = firestore.collection("users")
                        .document(myUid)
                        .collection("demo_wallet")
                        .document("main");

                walletRef.get().addOnSuccessListener(snap -> executor.execute(() -> {
                    double avail = 0.0;
                    double escrow = 0.0;
                    if (snap != null && snap.exists()) {
                        if (snap.get("availableBalance") instanceof Number) {
                            avail = ((Number) snap.get("availableBalance")).doubleValue();
                        }
                        if (snap.get("escrowBalance") instanceof Number) {
                            escrow = ((Number) snap.get("escrowBalance")).doubleValue();
                        }
                    }

                    if (avail < amount) {
                        postError(callback, "Insufficient available demo balance to fund escrow.");
                        return;
                    }

                    double newAvail = avail - amount;
                    double newEscrow = escrow + amount;

                    Map<String, Object> walletUpdates = new HashMap<>();
                    walletUpdates.put("availableBalance", newAvail);
                    walletUpdates.put("escrowBalance", newEscrow);
                    walletUpdates.put("updatedAt", now);

                    walletRef.update(walletUpdates).addOnSuccessListener(aVoid -> executor.execute(() -> {
                        updateLocalBalance(newAvail, newEscrow);

                        DocumentReference escrowRef = firestore.collection("demo_escrows").document();
                        String docId = escrowRef.getId();
                        long localId = parseOrGenerateEscrowId(docId, null);

                        Map<String, Object> data = new HashMap<>();
                        data.put("employerUid", myUid);
                        data.put("freelancerUid", payeeUid);
                        data.put("amount", amount);
                        data.put("currency", "BDT");
                        data.put("title", title != null ? title : "Demo Project Escrow");
                        data.put("description", description != null ? description : "Simulated Escrow Contract");
                        data.put("jobId", jobId != null ? jobId : 0L);
                        data.put("status", "HELD");
                        data.put("createdAt", now);
                        data.put("updatedAt", now);

                        escrowRef.set(data).addOnSuccessListener(v -> executor.execute(() -> {
                            recordFirestoreTransaction(myUid, amount, "ESCROW_LOCK",
                                    "Escrow Funded: " + title, "ESCROW", "COMPLETED", "Simulated Escrow Lock");

                            EscrowNetworkDto dto = new EscrowNetworkDto(
                                    localId, "ESC-" + localId, title, description, amount, "HELD",
                                    1L, "Employer", payeeId, "Freelancer", jobId,
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                                    DEMO_DISCLAIMER, DEMO_NOTICE
                            );
                            postSuccess(callback, dto);
                        })).addOnFailureListener(e -> executor.execute(() -> {
                            postError(callback, "Failed to create escrow contract: " + e.getMessage());
                        }));
                    })).addOnFailureListener(e -> executor.execute(() -> {
                        postError(callback, "Failed to lock wallet funds for escrow: " + e.getMessage());
                    }));
                })).addOnFailureListener(e -> executor.execute(() -> {
                    postError(callback, "Failed to read wallet: " + e.getMessage());
                }));
            } else {
                long localId = ESCROW_ID_GEN.incrementAndGet();
                EscrowNetworkDto dto = new EscrowNetworkDto(
                        localId, "ESC-" + localId, title, description, amount, "HELD",
                        1L, "Employer", payeeId, "Freelancer", jobId,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                        DEMO_DISCLAIMER, DEMO_NOTICE
                );
                postSuccess(callback, dto);
            }
        });
    }

    public void getEscrows(Callback<List<EscrowNetworkDto>> callback) {
        executor.execute(() -> {
            String myUid = resolveCurrentUid();

            if (firestore != null) {
                // Query escrows where current user is employer
                firestore.collection("demo_escrows")
                        .whereEqualTo("employerUid", myUid)
                        .get()
                        .addOnSuccessListener(empSnaps -> executor.execute(() -> {
                            List<EscrowNetworkDto> list = new ArrayList<>();
                            if (empSnaps != null) {
                                for (QueryDocumentSnapshot snap : empSnaps) {
                                    list.add(parseEscrowDto(snap));
                                }
                            }
                            // Also query escrows where current user is freelancer
                            firestore.collection("demo_escrows")
                                    .whereEqualTo("freelancerUid", myUid)
                                    .get()
                                    .addOnSuccessListener(flSnaps -> executor.execute(() -> {
                                        if (flSnaps != null) {
                                            for (QueryDocumentSnapshot snap : flSnaps) {
                                                list.add(parseEscrowDto(snap));
                                            }
                                        }
                                        postSuccess(callback, list);
                                    }))
                                    .addOnFailureListener(e -> executor.execute(() -> {
                                        postSuccess(callback, list);
                                    }));
                        }))
                        .addOnFailureListener(e -> executor.execute(() -> {
                            postSuccess(callback, new ArrayList<>());
                        }));
            } else {
                postSuccess(callback, new ArrayList<>());
            }
        });
    }

    public void releaseEscrow(long escrowId, Callback<EscrowNetworkDto> callback) {
        executor.execute(() -> {
            String docId = ESCROW_DOC_MAP.get(escrowId);
            long now = System.currentTimeMillis();
            String myUid = resolveCurrentUid();

            if (firestore != null && docId != null) {
                DocumentReference escrowRef = firestore.collection("demo_escrows").document(docId);
                escrowRef.get().addOnSuccessListener(snap -> executor.execute(() -> {
                    if (snap != null && snap.exists()) {
                        double amount = 0.0;
                        if (snap.get("amount") instanceof Number) {
                            amount = ((Number) snap.get("amount")).doubleValue();
                        }
                        String title = snap.getString("title") != null ? snap.getString("title") : "Escrow";

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "RELEASED");
                        updates.put("updatedAt", now);
                        updates.put("resolutionNote", "Funds released by employer");

                        double finalAmount = amount;
                        escrowRef.update(updates).addOnSuccessListener(v -> executor.execute(() -> {
                            // Update employer wallet escrow balance
                            adjustEmployerEscrow(myUid, -finalAmount, 0.0);

                            recordFirestoreTransaction(myUid, finalAmount, "ESCROW_RELEASE",
                                    "Escrow Released: " + title, "ESCROW", "COMPLETED", "Released to payee");

                            EscrowNetworkDto dto = new EscrowNetworkDto(
                                    escrowId, "ESC-" + escrowId, title, snap.getString("description"),
                                    finalAmount, "RELEASED", 1L, "Employer", 2L, "Freelancer",
                                    snap.getLong("jobId"),
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                                    DEMO_DISCLAIMER, DEMO_NOTICE
                            );
                            postSuccess(callback, dto);
                        })).addOnFailureListener(e -> executor.execute(() -> {
                            postError(callback, "Failed to release escrow: " + e.getMessage());
                        }));
                    } else {
                        postError(callback, "Escrow contract not found.");
                    }
                })).addOnFailureListener(e -> executor.execute(() -> {
                    postError(callback, "Failed to fetch escrow contract: " + e.getMessage());
                }));
            } else {
                EscrowNetworkDto dto = new EscrowNetworkDto(
                        escrowId, "ESC-" + escrowId, "Simulated Escrow", "Released", 0.0, "RELEASED",
                        1L, "Employer", 2L, "Freelancer", null,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                        DEMO_DISCLAIMER, DEMO_NOTICE
                );
                postSuccess(callback, dto);
            }
        });
    }

    public void refundEscrow(long escrowId, Callback<EscrowNetworkDto> callback) {
        executor.execute(() -> {
            String docId = ESCROW_DOC_MAP.get(escrowId);
            long now = System.currentTimeMillis();
            String myUid = resolveCurrentUid();

            if (firestore != null && docId != null) {
                DocumentReference escrowRef = firestore.collection("demo_escrows").document(docId);
                escrowRef.get().addOnSuccessListener(snap -> executor.execute(() -> {
                    if (snap != null && snap.exists()) {
                        double amount = 0.0;
                        if (snap.get("amount") instanceof Number) {
                            amount = ((Number) snap.get("amount")).doubleValue();
                        }
                        String title = snap.getString("title") != null ? snap.getString("title") : "Escrow";

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "REFUNDED");
                        updates.put("updatedAt", now);
                        updates.put("resolutionNote", "Refunded to employer");

                        double finalAmount = amount;
                        escrowRef.update(updates).addOnSuccessListener(v -> executor.execute(() -> {
                            // Update employer wallet: return to availableBalance, subtract from escrowBalance
                            adjustEmployerEscrow(myUid, -finalAmount, finalAmount);

                            recordFirestoreTransaction(myUid, finalAmount, "ESCROW_REFUND",
                                    "Escrow Refunded: " + title, "ESCROW", "COMPLETED", "Refunded to employer");

                            EscrowNetworkDto dto = new EscrowNetworkDto(
                                    escrowId, "ESC-" + escrowId, title, snap.getString("description"),
                                    finalAmount, "REFUNDED", 1L, "Employer", 2L, "Freelancer",
                                    snap.getLong("jobId"),
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                                    DEMO_DISCLAIMER, DEMO_NOTICE
                            );
                            postSuccess(callback, dto);
                        })).addOnFailureListener(e -> executor.execute(() -> {
                            postError(callback, "Failed to refund escrow: " + e.getMessage());
                        }));
                    } else {
                        postError(callback, "Escrow contract not found.");
                    }
                })).addOnFailureListener(e -> executor.execute(() -> {
                    postError(callback, "Failed to fetch escrow contract: " + e.getMessage());
                }));
            } else {
                EscrowNetworkDto dto = new EscrowNetworkDto(
                        escrowId, "ESC-" + escrowId, "Simulated Escrow", "Refunded", 0.0, "REFUNDED",
                        1L, "Employer", 2L, "Freelancer", null,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                        DEMO_DISCLAIMER, DEMO_NOTICE
                );
                postSuccess(callback, dto);
            }
        });
    }

    public void disputeEscrow(long escrowId, String reason, Callback<EscrowNetworkDto> callback) {
        executor.execute(() -> {
            String docId = ESCROW_DOC_MAP.get(escrowId);
            long now = System.currentTimeMillis();

            if (firestore != null && docId != null) {
                DocumentReference escrowRef = firestore.collection("demo_escrows").document(docId);
                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "DISPUTED");
                updates.put("submissionNote", reason != null ? reason : "Disputed by party");
                updates.put("updatedAt", now);

                escrowRef.update(updates).addOnSuccessListener(v -> executor.execute(() -> {
                    EscrowNetworkDto dto = new EscrowNetworkDto(
                            escrowId, "ESC-" + escrowId, "Disputed Escrow", reason, 0.0, "DISPUTED",
                            1L, "Employer", 2L, "Freelancer", null,
                            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                            DEMO_DISCLAIMER, DEMO_NOTICE
                    );
                    postSuccess(callback, dto);
                })).addOnFailureListener(e -> executor.execute(() -> {
                    postError(callback, "Failed to dispute escrow: " + e.getMessage());
                }));
            } else {
                EscrowNetworkDto dto = new EscrowNetworkDto(
                        escrowId, "ESC-" + escrowId, "Disputed Escrow", reason, 0.0, "DISPUTED",
                        1L, "Employer", 2L, "Freelancer", null,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(now)),
                        DEMO_DISCLAIMER, DEMO_NOTICE
                );
                postSuccess(callback, dto);
            }
        });
    }

    private void adjustEmployerEscrow(String uid, double escrowDelta, double availDelta) {
        if (firestore == null) return;
        DocumentReference walletRef = firestore.collection("users")
                .document(uid)
                .collection("demo_wallet")
                .document("main");

        walletRef.get().addOnSuccessListener(snap -> {
            if (snap != null && snap.exists()) {
                double avail = 0.0;
                double escrow = 0.0;
                if (snap.get("availableBalance") instanceof Number) {
                    avail = ((Number) snap.get("availableBalance")).doubleValue();
                }
                if (snap.get("escrowBalance") instanceof Number) {
                    escrow = ((Number) snap.get("escrowBalance")).doubleValue();
                }

                double newEscrow = Math.max(0.0, escrow + escrowDelta);
                double newAvail = Math.max(0.0, avail + availDelta);

                Map<String, Object> updates = new HashMap<>();
                updates.put("escrowBalance", newEscrow);
                if (availDelta != 0.0) {
                    updates.put("availableBalance", newAvail);
                }
                updates.put("updatedAt", System.currentTimeMillis());

                walletRef.update(updates).addOnSuccessListener(v -> {
                    updateLocalBalance(newAvail, newEscrow);
                });
            }
        });
    }

    private EscrowNetworkDto parseEscrowDto(QueryDocumentSnapshot snap) {
        String docId = snap.getId();
        long localId = parseOrGenerateEscrowId(docId, null);
        Map<String, Object> data = snap.getData();

        String title = data.get("title") != null ? String.valueOf(data.get("title")) : "Demo Escrow";
        String desc = data.get("description") != null ? String.valueOf(data.get("description")) : "";
        String status = data.get("status") != null ? String.valueOf(data.get("status")) : "HELD";
        double amount = 0.0;
        if (data.get("amount") instanceof Number) {
            amount = ((Number) data.get("amount")).doubleValue();
        }
        Long jobId = null;
        if (data.get("jobId") instanceof Number) {
            jobId = ((Number) data.get("jobId")).longValue();
        }
        long createdAt = System.currentTimeMillis();
        if (data.get("createdAt") instanceof Number) {
            createdAt = ((Number) data.get("createdAt")).longValue();
        }
        long updatedAt = createdAt;
        if (data.get("updatedAt") instanceof Number) {
            updatedAt = ((Number) data.get("updatedAt")).longValue();
        }
        String createdStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(createdAt));
        String updatedStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(updatedAt));

        return new EscrowNetworkDto(
                localId, "ESC-" + localId, title, desc, amount, status,
                1L, "Employer", 2L, "Freelancer", jobId,
                createdStr, updatedStr, DEMO_DISCLAIMER, DEMO_NOTICE
        );
    }

    private void updateLocalBalance(double available, double escrow) {
        String userId = sessionManager != null ? sessionManager.getUserId() : "student_1";
        if (userId != null && !userId.isBlank()) {
            db.userDao().updateBalance(userId, available, escrow);
        }
    }
}
