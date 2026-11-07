package com.teligati.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.teligati.app.data.database.AppDatabase;
import com.teligati.app.data.entity.*;
import com.teligati.app.network.model.ReferenceCreateRequest;
import com.teligati.app.network.model.ReferenceNetworkDto;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppRepository {
    private static volatile AppRepository INSTANCE;
    private final AppDatabase db;
    private final OpportunityRepository opportunityRepo;
    private final WalletRepository walletRepo;
    private final ReferenceRepository referenceRepo;
    private final MessagingRepository messagingRepo;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface Callback<T> {
        void onResult(T result);
    }

    private AppRepository(Context context) {
        this.db = AppDatabase.getInstance(context);
        this.opportunityRepo = OpportunityRepository.getInstance(context);
        this.walletRepo = WalletRepository.getInstance(context);
        this.referenceRepo = ReferenceRepository.getInstance(context);
        this.messagingRepo = MessagingRepository.getInstance(context);
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public AppRepository(AppDatabase db, OpportunityRepository opportunityRepo, WalletRepository walletRepo, ReferenceRepository referenceRepo, MessagingRepository messagingRepo, ExecutorService executor, Handler mainHandler) {
        this.db = db;
        this.opportunityRepo = opportunityRepo;
        this.walletRepo = walletRepo;
        this.referenceRepo = referenceRepo;
        this.messagingRepo = messagingRepo;
        this.executor = executor != null ? executor : Executors.newFixedThreadPool(4);
        this.mainHandler = mainHandler != null ? mainHandler : new Handler(Looper.getMainLooper());
    }

    public AppRepository(AppDatabase db, OpportunityRepository opportunityRepo, WalletRepository walletRepo, ReferenceRepository referenceRepo, ExecutorService executor, Handler mainHandler) {
        this(db, opportunityRepo, walletRepo, referenceRepo, null, executor, mainHandler);
    }

    public AppRepository(AppDatabase db, OpportunityRepository opportunityRepo, WalletRepository walletRepo, ExecutorService executor, Handler mainHandler) {
        this(db, opportunityRepo, walletRepo, null, executor, mainHandler);
    }

    public AppRepository(AppDatabase db, OpportunityRepository opportunityRepo, ExecutorService executor, Handler mainHandler) {
        this(db, opportunityRepo, null, executor, mainHandler);
    }

    public static AppRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private <T> void postResult(Callback<T> callback, T result) {
        if (callback != null) {
            mainHandler.post(() -> callback.onResult(result));
        }
    }

    public void ensureSeeded(Callback<Void> callback) {
        executor.execute(() -> {
            if (db.jobDao().getAllJobs().isEmpty()) {
                AppDatabase.seedInitialData(db);
            }
            postResult(callback, null);
        });
    }

    // ==========================================
    // Student Operations
    // ==========================================

    public void getAssistantJobs(Callback<List<JobPostingEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncAssistantJobs(new OpportunityRepository.Callback<List<JobPostingEntity>>() {
                @Override
                public void onSuccess(List<JobPostingEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<JobPostingEntity> list = db.jobDao().getJobsByCategory("ASSISTANT_WORK");
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<JobPostingEntity> list = db.jobDao().getJobsByCategory("ASSISTANT_WORK");
                postResult(callback, list);
            });
        }
    }

    public void getProfessionalJobs(Callback<List<JobPostingEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncProfessionalJobs(new OpportunityRepository.Callback<List<JobPostingEntity>>() {
                @Override
                public void onSuccess(List<JobPostingEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<JobPostingEntity> list = db.jobDao().getJobsByCategory("FULLTIME_JOBS");
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<JobPostingEntity> list = db.jobDao().getJobsByCategory("FULLTIME_JOBS");
                postResult(callback, list);
            });
        }
    }

    public void getReferenceOpportunities(Callback<List<JobPostingEntity>> callback) {
        executor.execute(() -> {
            List<JobPostingEntity> list = db.jobDao().getJobsByCategories(Arrays.asList("FULLTIME_JOBS", "RESEARCH_COLLABORATION"));
            postResult(callback, list);
        });
    }

    public void getResearchOpportunities(Callback<List<JobPostingEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncResearchOpportunities(new OpportunityRepository.Callback<List<JobPostingEntity>>() {
                @Override
                public void onSuccess(List<JobPostingEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<JobPostingEntity> list = db.jobDao().getJobsByCategory("RESEARCH_COLLABORATION");
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<JobPostingEntity> list = db.jobDao().getJobsByCategory("RESEARCH_COLLABORATION");
                postResult(callback, list);
            });
        }
    }

    public void checkApplicationExists(long jobId, String studentRoll, Callback<Boolean> callback) {
        executor.execute(() -> {
            int count = db.applicationDao().checkApplicationExists(jobId, studentRoll);
            postResult(callback, count > 0);
        });
    }

    public void applyForJob(long jobId, String studentRoll, String studentName, String studentDept,
                            double cgpa, String skills, String statement, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.applyForJob(jobId, studentRoll, studentName, studentDept, cgpa, skills, statement,
                    new OpportunityRepository.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            executor.execute(() -> {
                                JobPostingEntity job = db.jobDao().getJobById(jobId);
                                String title = (job != null) ? job.getTitle() : "Position #" + jobId;
                                db.communicationDao().insertNotification(new NotificationEntity(
                                        0, studentRoll, "Application Submitted",
                                        "You applied for: " + title + ". Awaiting review.",
                                        System.currentTimeMillis(), "Just now", false
                                ));
                            });
                            postResult(callback, result);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            postResult(callback, false);
                        }
                    });
        } else {
            executor.execute(() -> {
                int existing = db.applicationDao().checkApplicationExists(jobId, studentRoll);
                if (existing > 0) {
                    postResult(callback, false);
                    return;
                }

                JobPostingEntity job = db.jobDao().getJobById(jobId);
                String title = (job != null) ? job.getTitle() : "Position #" + jobId;

                JobApplicationEntity app = new JobApplicationEntity(
                        0, jobId, title, studentRoll, studentName, studentRoll, studentDept,
                        cgpa, skills, statement, "Applied", System.currentTimeMillis()
                );
                db.applicationDao().insertJobApplication(app);
                db.jobDao().setJobApplied(jobId, true);

                db.communicationDao().insertNotification(new NotificationEntity(
                        0, studentRoll, "Application Submitted",
                        "You applied for: " + title + ". Awaiting review.",
                        System.currentTimeMillis(), "Just now", false
                ));

                postResult(callback, true);
            });
        }
    }

    public void getJobApplicationsByStudent(String studentRoll, Callback<List<JobApplicationEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncStudentJobApplications(studentRoll, new OpportunityRepository.Callback<List<JobApplicationEntity>>() {
                @Override
                public void onSuccess(List<JobApplicationEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<JobApplicationEntity> list = db.applicationDao().getApplicationsByStudent(studentRoll);
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<JobApplicationEntity> list = db.applicationDao().getApplicationsByStudent(studentRoll);
                postResult(callback, list);
            });
        }
    }

    public void getApplicationsForJob(long jobId, Callback<List<JobApplicationEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.getApplicationsForJob(jobId, new OpportunityRepository.Callback<List<JobApplicationEntity>>() {
                @Override
                public void onSuccess(List<JobApplicationEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<JobApplicationEntity> list = db.applicationDao().getApplicationsForJob(jobId);
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<JobApplicationEntity> list = db.applicationDao().getApplicationsForJob(jobId);
                postResult(callback, list);
            });
        }
    }

    public void withdrawJobApplication(long id, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.withdrawJobApplication(id, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            executor.execute(() -> {
                db.applicationDao().updateJobApplicationStatus(id, "WITHDRAWN");
                postResult(callback, true);
            });
        }
    }

    public void getAllJobApplications(Callback<List<JobApplicationEntity>> callback) {
        executor.execute(() -> {
            List<JobApplicationEntity> list = db.applicationDao().getAllJobApplications();
            postResult(callback, list);
        });
    }

    public void updateJobApplicationStatus(long id, String status, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.updateJobApplicationStatus(id, status, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    executor.execute(() -> {
                        db.applicationDao().updateJobApplicationStatus(id, status);
                        JobApplicationEntity app = db.applicationDao().getApplicationById(id);
                        if (app != null) {
                            db.communicationDao().insertNotification(new NotificationEntity(
                                    0, app.getApplicantRoll(), "Application Status: " + status,
                                    "Your application for " + app.getJobTitle() + " is now marked as " + status + ".",
                                    System.currentTimeMillis(), "Just now", false
                            ));
                        }
                    });
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        db.applicationDao().updateJobApplicationStatus(id, status);
                        JobApplicationEntity app = db.applicationDao().getApplicationById(id);
                        if (app != null) {
                            db.communicationDao().insertNotification(new NotificationEntity(
                                    0, app.getApplicantRoll(), "Application Status: " + status,
                                    "Your application for " + app.getJobTitle() + " is now marked as " + status + ".",
                                    System.currentTimeMillis(), "Just now", false
                            ));
                        }
                        postResult(callback, true);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                db.applicationDao().updateJobApplicationStatus(id, status);
                JobApplicationEntity app = db.applicationDao().getApplicationById(id);
                if (app != null) {
                    db.communicationDao().insertNotification(new NotificationEntity(
                            0, app.getApplicantRoll(), "Application Status: " + status,
                            "Your application for " + app.getJobTitle() + " is now marked as " + status + ".",
                            System.currentTimeMillis(), "Just now", false
                    ));
                }
                postResult(callback, true);
            });
        }
    }

    public void checkResearchApplicationExists(long oppId, String studentRoll, Callback<Boolean> callback) {
        executor.execute(() -> {
            int count = db.researchDao().checkResearchApplicationExists(oppId, studentRoll);
            postResult(callback, count > 0);
        });
    }

    public void expressInterestInResearch(long oppId, String oppTitle, String studentRoll, String studentName,
                                          String dept, double cgpa, String skills, String note, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.expressInterestInResearch(oppId, oppTitle, studentRoll, studentName, dept, cgpa, skills, note,
                    new OpportunityRepository.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            executor.execute(() -> {
                                db.communicationDao().insertNotification(new NotificationEntity(
                                        0, studentRoll, "Research Interest Expressed",
                                        "You registered interest for '" + oppTitle + "'. Lab faculty will review your profile.",
                                        System.currentTimeMillis(), "Just now", false
                                ));
                            });
                            postResult(callback, result);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            postResult(callback, false);
                        }
                    });
        } else {
            executor.execute(() -> {
                int count = db.researchDao().checkResearchApplicationExists(oppId, studentRoll);
                if (count > 0) {
                    postResult(callback, false);
                    return;
                }
                ResearchApplicationEntity app = new ResearchApplicationEntity(
                        0, oppId, oppTitle, studentRoll, studentName, dept, cgpa, skills, note, "Applicant", System.currentTimeMillis()
                );
                db.researchDao().insertResearchApplication(app);
                db.communicationDao().insertNotification(new NotificationEntity(
                        0, studentRoll, "Research Interest Expressed",
                        "You registered interest for '" + oppTitle + "'. Lab faculty will review your profile.",
                        System.currentTimeMillis(), "Just now", false
                ));
                postResult(callback, true);
            });
        }
    }

    public void getResearchApplicationsByStudent(String studentRoll, Callback<List<ResearchApplicationEntity>> callback) {
        executor.execute(() -> {
            List<ResearchApplicationEntity> list = db.researchDao().getResearchApplicationsByStudent(studentRoll);
            postResult(callback, list);
        });
    }

    public void getStudentReferenceRequests(String studentRoll, Callback<List<ReferenceRequestEntity>> callback) {
        if (referenceRepo != null) {
            referenceRepo.getSentRequests(new ReferenceRepository.Callback<List<ReferenceRequestEntity>>() {
                @Override
                public void onSuccess(List<ReferenceRequestEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsByStudent(studentRoll);
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsByStudent(studentRoll);
                postResult(callback, list);
            });
        }
    }

    public void submitReferenceRequest(String studentRoll, String studentName, String studentDept,
                                       String targetCompany, String message, String alumniId,
                                       Callback<Boolean> callback) {
        if (referenceRepo != null) {
            ReferenceCreateRequest req = new ReferenceCreateRequest(
                    alumniId, "JOB_REFERRAL", targetCompany, null, null, message, "Applicant: " + studentRoll + " (" + studentDept + ")"
            );
            referenceRepo.submitReferenceRequest(req, new ReferenceRepository.Callback<ReferenceNetworkDto>() {
                @Override
                public void onSuccess(ReferenceNetworkDto result) {
                    postResult(callback, true);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        ReferenceRequestEntity entity = new ReferenceRequestEntity(
                                0, studentRoll, studentName, studentDept, targetCompany, message, "PENDING", alumniId, System.currentTimeMillis()
                        );
                        db.referenceRequestDao().insertRequest(entity);
                        postResult(callback, true);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                ReferenceRequestEntity req = new ReferenceRequestEntity(
                        0, studentRoll, studentName, studentDept, targetCompany, message, "Pending", alumniId, System.currentTimeMillis()
                );
                db.referenceRequestDao().insertRequest(req);
                postResult(callback, true);
            });
        }
    }

    public void getStudentHistory(String studentRoll, Callback<List<TransactionEntity>> callback) {
        executor.execute(() -> {
            List<TransactionEntity> list = db.communicationDao().getAllTransactions();
            postResult(callback, list);
        });
    }

    public void getStudentProfile(String roll, Callback<StudentProfileEntity> callback) {
        executor.execute(() -> {
            StudentProfileEntity profile = db.profileDao().getStudentProfile(roll);
            postResult(callback, profile);
        });
    }

    public void updateStudentProfile(StudentProfileEntity profile, String callerRoll, Callback<Boolean> callback) {
        executor.execute(() -> {
            if (callerRoll != null && !callerRoll.equalsIgnoreCase("admin") && !callerRoll.equalsIgnoreCase(profile.getRoll())) {
                postResult(callback, false);
                return;
            }
            db.profileDao().updateStudentProfile(profile);
            postResult(callback, true);
        });
    }

    public void updateStudentProfile(StudentProfileEntity profile, Callback<Boolean> callback) {
        updateStudentProfile(profile, "admin", callback);
    }

    public void getAlumniDirectory(Callback<List<AlumniProfileEntity>> callback) {
        executor.execute(() -> {
            List<AlumniProfileEntity> list = db.profileDao().getAllAlumniProfiles();
            postResult(callback, list);
        });
    }

    // ==========================================
    // Alumni Operations
    // ==========================================

    public void createJobPosting(JobPostingEntity job, Callback<Long> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.createJobPosting(job, new OpportunityRepository.Callback<Long>() {
                @Override
                public void onSuccess(Long result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        long id = db.jobDao().insertJob(job);
                        postResult(callback, id);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                long id = db.jobDao().insertJob(job);
                postResult(callback, id);
            });
        }
    }

    public void getAlumniJobs(String posterId, Callback<List<JobPostingEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncAlumniJobs(posterId, new OpportunityRepository.Callback<List<JobPostingEntity>>() {
                @Override
                public void onSuccess(List<JobPostingEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<JobPostingEntity> list;
                        if (posterId == null || posterId.isEmpty() || "ALL".equalsIgnoreCase(posterId)) {
                            list = db.jobDao().getAllJobs();
                        } else {
                            list = db.jobDao().getJobsByPoster(posterId);
                        }
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<JobPostingEntity> list;
                if (posterId == null || posterId.isEmpty() || "ALL".equalsIgnoreCase(posterId)) {
                    list = db.jobDao().getAllJobs();
                } else {
                    list = db.jobDao().getJobsByPoster(posterId);
                }
                postResult(callback, list);
            });
        }
    }

    public void deleteJobPosting(long id, String callerUserId, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.deleteJobPosting(id, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            executor.execute(() -> {
                JobPostingEntity job = db.jobDao().getJobById(id);
                if (job == null) {
                    postResult(callback, false);
                    return;
                }
                boolean isOwner = callerUserId != null && (
                        callerUserId.equalsIgnoreCase("admin") ||
                        callerUserId.equalsIgnoreCase(job.getPostedByUserId()) ||
                        callerUserId.equalsIgnoreCase(job.getPostedByName())
                );
                if (!isOwner) {
                    postResult(callback, false);
                    return;
                }
                // Cascade delete applications for this job to prevent orphaned records
                db.applicationDao().deleteApplicationsForJob(id);
                db.jobDao().deleteJob(id);
                postResult(callback, true);
            });
        }
    }

    public void deleteJobPosting(long id, Callback<Boolean> callback) {
        deleteJobPosting(id, "admin", callback);
    }

    public void updateJobPosting(JobPostingEntity job, String callerUserId, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.updateJobPosting(job, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            executor.execute(() -> {
                JobPostingEntity existing = db.jobDao().getJobById(job.getId());
                if (existing != null) {
                    boolean isOwner = callerUserId != null && (
                            callerUserId.equalsIgnoreCase("admin") ||
                            callerUserId.equalsIgnoreCase(existing.getPostedByUserId()) ||
                            callerUserId.equalsIgnoreCase(existing.getPostedByName())
                    );
                    if (!isOwner) {
                        postResult(callback, false);
                        return;
                    }
                }
                db.jobDao().updateJob(job);
                postResult(callback, true);
            });
        }
    }

    public void updateJobPosting(JobPostingEntity job, Callback<Boolean> callback) {
        updateJobPosting(job, "admin", callback);
    }

    public void getAlumniJobApplications(String alumniId, String alumniName, Callback<List<JobApplicationEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncAlumniJobApplications(alumniId, alumniName, new OpportunityRepository.Callback<List<JobApplicationEntity>>() {
                @Override
                public void onSuccess(List<JobApplicationEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<JobApplicationEntity> list = db.applicationDao().getJobApplicationsForAlumni(alumniId, alumniName);
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<JobApplicationEntity> list = db.applicationDao().getJobApplicationsForAlumni(alumniId, alumniName);
                postResult(callback, list);
            });
        }
    }

    public void getAlumniReferenceRequests(String alumniId, String alumniName, Callback<List<ReferenceRequestEntity>> callback) {
        if (referenceRepo != null) {
            referenceRepo.getIncomingRequests(null, new ReferenceRepository.Callback<List<ReferenceRequestEntity>>() {
                @Override
                public void onSuccess(List<ReferenceRequestEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsForAlumni(alumniId, alumniName);
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<ReferenceRequestEntity> list = db.referenceRequestDao().getRequestsForAlumni(alumniId, alumniName);
                postResult(callback, list);
            });
        }
    }

    public void getReferenceRequests(Callback<List<ReferenceRequestEntity>> callback) {
        executor.execute(() -> {
            List<ReferenceRequestEntity> list = db.referenceRequestDao().getAllRequests();
            postResult(callback, list);
        });
    }

    public void updateReferenceRequestStatus(long id, String status, Callback<Boolean> callback) {
        String upper = status != null ? status.toUpperCase().trim() : "";
        if (upper.contains("GUID")) {
            // Local mentorship/guidance status note
            executor.execute(() -> {
                db.referenceRequestDao().updateRequestStatus(id, status);
                postResult(callback, true);
            });
            return;
        }

        final String normalized = upper.contains("ACCEPT") || upper.contains("ENDORSE") ? "ACCEPTED" : "DECLINED";
        if (referenceRepo != null) {
            referenceRepo.decideRequest(id, normalized, "Updated by referee", new ReferenceRepository.Callback<ReferenceNetworkDto>() {
                @Override
                public void onSuccess(ReferenceNetworkDto result) {
                    executor.execute(() -> {
                        db.referenceRequestDao().updateRequestStatus(id, normalized);
                        ReferenceRequestEntity req = db.referenceRequestDao().getRequestById(id);
                        if (req != null) {
                            db.communicationDao().insertNotification(new NotificationEntity(
                                    0, req.getStudentRoll(), "Reference Request: " + normalized,
                                    "Your reference request for " + req.getTargetCompany() + " was marked as: " + normalized,
                                    System.currentTimeMillis(), "Just now", false
                            ));
                        }
                        postResult(callback, true);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        db.referenceRequestDao().updateRequestStatus(id, normalized);
                        ReferenceRequestEntity req = db.referenceRequestDao().getRequestById(id);
                        if (req != null) {
                            db.communicationDao().insertNotification(new NotificationEntity(
                                    0, req.getStudentRoll(), "Reference Request: " + normalized,
                                    "Your reference request for " + req.getTargetCompany() + " was marked as: " + normalized,
                                    System.currentTimeMillis(), "Just now", false
                            ));
                        }
                        postResult(callback, true);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                db.referenceRequestDao().updateRequestStatus(id, normalized);
                ReferenceRequestEntity req = db.referenceRequestDao().getRequestById(id);
                if (req != null) {
                    db.communicationDao().insertNotification(new NotificationEntity(
                            0, req.getStudentRoll(), "Reference Request: " + normalized,
                            "Your reference request for " + req.getTargetCompany() + " was marked as: " + normalized,
                            System.currentTimeMillis(), "Just now", false
                    ));
                }
                postResult(callback, true);
            });
        }
    }

    public void getAlumniProfile(String userId, Callback<AlumniProfileEntity> callback) {
        executor.execute(() -> {
            AlumniProfileEntity profile = db.profileDao().getAlumniProfileByUserId(userId);
            postResult(callback, profile);
        });
    }

    public void updateAlumniProfile(AlumniProfileEntity profile, Callback<Boolean> callback) {
        executor.execute(() -> {
            db.profileDao().updateAlumniProfile(profile);
            postResult(callback, true);
        });
    }

    // ==========================================
    // Teacher Operations
    // ==========================================

    public void publishResearchPaper(ResearchPaperEntity paper, Callback<Long> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.publishResearchPaper(paper, new OpportunityRepository.Callback<Long>() {
                @Override
                public void onSuccess(Long result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        long id = db.researchDao().insertPaper(paper);
                        postResult(callback, id);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                long id = db.researchDao().insertPaper(paper);
                postResult(callback, id);
            });
        }
    }

    public void getResearchPapers(Callback<List<ResearchPaperEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncResearchPapers(new OpportunityRepository.Callback<List<ResearchPaperEntity>>() {
                @Override
                public void onSuccess(List<ResearchPaperEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<ResearchPaperEntity> list = db.researchDao().getAllPapers();
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<ResearchPaperEntity> list = db.researchDao().getAllPapers();
                postResult(callback, list);
            });
        }
    }

    public void getTeacherPapers(String teacherId, String teacherName, Callback<List<ResearchPaperEntity>> callback) {
        executor.execute(() -> {
            List<ResearchPaperEntity> list = db.researchDao().getPapersForTeacher(teacherId, teacherName);
            postResult(callback, list);
        });
    }

    public void deleteResearchPaper(long id, String callerUserId, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.deleteResearchPaper(id, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            executor.execute(() -> {
                ResearchPaperEntity paper = db.researchDao().getPaperById(id);
                if (paper == null) {
                    postResult(callback, false);
                    return;
                }
                boolean isOwner = callerUserId != null && (
                        callerUserId.equalsIgnoreCase("admin") ||
                        callerUserId.equalsIgnoreCase(paper.getOwnerId()) ||
                        callerUserId.equalsIgnoreCase(paper.getOwnerName())
                );
                if (!isOwner) {
                    postResult(callback, false);
                    return;
                }
                // Cascade delete applications for this opportunity to prevent orphaned records
                db.researchDao().deleteApplicationsForOpportunity(id);
                db.researchDao().deletePaper(id);
                postResult(callback, true);
            });
        }
    }

    public void deleteResearchPaper(long id, Callback<Boolean> callback) {
        deleteResearchPaper(id, "admin", callback);
    }

    public void updateResearchPaper(ResearchPaperEntity paper, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.updateResearchPaper(paper, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        db.researchDao().updatePaper(paper);
                        postResult(callback, true);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                db.researchDao().updatePaper(paper);
                postResult(callback, true);
            });
        }
    }

    public interface ProposalSubmissionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public void submitResearchProposal(String studentRoll, String studentName, String topic,
                                       String proposalAbstract, String degreeType, String teacherId,
                                       ProposalSubmissionCallback callback) {
        if (opportunityRepo != null) {
            opportunityRepo.submitResearchProposal(studentRoll, studentName, topic, proposalAbstract, degreeType, teacherId,
                    new OpportunityRepository.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean result) {
                            if (callback != null) {
                                mainHandler.post(callback::onSuccess);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (callback != null) {
                                mainHandler.post(() -> callback.onError(errorMessage));
                            }
                        }
                    });
        } else {
            executor.execute(() -> {
                ResearchProposalEntity prop = new ResearchProposalEntity(
                        0, studentRoll, studentName, topic, proposalAbstract, degreeType,
                        "Pending", teacherId, "Under review", System.currentTimeMillis()
                );
                db.researchDao().insertProposal(prop);
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            });
        }
    }

    public void submitResearchProposal(String studentRoll, String studentName, String topic,
                                       String proposalAbstract, String degreeType, String teacherId,
                                       Callback<Boolean> callback) {
        submitResearchProposal(studentRoll, studentName, topic, proposalAbstract, degreeType, teacherId, new ProposalSubmissionCallback() {
            @Override
            public void onSuccess() {
                postResult(callback, true);
            }

            @Override
            public void onError(String errorMessage) {
                postResult(callback, false);
            }
        });
    }

    public void getMyResearchProposals(String studentRoll, String studentEmail, Callback<List<ResearchProposalEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncMyResearchProposals(studentRoll, studentEmail, new OpportunityRepository.Callback<List<ResearchProposalEntity>>() {
                @Override
                public void onSuccess(List<ResearchProposalEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<ResearchProposalEntity> list = db.researchDao().getProposalsByStudent(studentRoll, studentEmail);
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<ResearchProposalEntity> list = db.researchDao().getProposalsByStudent(studentRoll, studentEmail);
                postResult(callback, list);
            });
        }
    }

    public void getResearchProposals(Callback<List<ResearchProposalEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.syncResearchProposals(new OpportunityRepository.Callback<List<ResearchProposalEntity>>() {
                @Override
                public void onSuccess(List<ResearchProposalEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<ResearchProposalEntity> list = db.researchDao().getAllProposals();
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<ResearchProposalEntity> list = db.researchDao().getAllProposals();
                postResult(callback, list);
            });
        }
    }

    public void getTeacherProposals(String teacherId, String teacherEmail, Callback<List<ResearchProposalEntity>> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.getTeacherProposals(teacherId, teacherEmail, new OpportunityRepository.Callback<List<ResearchProposalEntity>>() {
                @Override
                public void onSuccess(List<ResearchProposalEntity> result) {
                    executor.execute(() -> {
                        List<ResearchProposalEntity> list = db.researchDao().getProposalsForTeacher(teacherId, teacherEmail, teacherId);
                        if (list == null || list.isEmpty()) {
                            list = result;
                        }
                        postResult(callback, list);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<ResearchProposalEntity> list = db.researchDao().getProposalsForTeacher(teacherId, teacherEmail, teacherId);
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<ResearchProposalEntity> list = db.researchDao().getProposalsForTeacher(teacherId, teacherEmail, teacherId);
                postResult(callback, list);
            });
        }
    }

    public void getTeacherStudentApplicants(String teacherId, String teacherName, Callback<List<ResearchApplicationEntity>> callback) {
        executor.execute(() -> {
            List<ResearchApplicationEntity> list = db.researchDao().getResearchApplicationsForTeacher(teacherId, teacherName);
            postResult(callback, list);
        });
    }

    public void updateResearchProposalStatus(long id, String status, String feedback, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.decideResearchProposal(id, status, feedback, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    executor.execute(() -> {
                        db.researchDao().updateProposalStatus(id, status, feedback);
                        ResearchProposalEntity prop = db.researchDao().getProposalById(id);
                        if (prop != null) {
                            db.communicationDao().insertNotification(new NotificationEntity(
                                    0, prop.getStudentRoll(), "Proposal " + status,
                                    "Your proposal '" + prop.getTopic() + "' is now " + status + ". Faculty note: " + feedback,
                                    System.currentTimeMillis(), "Just now", false
                            ));
                        }
                    });
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        db.researchDao().updateProposalStatus(id, status, feedback);
                        ResearchProposalEntity prop = db.researchDao().getProposalById(id);
                        if (prop != null) {
                            db.communicationDao().insertNotification(new NotificationEntity(
                                    0, prop.getStudentRoll(), "Proposal " + status,
                                    "Your proposal '" + prop.getTopic() + "' is now " + status + ". Faculty note: " + feedback,
                                    System.currentTimeMillis(), "Just now", false
                            ));
                        }
                        postResult(callback, true);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                db.researchDao().updateProposalStatus(id, status, feedback);
                ResearchProposalEntity prop = db.researchDao().getProposalById(id);
                if (prop != null) {
                    db.communicationDao().insertNotification(new NotificationEntity(
                            0, prop.getStudentRoll(), "Proposal " + status,
                            "Your proposal '" + prop.getTopic() + "' is now " + status + ". Faculty note: " + feedback,
                            System.currentTimeMillis(), "Just now", false
                    ));
                }
                postResult(callback, true);
            });
        }
    }

    public void getStudentApplicants(Callback<List<ResearchApplicationEntity>> callback) {
        executor.execute(() -> {
            List<ResearchApplicationEntity> list = db.researchDao().getAllResearchApplications();
            postResult(callback, list);
        });
    }

    public void updateStudentApplicantStatus(long id, String status, Callback<Boolean> callback) {
        reviewResearchApplication(id, status, "Application " + status + " by faculty.", callback);
    }

    public void reviewResearchApplication(long id, String status, String feedback, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.reviewResearchApplication(id, status, feedback, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    executor.execute(() -> {
                        db.researchDao().updateResearchApplicationStatus(id, status);
                        postResult(callback, result);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        db.researchDao().updateResearchApplicationStatus(id, status);
                        postResult(callback, true);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                db.researchDao().updateResearchApplicationStatus(id, status);
                postResult(callback, true);
            });
        }
    }

    public void resubmitResearchApplication(long id, String statement, String skills, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.resubmitResearchApplication(id, statement, skills, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            executor.execute(() -> {
                db.researchDao().updateResearchApplicationStatus(id, "PENDING");
                postResult(callback, true);
            });
        }
    }

    public void withdrawResearchApplication(long id, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.withdrawResearchApplication(id, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            executor.execute(() -> {
                db.researchDao().updateResearchApplicationStatus(id, "WITHDRAWN");
                postResult(callback, true);
            });
        }
    }

    public void closeResearchOpportunity(long id, Callback<Boolean> callback) {
        if (opportunityRepo != null) {
            opportunityRepo.closeResearchOpportunity(id, new OpportunityRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            executor.execute(() -> {
                db.jobDao().deleteJob(id);
                postResult(callback, true);
            });
        }
    }

    public void getTeacherProfile(String userId, Callback<TeacherProfileEntity> callback) {
        executor.execute(() -> {
            TeacherProfileEntity profile = db.profileDao().getTeacherProfileByUserId(userId);
            postResult(callback, profile);
        });
    }

    public void updateTeacherProfile(TeacherProfileEntity profile, Callback<Boolean> callback) {
        executor.execute(() -> {
            db.profileDao().updateTeacherProfile(profile);
            postResult(callback, true);
        });
    }

    // ==========================================
    // Communication & User Operations
    // ==========================================

    public void getMessages(String userId, Callback<List<MessageEntity>> callback) {
        executor.execute(() -> {
            List<MessageEntity> list = db.communicationDao().getAllMessages();
            postResult(callback, list);
        });
    }

    public void sendMessage(MessageEntity msg, Callback<Long> callback) {
        executor.execute(() -> {
            long id = db.communicationDao().insertMessage(msg);
            postResult(callback, id);
        });
    }

    public void sendDirectMessage(String senderId, String senderName, String receiverId, String receiverName,
                                  String content, Callback<Long> callback) {
        if (messagingRepo != null) {
            messagingRepo.getOrCreateConversation(receiverId, receiverName, "USER", "KUET", "", content, new MessagingRepository.Callback<ConversationEntity>() {
                @Override
                public void onSuccess(ConversationEntity result) {
                    postResult(callback, result != null ? result.getId() : 1L);
                }

                @Override
                public void onError(String errorMessage) {
                    saveDirectMessageLocally(senderId, senderName, receiverId, receiverName, content, callback);
                }
            });
            return;
        }
        saveDirectMessageLocally(senderId, senderName, receiverId, receiverName, content, callback);
    }

    private void saveDirectMessageLocally(String senderId, String senderName, String receiverId, String receiverName,
                                          String content, Callback<Long> callback) {
        executor.execute(() -> {
            MessageEntity msg = new MessageEntity(
                    0, senderId, senderName, receiverId, receiverName, content,
                    System.currentTimeMillis(), "Just now"
            );
            long id = db.communicationDao().insertMessage(msg);
            db.communicationDao().insertNotification(new NotificationEntity(
                    0, receiverId, "New Message from " + senderName,
                    content.length() > 60 ? content.substring(0, 57) + "..." : content,
                    System.currentTimeMillis(), "Just now", false
            ));
            postResult(callback, id);
        });
    }

    public MessagingRepository getMessagingRepository() {
        return messagingRepo;
    }

    public void getConversations(Callback<List<ConversationEntity>> callback) {
        if (messagingRepo != null) {
            messagingRepo.getConversations(new MessagingRepository.Callback<List<ConversationEntity>>() {
                @Override
                public void onSuccess(List<ConversationEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<ConversationEntity> list = db.communicationDao().getAllConversations();
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<ConversationEntity> list = db.communicationDao().getAllConversations();
                postResult(callback, list);
            });
        }
    }

    public void getConversationMessages(long conversationId, Callback<List<MessageEntity>> callback) {
        if (messagingRepo != null) {
            messagingRepo.getConversationMessages(conversationId, new MessagingRepository.Callback<List<MessageEntity>>() {
                @Override
                public void onSuccess(List<MessageEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<MessageEntity> list = db.communicationDao().getMessagesForConversation(conversationId);
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<MessageEntity> list = db.communicationDao().getMessagesForConversation(conversationId);
                postResult(callback, list);
            });
        }
    }

    public void sendConversationMessage(long conversationId, String content, Callback<MessageEntity> callback) {
        if (messagingRepo != null) {
            messagingRepo.sendMessage(conversationId, content, new MessagingRepository.Callback<MessageEntity>() {
                @Override
                public void onSuccess(MessageEntity result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            executor.execute(() -> {
                MessageEntity msg = new MessageEntity(
                        0, conversationId, "sender", "User", "receiver", "Recipient",
                        content, System.currentTimeMillis(), "Just now", false
                );
                long id = db.communicationDao().insertMessage(msg);
                msg.setId(id);
                postResult(callback, msg);
            });
        }
    }

    public void markConversationAsRead(long conversationId, Callback<Void> callback) {
        if (messagingRepo != null) {
            messagingRepo.markConversationAsRead(conversationId, new MessagingRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    postResult(callback, null);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            postResult(callback, null);
        }
    }

    public void getConversation(String u1, String u2, Callback<List<MessageEntity>> callback) {
        executor.execute(() -> {
            List<MessageEntity> list = db.communicationDao().getConversationBetween(u1, u2);
            postResult(callback, list);
        });
    }

    public void markNotificationAsRead(long id, Callback<Boolean> callback) {
        if (messagingRepo != null) {
            messagingRepo.markNotificationAsRead(id, new MessagingRepository.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        db.communicationDao().markNotificationAsRead(id);
                        postResult(callback, true);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                db.communicationDao().markNotificationAsRead(id);
                postResult(callback, true);
            });
        }
    }

    public void getNotifications(String userId, Callback<List<NotificationEntity>> callback) {
        if (messagingRepo != null) {
            messagingRepo.syncNotifications(userId, new MessagingRepository.Callback<List<NotificationEntity>>() {
                @Override
                public void onSuccess(List<NotificationEntity> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        List<NotificationEntity> list = db.communicationDao().getAllNotifications();
                        postResult(callback, list);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                List<NotificationEntity> list = db.communicationDao().getAllNotifications();
                postResult(callback, list);
            });
        }
    }

    public void getUser(String userId, Callback<UserEntity> callback) {
        executor.execute(() -> {
            UserEntity user = db.userDao().getUserById(userId);
            postResult(callback, user);
        });
    }

    public void updateUserBalance(String userId, double available, double escrow, Callback<Boolean> callback) {
        if (walletRepo != null) {
            walletRepo.updateBalance(available, escrow, new WalletRepository.Callback<com.teligati.app.network.model.WalletNetworkDto>() {
                @Override
                public void onSuccess(com.teligati.app.network.model.WalletNetworkDto result) {
                    executor.execute(() -> {
                        db.userDao().updateBalance(userId, available, escrow);
                        postResult(callback, true);
                    });
                }

                @Override
                public void onError(String errorMessage) {
                    executor.execute(() -> {
                        db.userDao().updateBalance(userId, available, escrow);
                        postResult(callback, true);
                    });
                }
            });
        } else {
            executor.execute(() -> {
                db.userDao().updateBalance(userId, available, escrow);
                postResult(callback, true);
            });
        }
    }

    public WalletRepository getWalletRepository() {
        return walletRepo;
    }

    public void syncWallet(Callback<com.teligati.app.network.model.WalletNetworkDto> callback) {
        if (walletRepo != null) {
            walletRepo.syncWallet(new WalletRepository.Callback<com.teligati.app.network.model.WalletNetworkDto>() {
                @Override
                public void onSuccess(com.teligati.app.network.model.WalletNetworkDto result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            postResult(callback, null);
        }
    }

    public void simulateTopUp(double amount, String method, String note, Callback<com.teligati.app.network.model.WalletNetworkDto> callback) {
        if (walletRepo != null) {
            walletRepo.topUp(amount, method, note, new WalletRepository.Callback<com.teligati.app.network.model.WalletNetworkDto>() {
                @Override
                public void onSuccess(com.teligati.app.network.model.WalletNetworkDto result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            postResult(callback, null);
        }
    }

    public void simulateWithdraw(double amount, String destination, String note, Callback<com.teligati.app.network.model.WalletNetworkDto> callback) {
        if (walletRepo != null) {
            walletRepo.withdraw(amount, destination, note, new WalletRepository.Callback<com.teligati.app.network.model.WalletNetworkDto>() {
                @Override
                public void onSuccess(com.teligati.app.network.model.WalletNetworkDto result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            postResult(callback, null);
        }
    }

    public void syncTransactions(Callback<List<com.teligati.app.network.model.TransactionNetworkDto>> callback) {
        if (walletRepo != null) {
            walletRepo.syncTransactions(new WalletRepository.Callback<List<com.teligati.app.network.model.TransactionNetworkDto>>() {
                @Override
                public void onSuccess(List<com.teligati.app.network.model.TransactionNetworkDto> result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            postResult(callback, null);
        }
    }

    public void createEscrow(String title, String description, double amount, long payeeId, Long jobId, Callback<com.teligati.app.network.model.EscrowNetworkDto> callback) {
        if (walletRepo != null) {
            walletRepo.createEscrow(title, description, amount, payeeId, jobId, new WalletRepository.Callback<com.teligati.app.network.model.EscrowNetworkDto>() {
                @Override
                public void onSuccess(com.teligati.app.network.model.EscrowNetworkDto result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            postResult(callback, null);
        }
    }

    public void releaseEscrow(long escrowId, Callback<com.teligati.app.network.model.EscrowNetworkDto> callback) {
        if (walletRepo != null) {
            walletRepo.releaseEscrow(escrowId, new WalletRepository.Callback<com.teligati.app.network.model.EscrowNetworkDto>() {
                @Override
                public void onSuccess(com.teligati.app.network.model.EscrowNetworkDto result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            postResult(callback, null);
        }
    }

    public void refundEscrow(long escrowId, Callback<com.teligati.app.network.model.EscrowNetworkDto> callback) {
        if (walletRepo != null) {
            walletRepo.refundEscrow(escrowId, new WalletRepository.Callback<com.teligati.app.network.model.EscrowNetworkDto>() {
                @Override
                public void onSuccess(com.teligati.app.network.model.EscrowNetworkDto result) {
                    postResult(callback, result);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, null);
                }
            });
        } else {
            postResult(callback, null);
        }
    }

    public ReferenceRepository getReferenceRepository() {
        return referenceRepo;
    }

    public void submitRecommendation(long id, String text, String notes, Callback<Boolean> callback) {
        if (referenceRepo != null) {
            referenceRepo.completeRecommendation(id, text, notes, new ReferenceRepository.Callback<ReferenceNetworkDto>() {
                @Override
                public void onSuccess(ReferenceNetworkDto result) {
                    postResult(callback, true);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            postResult(callback, false);
        }
    }

    public void cancelReferenceRequest(long id, Callback<Boolean> callback) {
        if (referenceRepo != null) {
            referenceRepo.cancelRequest(id, new ReferenceRepository.Callback<ReferenceNetworkDto>() {
                @Override
                public void onSuccess(ReferenceNetworkDto result) {
                    postResult(callback, true);
                }

                @Override
                public void onError(String errorMessage) {
                    postResult(callback, false);
                }
            });
        } else {
            postResult(callback, false);
        }
    }
}
