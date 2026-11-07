package com.teligati.app.data.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.teligati.app.data.dao.*;
import com.teligati.app.data.entity.*;
import com.teligati.app.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@Database(
        entities = {
                UserEntity.class,
                StudentProfileEntity.class,
                AlumniProfileEntity.class,
                TeacherProfileEntity.class,
                JobPostingEntity.class,
                JobApplicationEntity.class,
                ReferenceRequestEntity.class,
                ResearchPaperEntity.class,
                ResearchProposalEntity.class,
                ResearchApplicationEntity.class,
                ConversationEntity.class,
                MessageEntity.class,
                NotificationEntity.class,
                TransactionEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "wizards_of_teligati.db";
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract ProfileDao profileDao();
    public abstract JobDao jobDao();
    public abstract ApplicationDao applicationDao();
    public abstract ReferenceRequestDao referenceRequestDao();
    public abstract ResearchDao researchDao();
    public abstract CommunicationDao communicationDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // Seed initial demonstration data once on database creation
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        seedInitialData(getInstance(context));
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void seedInitialData(AppDatabase db) {
        // 1. Seed Users
        List<UserEntity> users = new ArrayList<>();
        users.add(new UserEntity("2307003", "STUDENT", "Samun Sadab Wafi", "wafi2307003@stud.kuet.ac.bd",
                "CSE", "Batch 2K23", "Undergraduate Student", true, 18500.0, 3500.0));
        users.add(new UserEntity("tanvir.munich@kuet.ac.bd", "ALUMNI", "Engr. Tanvir Ahmed", "tanvir.munich@kuet.ac.bd",
                "CSE", "Batch 2K12", "Staff Engineer @ BMW Group", true, 0.0, 0.0));
        users.add(new UserEntity("azharul@cse.kuet.ac.bd", "TEACHER", "Prof. Dr. K. M. Azharul Hasan", "azharul@cse.kuet.ac.bd",
                "CSE", "Faculty", "Professor & Head of Research", true, 0.0, 0.0));
        db.userDao().insertUsers(users);

        // 2. Seed Student Profile
        db.profileDao().insertStudentProfile(new StudentProfileEntity(
                "2307003",
                "Samun Sadab Wafi",
                "Computer Science and Engineering",
                "Batch 2K23",
                3.84,
                "Java, Kotlin, Python, Spring Boot, PyTorch, Docker, Git, ROS",
                "Autonomous Campus Rover (ROS), Wizards of Teligati (Android/Spring)",
                "https://github.com/kuet-wizards",
                "https://linkedin.com/in/kuet-student",
                "Distributed Systems & Edge AI",
                "SGIPC Executive, KUET Career Club",
                "KUET CSE undergraduate dedicated to building robust distributed software."
        ));

        // 3. Seed Alumni Profiles
        List<AlumniProfileEntity> alumniList = new ArrayList<>();
        for (AlumniProfile a : SampleData.getAlumniList()) {
            StringBuilder sb = new StringBuilder();
            if (a.getSkills() != null) {
                for (int i = 0; i < a.getSkills().size(); i++) {
                    sb.append(a.getSkills().get(i));
                    if (i < a.getSkills().size() - 1) sb.append(", ");
                }
            }
            alumniList.add(new AlumniProfileEntity(
                    a.getId(),
                    a.getId() == 1 ? "tanvir.munich@kuet.ac.bd" : "alumni_" + a.getId(),
                    a.getName(),
                    a.getDepartment(),
                    a.getBatch(),
                    a.getCurrentCompany(),
                    a.getCurrentPosition(),
                    a.getCountry(),
                    a.getCity(),
                    a.isVerified(),
                    a.isAvailableForMentorship(),
                    a.isActivelyHiring(),
                    sb.toString(),
                    a.getBio()
            ));
        }
        db.profileDao().insertAlumniProfiles(alumniList);

        // 4. Seed Teacher Profile
        db.profileDao().insertTeacherProfile(new TeacherProfileEntity(
                1,
                "azharul@cse.kuet.ac.bd",
                "Prof. Dr. K. M. Azharul Hasan",
                "Professor & Head of Research",
                "Computer Science and Engineering",
                "Cloud Computing, Big Data, Distributed Databases, Edge AI",
                "Intelligent Systems & Big Data Lab",
                100,
                "azharul@cse.kuet.ac.bd",
                "Supervising thesis, PhD, and undergraduate research in intelligent systems."
        ));

        // 5. Seed Job Postings (Assistant & Professional Jobs)
        List<JobPostingEntity> jobs = new ArrayList<>();
        for (Opportunity o : SampleData.getOpportunities()) {
            StringBuilder sb = new StringBuilder();
            if (o.getSkillsRequired() != null) {
                for (int i = 0; i < o.getSkillsRequired().size(); i++) {
                    sb.append(o.getSkillsRequired().get(i));
                    if (i < o.getSkillsRequired().size() - 1) sb.append(", ");
                }
            }
            jobs.add(new JobPostingEntity(
                    o.getId(),
                    o.getTitle(),
                    o.getCategory().name(),
                    o.getOrganization(),
                    o.getLocation(),
                    o.getPostedByName(),
                    o.getPosterRole(),
                    o.getCompensation(),
                    o.getDuration(),
                    o.getDescription(),
                    sb.toString(),
                    o.getMinCgpa(),
                    o.isApplied(),
                    "tanvir.munich@kuet.ac.bd",
                    System.currentTimeMillis() - (o.getId() * 3600000L)
            ));
        }
        db.jobDao().insertJobs(jobs);

        // 6. Seed Research Papers
        List<ResearchPaperEntity> papers = new ArrayList<>();
        for (ProjectItem p : SampleData.getProjects()) {
            StringBuilder sb = new StringBuilder();
            if (p.getTechnologies() != null) {
                for (int i = 0; i < p.getTechnologies().size(); i++) {
                    sb.append(p.getTechnologies().get(i));
                    if (i < p.getTechnologies().size() - 1) sb.append(", ");
                }
            }
            papers.add(new ResearchPaperEntity(
                    p.getId(),
                    p.getName(),
                    p.getType(),
                    p.getDescription(),
                    sb.toString(),
                    p.getOwnerName(),
                    p.getOwnerDept(),
                    "azharul@cse.kuet.ac.bd",
                    p.getContributorsCount(),
                    p.isOpenForContribution(),
                    p.getGithubUrl(),
                    p.getPaperDoi(),
                    System.currentTimeMillis() - (p.getId() * 7200000L)
            ));
        }
        db.researchDao().insertPapers(papers);

        // 7. Seed Reference Requests
        List<ReferenceRequestEntity> reqs = new ArrayList<>();
        for (ReferenceRequest r : SampleData.getReferenceRequests()) {
            reqs.add(new ReferenceRequestEntity(
                    r.getId(),
                    r.getStudentRoll(),
                    r.getStudentName(),
                    r.getStudentDept(),
                    r.getTargetCompany(),
                    r.getMessage(),
                    r.getStatus(),
                    "tanvir.munich@kuet.ac.bd",
                    System.currentTimeMillis() - (r.getId() * 86400000L)
            ));
        }
        db.referenceRequestDao().insertRequests(reqs);

        // 8. Seed Research Proposals
        List<ResearchProposalEntity> proposals = new ArrayList<>();
        for (ResearchProposal rp : SampleData.getResearchProposals()) {
            proposals.add(new ResearchProposalEntity(
                    rp.getId(),
                    rp.getStudentRoll(),
                    rp.getStudentName(),
                    rp.getTopic(),
                    rp.getProposalAbstract(),
                    rp.getDegreeType(),
                    rp.getStatus(),
                    "azharul@cse.kuet.ac.bd",
                    "Under review by Lab committee",
                    System.currentTimeMillis() - (rp.getId() * 43200000L)
            ));
        }
        db.researchDao().insertProposals(proposals);

        // 9. Seed Student Applicants / Research Collaborations
        List<ResearchApplicationEntity> apps = new ArrayList<>();
        for (StudentApplicant sa : SampleData.getStudentApplicants()) {
            apps.add(new ResearchApplicationEntity(
                    sa.getId(),
                    2L, // Autonomous Vehicles lab opportunity
                    "Research Assistant — Computer Vision for Autonomous Vehicles",
                    sa.getRoll(),
                    sa.getName(),
                    sa.getDepartment(),
                    sa.getCgpa(),
                    sa.getSkills(),
                    sa.getLabInterest(),
                    sa.getStatus(),
                    System.currentTimeMillis() - (sa.getId() * 18000000L)
            ));
        }
        db.researchDao().insertResearchApplications(apps);

        // 10. Seed Transactions
        List<TransactionEntity> txs = new ArrayList<>();
        for (TransactionRecord t : SampleData.getTransactions()) {
            txs.add(new TransactionEntity(
                    t.getId(),
                    t.getTitle(),
                    t.getCategory(),
                    t.getAmount(),
                    t.getDate(),
                    t.getStatus(),
                    "2307003"
            ));
        }
        db.communicationDao().insertTransactions(txs);

        // 11. Seed Messages
        List<MessageEntity> msgs = new ArrayList<>();
        msgs.add(new MessageEntity(1, "tanvir.munich@kuet.ac.bd", "Engr. Tanvir Ahmed (BMW Munich)",
                "2307003", "Samun Sadab Wafi",
                "Checked your GitHub repository for ROS rover. Ready for the phone screen on Saturday?",
                System.currentTimeMillis() - 7200000L, "Today • 11:42 AM"));
        msgs.add(new MessageEntity(2, "azharul@cse.kuet.ac.bd", "Prof. Dr. K. M. Azharul Hasan (Big Data Lab)",
                "2307003", "Samun Sadab Wafi",
                "Your proposal on Edge AI Vision Transformer has been accepted for lab co-authorship.",
                System.currentTimeMillis() - 86400000L, "Yesterday • 4:15 PM"));
        db.communicationDao().insertMessages(msgs);

        // 12. Seed Notifications
        List<NotificationEntity> notifs = new ArrayList<>();
        notifs.add(new NotificationEntity(1, "2307003", "🎉 Reference Request Accepted!",
                "Engr. Tanvir Ahmed (BMW Munich) endorsed your application for the Software Engineer role.",
                System.currentTimeMillis() - 600000L, "10 minutes ago", false));
        notifs.add(new NotificationEntity(2, "2307003", "💰 Escrow Payment Released",
                "৳15,000 for Python Script Automation has been released to your available balance.",
                System.currentTimeMillis() - 7200000L, "2 hours ago", false));
        db.communicationDao().insertNotifications(notifs);
    }
}
