const testing = require('@firebase/rules-unit-testing');
const fs = require('fs');
const path = require('path');

async function runTestSuite() {
    console.log('========================================================================');
    console.log('  WIZARDS OF TELIGATI — FIRESTORE EMULATOR COMPREHENSIVE SECURITY TEST  ');
    console.log('========================================================================');

    const rulesPath = path.resolve(__dirname, '..', 'firestore.rules');
    const rules = fs.readFileSync(rulesPath, 'utf8');

    // Default to port 8081 to avoid conflict with Spring Boot backend (port 8080)
    const hostPort = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8081';
    const [host, portStr] = hostPort.split(':');
    const port = parseInt(portStr);

    console.log(`[Config] Target Firestore Emulator: ${host}:${port}`);

    const PROJECT_ID = 'wizards-of-teligati';

    const testEnv = await testing.initializeTestEnvironment({
        projectId: PROJECT_ID,
        firestore: {
            host: host,
            port: port,
            rules: rules
        }
    });

    console.log('[Setup] Clearing previous emulator data for pristine test isolation...');
    await testEnv.clearFirestore();

    console.log('[Setup] Seeding minimum required test fixtures with rules disabled...');
    await testEnv.withSecurityRulesDisabled(async (context) => {
        const adminDb = context.firestore();

        // 1. Seed Users (Private accounts)
        await adminDb.doc('users/student_1').set({
            uid: 'student_1',
            email: 'student1@stud.kuet.ac.bd',
            role: 'STUDENT',
            name: 'Samun Sadab Wafi',
            department: 'CSE',
            roll: '2307003',
            createdAt: 1000
        });
        await adminDb.doc('users/student_2').set({
            uid: 'student_2',
            email: 'student2@stud.kuet.ac.bd',
            role: 'STUDENT',
            name: 'Peer Student',
            department: 'CSE',
            roll: '2307004',
            cgpa: '3.90',
            createdAt: 1000
        });
        await adminDb.doc('users/faculty_1').set({
            uid: 'faculty_1',
            email: 'azharul@cse.kuet.ac.bd',
            role: 'TEACHER',
            name: 'Prof. Dr. K. M. Azharul Hasan',
            department: 'CSE',
            createdAt: 1000
        });
        await adminDb.doc('users/faculty_2').set({
            uid: 'faculty_2',
            email: 'unassigned@cse.kuet.ac.bd',
            role: 'TEACHER',
            name: 'Prof. Unassigned Faculty',
            department: 'CSE',
            createdAt: 1000
        });
        await adminDb.doc('users/alumni_1').set({
            uid: 'alumni_1',
            email: 'tanvir@kuet.ac.bd',
            role: 'ALUMNI',
            name: 'Engr. Tanvir Ahmed',
            department: 'CSE',
            createdAt: 1000
        });
        await adminDb.doc('users/alumni_2').set({
            uid: 'alumni_2',
            email: 'otheralumni@kuet.ac.bd',
            role: 'ALUMNI',
            name: 'Engr. Other Alumni',
            department: 'CSE',
            createdAt: 1000
        });

        // 2. Seed Public Profiles
        await adminDb.doc('public_profiles/student_1').set({
            uid: 'student_1',
            role: 'STUDENT',
            name: 'Samun Sadab Wafi',
            department: 'CSE'
        });
        await adminDb.doc('public_profiles/student_2').set({
            uid: 'student_2',
            role: 'STUDENT',
            name: 'Peer Student',
            department: 'CSE'
        });
        await adminDb.doc('public_profiles/faculty_1').set({
            uid: 'faculty_1',
            role: 'TEACHER',
            name: 'Prof. Dr. K. M. Azharul Hasan',
            department: 'CSE'
        });
        await adminDb.doc('public_profiles/faculty_2').set({
            uid: 'faculty_2',
            role: 'TEACHER',
            name: 'Prof. Unassigned Faculty',
            department: 'CSE'
        });
        await adminDb.doc('public_profiles/alumni_1').set({
            uid: 'alumni_1',
            role: 'ALUMNI',
            name: 'Engr. Tanvir Ahmed',
            department: 'CSE'
        });
        await adminDb.doc('public_profiles/alumni_2').set({
            uid: 'alumni_2',
            role: 'ALUMNI',
            name: 'Engr. Other Alumni',
            department: 'CSE'
        });

        // 3. Seed Research Proposal assigned to faculty_1 and student_1
        await adminDb.doc('research_proposals/prop_assigned').set({
            studentUid: 'student_1',
            facultyUid: 'faculty_1',
            topic: 'Autonomous Edge Computing for KUET IoT',
            proposalAbstract: 'Optimized decentralized broker architecture',
            degreeType: 'BSC',
            status: 'PENDING',
            submittedAt: 1000
        });

        // 4. Seed Job and Job Application (faculty_1 poster, student_1 applicant)
        await adminDb.doc('jobs/job_1').set({
            postedByUid: 'faculty_1',
            postedByRole: 'TEACHER',
            title: 'Graduate Research Assistant',
            company: 'KUET AI Lab',
            category: 'RESEARCH_ASSISTANT',
            status: 'OPEN',
            createdAt: 1000
        });
        await adminDb.doc('job_applications/app_1').set({
            applicantUid: 'student_1',
            posterUid: 'faculty_1',
            jobId: 'job_1',
            jobTitle: 'Graduate Research Assistant',
            status: 'PENDING',
            appliedAt: 1000
        });

        // 5. Seed Reference Request (student_1 requester, alumni_1 referee)
        await adminDb.doc('reference_requests/ref_1').set({
            studentUid: 'student_1',
            alumniUid: 'alumni_1',
            purpose: 'Graduate School Recommendation at TU Munich',
            status: 'PENDING',
            createdAt: 1000
        });

        // 6. Seed Conversation between student_1 and student_2
        await adminDb.doc('conversations/conv_1').set({
            participantUids: ['student_1', 'student_2'],
            lastMessage: 'Let us coordinate on the project',
            lastMessageTime: 1000,
            lastSenderUid: 'student_1'
        });
        await adminDb.doc('conversations/conv_1/messages/msg_1').set({
            senderUid: 'student_1',
            recipientUid: 'student_2',
            content: 'Hello peer!',
            timestamp: 1000
        });

        // 7. Seed Demo Wallets
        await adminDb.doc('users/student_1/demo_wallet/main').set({
            availableBalance: 10000,
            escrowBalance: 0,
            currency: 'DEMO_CREDITS',
            disclaimer: 'DEMO WALLET',
            demoNotice: 'DEMO MODE'
        });
        await adminDb.doc('users/student_2/demo_wallet/main').set({
            availableBalance: 5000,
            escrowBalance: 0,
            currency: 'DEMO_CREDITS',
            disclaimer: 'DEMO WALLET',
            demoNotice: 'DEMO MODE'
        });

        // 8. Seed Demo Transaction
        await adminDb.doc('demo_transactions/tx_1').set({
            userId: 'student_1',
            amount: 500,
            type: 'TOPUP',
            status: 'COMPLETED',
            timestamp: 1000
        });

        // 9. Seed Demo Escrow (faculty_1 employer, student_1 freelancer)
        await adminDb.doc('demo_escrows/escrow_1').set({
            employerUid: 'faculty_1',
            freelancerUid: 'student_1',
            amount: 1500,
            currency: 'BDT',
            jobId: 101,
            status: 'HELD',
            createdAt: 1000
        });
    });
    console.log('[Setup] Test fixtures seeded successfully.\n');

    // Contexts for actors
    const student1Db = testEnv.authenticatedContext('student_1', { email: 'student1@stud.kuet.ac.bd' }).firestore();
    const student2Db = testEnv.authenticatedContext('student_2', { email: 'student2@stud.kuet.ac.bd' }).firestore();
    const faculty1Db = testEnv.authenticatedContext('faculty_1', { email: 'azharul@cse.kuet.ac.bd' }).firestore();
    const faculty2Db = testEnv.authenticatedContext('faculty_2', { email: 'unassigned@cse.kuet.ac.bd' }).firestore();
    const alumni1Db = testEnv.authenticatedContext('alumni_1', { email: 'tanvir@kuet.ac.bd' }).firestore();
    const alumni2Db = testEnv.authenticatedContext('alumni_2', { email: 'otheralumni@kuet.ac.bd' }).firestore();

    let passedCount = 0;
    let failedCount = 0;

    async function runTest(testName, testFn) {
        try {
            await testFn();
            console.log(`[PASS] ${testName}`);
            passedCount++;
        } catch (error) {
            console.error(`[FAIL] ${testName}`);
            console.error(`       Error: ${error.message || error}`);
            failedCount++;
        }
    }

    // -------------------------------------------------------------------------
    // BATCH 1: CORE SECURITY INVARIANTS (1 - 14)
    // -------------------------------------------------------------------------

    // 1. Student reading another student's private profile: DENIED
    await runTest("1. Student reading another student's private profile -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('users/student_2').get());
    });

    // 2. Student modifying their own role to TEACHER: DENIED
    await runTest("2. Student modifying their own role to TEACHER -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('users/student_1').update({ role: 'TEACHER' }));
    });

    // 3. Student creating a notification for an unrelated user: DENIED
    await runTest("3. Student creating a notification for an unrelated user -> DENIED", async () => {
        await testing.assertFails(student1Db.collection('users/student_2/notifications').add({
            recipientUid: 'student_2',
            senderUid: 'student_1',
            title: 'Unsolicited Spam Alert',
            message: 'You have been selected for a prize!',
            read: false,
            type: 'GENERAL'
        }));
    });

    // 4. Legitimate self-confirmation notification: ALLOWED
    await runTest("4. Legitimate self-confirmation notification -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.collection('users/student_1/notifications').add({
            recipientUid: 'student_1',
            senderUid: 'student_1',
            title: 'Application Submitted',
            message: 'You applied for Lab Assistant. Awaiting faculty review.',
            read: false
        }));
    });

    // 5. Faculty notifying a student about their assigned proposal: ALLOWED
    await runTest("5. Faculty notifying student about assigned proposal -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.collection('users/student_1/notifications').add({
            recipientUid: 'student_1',
            senderUid: 'faculty_1',
            title: 'Proposal Decision',
            message: 'Your research proposal was reviewed and accepted.',
            read: false,
            type: 'PROPOSAL_DECISION',
            referenceId: 'prop_assigned'
        }));
    });

    // 6. Unassigned faculty notifying that student about the proposal: DENIED
    await runTest("6. Unassigned faculty notifying that student about proposal -> DENIED", async () => {
        await testing.assertFails(faculty2Db.collection('users/student_1/notifications').add({
            recipientUid: 'student_1',
            senderUid: 'faculty_2',
            title: 'Proposal Decision',
            message: 'Fraudulent decision attempt.',
            read: false,
            type: 'PROPOSAL_DECISION',
            referenceId: 'prop_assigned'
        }));
    });

    // 7. Creating a conversation with oneself: DENIED
    await runTest("7. Creating a conversation with oneself -> DENIED", async () => {
        await testing.assertFails(student1Db.collection('conversations').add({
            participantUids: ['student_1', 'student_1'],
            lastMessage: 'Self monologue',
            lastMessageTime: Date.now(),
            lastSenderUid: 'student_1',
            createdAt: Date.now()
        }));
    });

    // 8. Creating a conversation with a nonexistent user: DENIED
    await runTest("8. Creating a conversation with a nonexistent user -> DENIED", async () => {
        await testing.assertFails(student1Db.collection('conversations').add({
            participantUids: ['student_1', 'ghost_user_9999'],
            lastMessage: 'Are you there?',
            lastMessageTime: Date.now(),
            lastSenderUid: 'student_1',
            createdAt: Date.now()
        }));
    });

    // 9. A conversation participant reading messages: ALLOWED
    await runTest("9. Conversation participant reading messages -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.collection('conversations/conv_1/messages').get());
    });

    // 10. A non-participant reading messages: DENIED
    await runTest("10. Non-participant reading messages -> DENIED", async () => {
        await testing.assertFails(faculty1Db.collection('conversations/conv_1/messages').get());
    });

    // 11. A user modifying another user's demo wallet: DENIED
    await runTest("11. User modifying another user's demo wallet -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('users/student_2/demo_wallet/main').update({
            availableBalance: 0
        }));
    });

    // 12. Negative wallet balance: DENIED
    await runTest("12. Negative wallet balance -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('users/student_1/demo_wallet/main').update({
            availableBalance: -500
        }));
    });

    // 13. A user modifying an immutable transaction: DENIED
    await runTest("13. User modifying an immutable transaction -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('demo_transactions/tx_1').update({
            amount: 999999
        }));
    });

    // 14. Unauthorized escrow release (by freelancer): DENIED
    await runTest("14. Unauthorized escrow release (by freelancer) -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('demo_escrows/escrow_1').update({
            status: 'RELEASED'
        }));
    });

    // -------------------------------------------------------------------------
    // BATCH 2: REMAINING TARGETED SCENARIOS (15 - 21)
    // -------------------------------------------------------------------------

    // 15. A legitimate job poster notifying the correct applicant about an application decision: ALLOWED
    await runTest("15. Legitimate job poster notifying applicant about decision -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.collection('users/student_1/notifications').add({
            recipientUid: 'student_1',
            senderUid: 'faculty_1',
            title: 'Job Application Accepted',
            message: 'You have been selected for Graduate Research Assistant position.',
            read: false,
            type: 'APPLICATION_DECISION',
            referenceId: 'app_1'
        }));
    });

    // 16. An unrelated user attempting the same job application notification: DENIED
    await runTest("16. Unrelated user attempting application decision notification -> DENIED", async () => {
        await testing.assertFails(faculty2Db.collection('users/student_1/notifications').add({
            recipientUid: 'student_1',
            senderUid: 'faculty_2',
            title: 'Job Application Accepted',
            message: 'Impersonated job application acceptance note.',
            read: false,
            type: 'APPLICATION_DECISION',
            referenceId: 'app_1'
        }));
    });

    // 17. The assigned alumni referee notifying the correct student about a reference decision: ALLOWED
    await runTest("17. Assigned alumni referee notifying student about reference decision -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.collection('users/student_1/notifications').add({
            recipientUid: 'student_1',
            senderUid: 'alumni_1',
            title: 'Reference Completed',
            message: 'I have submitted your recommendation letter to TU Munich.',
            read: false,
            type: 'REFERENCE_DECISION',
            referenceId: 'ref_1'
        }));
    });

    // 18. An unrelated alumni attempting that reference notification: DENIED
    await runTest("18. Unrelated alumni attempting reference decision notification -> DENIED", async () => {
        await testing.assertFails(alumni2Db.collection('users/student_1/notifications').add({
            recipientUid: 'student_1',
            senderUid: 'alumni_2',
            title: 'Reference Completed',
            message: 'Fraudulent reference notification from unassigned alumni.',
            read: false,
            type: 'REFERENCE_DECISION',
            referenceId: 'ref_1'
        }));
    });

    // 19. A conversation participant notifying the other participant: ALLOWED
    await runTest("19. Conversation participant notifying the other participant -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.collection('users/student_2/notifications').add({
            recipientUid: 'student_2',
            senderUid: 'student_1',
            title: 'New Message from Samun Sadab Wafi',
            message: 'Let us coordinate on the project code tonight.',
            read: false,
            type: 'MESSAGE',
            referenceId: 'conv_1'
        }));
    });

    // 20. A user attempting to send a message notification to someone outside that conversation: DENIED
    await runTest("20. User sending message notification to user outside conversation -> DENIED", async () => {
        await testing.assertFails(student1Db.collection('users/faculty_1/notifications').add({
            recipientUid: 'faculty_1',
            senderUid: 'student_1',
            title: 'New Message from Samun Sadab Wafi',
            message: 'Spamming faculty with fake message alert.',
            read: false,
            type: 'MESSAGE',
            referenceId: 'conv_1'
        }));
    });

    // 21. A user directly setting their own demo wallet balance to an arbitrary positive amount below ceiling:
    // Determine whether current rules permit this and report the exact truth.
    await runTest("21. User directly setting own demo wallet balance to positive amount (e.g. 75,000) -> ALLOWED (Simulation limitation)", async () => {
        await testing.assertSucceeds(student1Db.doc('users/student_1/demo_wallet/main').update({
            availableBalance: 75000
        }));
    });

    // -------------------------------------------------------------------------
    // BATCH 3: PHASE 3 — RESEARCH PAPERS & PROPOSALS SECURITY SUITE (22 - 36)
    // -------------------------------------------------------------------------

    // 22. Teacher publishing a research paper: ALLOWED
    await runTest("22. Teacher publishing a research paper -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.doc('research_papers/paper_faculty1').set({
            ownerUid: 'faculty_1',
            title: 'Distributed Consensus in Edge AI',
            description: 'Edge clustering algorithms for low latency networks',
            technologies: 'Edge AI, PyTorch',
            ownerName: 'Prof. Dr. K. M. Azharul Hasan',
            ownerDept: 'Dept of CSE',
            paperDoi: 'doi.org/10.1145/kuet.edge.2026',
            githubUrl: 'https://github.com/kuet-wizards',
            type: 'Research Paper',
            isOpenForContribution: true,
            contributorsCount: 1,
            createdAt: Date.now()
        }));
    });

    // 23. Non-owner attempting to modify that paper: DENIED
    await runTest("23. Non-owner (Student) attempting to modify teacher paper -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('research_papers/paper_faculty1').update({
            title: 'Hacked Title by Student'
        }));
    });

    // 24. Non-owner attempting to delete that paper: DENIED
    await runTest("24. Non-owner (Faculty 2) attempting to delete teacher paper -> DENIED", async () => {
        await testing.assertFails(faculty2Db.doc('research_papers/paper_faculty1').delete());
    });

    // 25. Owner modifying their own paper: ALLOWED
    await runTest("25. Paper owner modifying their own paper -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.doc('research_papers/paper_faculty1').update({
            description: 'Updated abstract with new experimental results',
            updatedAt: Date.now()
        }));
    });

    // 26. Student submitting proposal with initial PENDING status: ALLOWED
    await runTest("26. Student submitting research proposal (status: PENDING) -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('research_proposals/prop_student1').set({
            studentUid: 'student_1',
            facultyUid: 'faculty_1',
            studentRoll: '2307003',
            studentName: 'Samun Sadab Wafi',
            topic: 'Autonomous Edge AI Architecture',
            proposalAbstract: 'Investigation of lightweight deep neural nets on edge gateways',
            degreeType: 'Undergraduate Thesis',
            status: 'PENDING',
            submittedAt: 2000,
            updatedAt: 2000
        }));
    });

    // 27. Student submitting proposal to themselves: DENIED
    await runTest("27. Student submitting proposal to themselves (studentUid == facultyUid) -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('research_proposals/prop_self').set({
            studentUid: 'student_1',
            facultyUid: 'student_1',
            studentRoll: '2307003',
            studentName: 'Samun Sadab Wafi',
            topic: 'Self Proposal',
            proposalAbstract: 'Self proposed',
            degreeType: 'Undergraduate Thesis',
            status: 'PENDING',
            submittedAt: 2000
        }));
    });

    // 28. Student submitting proposal with initial status other than PENDING: DENIED
    await runTest("28. Student submitting proposal with initial status ACCEPTED -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('research_proposals/prop_fake_accepted').set({
            studentUid: 'student_1',
            facultyUid: 'faculty_1',
            studentRoll: '2307003',
            studentName: 'Samun Sadab Wafi',
            topic: 'Fake Accepted Proposal',
            proposalAbstract: 'Bypassing review',
            degreeType: 'Undergraduate Thesis',
            status: 'ACCEPTED',
            submittedAt: 2000
        }));
    });

    // 29. Unauthorized student decision (Student attempting to approve/reject own proposal): DENIED
    await runTest("29. Unauthorized student decision (Student approving own proposal) -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('research_proposals/prop_student1').update({
            status: 'ACCEPTED'
        }));
    });

    // 30. Student attempting to alter faculty feedback: DENIED
    await runTest("30. Student attempting to alter faculty feedback -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('research_proposals/prop_assigned').update({
            feedback: 'Fabricated high-praise feedback from advisor'
        }));
    });

    // 31. Unauthorized faculty decision (Unassigned faculty attempting to decide proposal): DENIED
    await runTest("31. Unauthorized faculty decision (Unassigned faculty 2 deciding prop_assigned) -> DENIED", async () => {
        await testing.assertFails(faculty2Db.doc('research_proposals/prop_assigned').update({
            status: 'REJECTED',
            feedback: 'Unauthorized rejection note',
            updatedAt: Date.now()
        }));
    });

    // 32. Assigned faculty making proposal decision (status & feedback update): ALLOWED
    await runTest("32. Assigned faculty (faculty 1) accepting proposal with feedback -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.doc('research_proposals/prop_assigned').update({
            status: 'ACCEPTED',
            feedback: 'Approved for Intelligent Systems Lab supervision. Well-formulated scope.',
            updatedAt: Date.now()
        }));
    });

    // 33. Assigned faculty attempting to modify immutable student proposal topic/abstract: DENIED
    await runTest("33. Assigned faculty altering immutable proposal topic during decision -> DENIED", async () => {
        await testing.assertFails(faculty1Db.doc('research_proposals/prop_student1').update({
            topic: 'Altered Topic by Faculty',
            status: 'ACCEPTED',
            feedback: 'Topic changed',
            updatedAt: Date.now()
        }));
    });

    // 34. Student proposal ownership access: Student reading own proposals -> ALLOWED
    await runTest("34. Student querying own proposals (studentUid == auth.uid) -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.collection('research_proposals')
            .where('studentUid', '==', 'student_1')
            .get());
    });

    // 35. Unrelated student attempting to read another student's proposal document directly: DENIED
    await runTest("35. Unrelated student (student 2) reading student 1 proposal doc -> DENIED", async () => {
        await testing.assertFails(student2Db.doc('research_proposals/prop_assigned').get());
    });

    // 36. Any user attempting to delete a research proposal: DENIED
    await runTest("36. Deleting a research proposal -> DENIED", async () => {
        await testing.assertFails(faculty1Db.doc('research_proposals/prop_assigned').delete());
    });

    // =========================================================================
    // BATCH 4: JOBS, OPPORTUNITIES & JOB APPLICATIONS (PHASE 4)
    // =========================================================================
    console.log('\n--- BATCH 4: JOBS & JOB APPLICATIONS SECURITY RULES ---');

    // 37. Alumni creating job: ALLOWED
    await runTest("37. Alumni (alumni_1) creating assistant job posting -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.doc('jobs/job_alumni_1').set({
            postedByUid: 'alumni_1',
            postedByRole: 'ALUMNI',
            title: 'Senior Frontend React Developer',
            organization: 'KUET Tech Ventures',
            category: 'FULLTIME_JOBS',
            compensation: '৳80,000 / month',
            createdAt: Date.now()
        }));
    });

    // 38. Teacher creating job/opportunity: ALLOWED
    await runTest("38. Teacher (faculty_1) creating research assistant opportunity -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.doc('jobs/job_faculty_1').set({
            postedByUid: 'faculty_1',
            postedByRole: 'TEACHER',
            title: 'Graduate ML Research Assistant',
            organization: 'KUET Intelligent Systems Lab',
            category: 'RESEARCH_COLLABORATION',
            compensation: '৳20,000 / month',
            createdAt: Date.now()
        }));
    });

    // 39. Student attempting to create job posting: DENIED
    await runTest("39. Student (student_1) attempting to create job posting -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('jobs/job_student_illegal').set({
            postedByUid: 'student_1',
            postedByRole: 'STUDENT',
            title: 'Unauthorized Student Job',
            category: 'ASSISTANT_WORK',
            createdAt: Date.now()
        }));
    });

    // 40. Non-owner attempting to update job posting: DENIED
    await runTest("40. Non-owner (alumni_2) attempting to update job_alumni_1 -> DENIED", async () => {
        await testing.assertFails(alumni2Db.doc('jobs/job_alumni_1').update({
            title: 'Hijacked Job Title'
        }));
    });

    // 41. Job owner updating their job details: ALLOWED
    await runTest("41. Job owner (alumni_1) updating job details -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.doc('jobs/job_alumni_1').update({
            title: 'Lead Full-Stack React & Node Engineer',
            compensation: '৳95,000 / month',
            updatedAt: Date.now()
        }));
    });

    // 42. Job owner attempting to reassign postedByUid (immutable poster): DENIED
    await runTest("42. Job owner attempting to alter immutable postedByUid -> DENIED", async () => {
        await testing.assertFails(alumni1Db.doc('jobs/job_alumni_1').update({
            postedByUid: 'alumni_2'
        }));
    });

    // 43. Non-owner attempting to delete job posting: DENIED
    await runTest("43. Non-owner (alumni_2) attempting to delete job_alumni_1 -> DENIED", async () => {
        await testing.assertFails(alumni2Db.doc('jobs/job_alumni_1').delete());
    });

    // 44. Authenticated user reading jobs: ALLOWED
    await runTest("44. Authenticated student (student_1) reading jobs list -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.collection('jobs').get());
    });

    // 45. Unauthenticated user reading jobs: DENIED
    await runTest("45. Unauthenticated user reading jobs collection -> DENIED", async () => {
        const unauthDb = testEnv.unauthenticatedContext().firestore();
        await testing.assertFails(unauthDb.collection('jobs').get());
    });

    // 46. Student applying for job (status APPLIED, student != poster): ALLOWED
    await runTest("46. Student (student_1) submitting job application (status APPLIED) -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('job_applications/app_s1_j1').set({
            applicantUid: 'student_1',
            posterUid: 'alumni_1',
            jobId: 'job_alumni_1',
            jobTitle: 'Lead Full-Stack React & Node Engineer',
            status: 'APPLIED',
            statement: 'Excited to contribute to alumni venture.',
            appliedAt: Date.now()
        }));
    });

    // 47. Student applying for job (status PENDING): ALLOWED
    await runTest("47. Student (student_2) submitting job application (status PENDING) -> ALLOWED", async () => {
        await testing.assertSucceeds(student2Db.doc('job_applications/app_s2_j1').set({
            applicantUid: 'student_2',
            posterUid: 'alumni_1',
            jobId: 'job_alumni_1',
            jobTitle: 'Lead Full-Stack React & Node Engineer',
            status: 'PENDING',
            statement: 'Solid background in distributed backends.',
            appliedAt: Date.now()
        }));
    });

    // 48. Poster applying to their own job: DENIED
    await runTest("48. Poster applying to their own job (applicantUid == posterUid) -> DENIED", async () => {
        await testing.assertFails(alumni1Db.doc('job_applications/app_self').set({
            applicantUid: 'alumni_1',
            posterUid: 'alumni_1',
            jobId: 'job_alumni_1',
            status: 'APPLIED',
            appliedAt: Date.now()
        }));
    });

    // 49. Student applying with invalid initial status (ACCEPTED): DENIED
    await runTest("49. Student applying with pre-accepted status (status: ACCEPTED) -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('job_applications/app_fake_accept').set({
            applicantUid: 'student_1',
            posterUid: 'alumni_1',
            jobId: 'job_alumni_1',
            status: 'ACCEPTED',
            appliedAt: Date.now()
        }));
    });

    // 50. Applicant reading their own applications via query: ALLOWED
    await runTest("50. Applicant querying own applications (applicantUid == auth.uid) -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.collection('job_applications')
            .where('applicantUid', '==', 'student_1')
            .get());
    });

    // 51. Job poster querying received applications: ALLOWED
    await runTest("51. Job poster querying received applications (posterUid == auth.uid) -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.collection('job_applications')
            .where('posterUid', '==', 'alumni_1')
            .get());
    });

    // 52. Unrelated user directly reading another's application: DENIED
    await runTest("52. Unrelated user (student_2) directly reading app_s1_j1 -> DENIED", async () => {
        await testing.assertFails(student2Db.doc('job_applications/app_s1_j1').get());
    });

    // 53. Unauthorized user attempting decision on application: DENIED
    await runTest("53. Unauthorized user (alumni_2) deciding app_s1_j1 -> DENIED", async () => {
        await testing.assertFails(alumni2Db.doc('job_applications/app_s1_j1').update({
            status: 'ACCEPTED',
            posterNotes: 'Unauthorized approval',
            updatedAt: Date.now()
        }));
    });

    // 54. Poster updating application decision (status ACCEPTED & posterNotes): ALLOWED
    await runTest("54. Poster (alumni_1) accepting application app_s1_j1 -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.doc('job_applications/app_s1_j1').update({
            status: 'ACCEPTED',
            posterNotes: 'Selected for interview and preliminary offer.',
            updatedAt: Date.now()
        }));
    });

    // 55. Poster attempting to alter student statement during review: DENIED
    await runTest("55. Poster attempting to alter immutable student statement -> DENIED", async () => {
        await testing.assertFails(alumni1Db.doc('job_applications/app_s2_j1').update({
            statement: 'Tampered student statement',
            status: 'UNDER_REVIEW',
            updatedAt: Date.now()
        }));
    });

    // 56. Applicant withdrawing pending application (app_s2_j1): ALLOWED
    await runTest("56. Applicant (student_2) withdrawing pending application -> ALLOWED", async () => {
        await testing.assertSucceeds(student2Db.doc('job_applications/app_s2_j1').update({
            status: 'WITHDRAWN',
            updatedAt: Date.now()
        }));
    });

    // 57. Applicant attempting to approve own application via update: DENIED
    await runTest("57. Applicant attempting to change own status to ACCEPTED -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('job_applications/app_1').update({
            status: 'ACCEPTED',
            updatedAt: Date.now()
        }));
    });

    // 58. Any user attempting to delete a job application: DENIED
    await runTest("58. Attempting to delete a job application -> DENIED", async () => {
        await testing.assertFails(alumni1Db.doc('job_applications/app_s1_j1').delete());
    });

    // 59. Job owner deleting their own job: ALLOWED
    await runTest("59. Job owner (faculty_1) deleting their own job posting -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.doc('jobs/job_faculty_1').delete());
    });

    // =========================================================================
    // BATCH 5: MESSAGING, CONVERSATIONS & NOTIFICATIONS (PHASE 5)
    // =========================================================================
    console.log('\n--- BATCH 5: MESSAGING, CONVERSATIONS & NOTIFICATIONS SECURITY RULES ---');

    // 60. Conversation creation between two valid users: ALLOWED
    await runTest("60. Valid user (student_1) creating conversation with another valid user (student_2) -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('conversations/conv_s1_s2').set({
            participantUids: ['student_1', 'student_2'],
            lastMessage: 'Hello peer student!',
            lastMessageTime: Date.now(),
            lastSenderUid: 'student_1',
            createdAt: Date.now(),
            updatedAt: Date.now()
        }));
    });

    // 61. Rejection of self-conversations: DENIED
    await runTest("61. Rejection of self-conversation (participantUids[0] == participantUids[1]) -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('conversations/conv_self').set({
            participantUids: ['student_1', 'student_1'],
            lastMessage: 'Talking to myself',
            lastMessageTime: Date.now(),
            lastSenderUid: 'student_1'
        }));
    });

    // 62. Rejection of conversations with nonexistent users: DENIED
    await runTest("62. Rejection of conversation with nonexistent user (missing public profile) -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('conversations/conv_ghost').set({
            participantUids: ['student_1', 'ghost_user_999'],
            lastMessage: 'Are you there?',
            lastMessageTime: Date.now(),
            lastSenderUid: 'student_1'
        }));
    });

    // 63. Participant access to conversation: ALLOWED
    await runTest("63. Conversation participant (student_2) reading conversation doc -> ALLOWED", async () => {
        await testing.assertSucceeds(student2Db.doc('conversations/conv_s1_s2').get());
    });

    // 64. Nonparticipant access denial to conversation: DENIED
    await runTest("64. Nonparticipant (alumni_1) reading conv_s1_s2 -> DENIED", async () => {
        await testing.assertFails(alumni1Db.doc('conversations/conv_s1_s2').get());
    });

    // 65. Participant querying conversations list: ALLOWED
    await runTest("65. Participant (student_1) querying conversations list (whereArrayContains) -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.collection('conversations')
            .where('participantUids', 'array-contains', 'student_1')
            .get());
    });

    // 66. Message creation by valid participant: ALLOWED
    await runTest("66. Valid participant (student_1) sending message in conv_s1_s2 -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('conversations/conv_s1_s2/messages/msg_1').set({
            senderUid: 'student_1',
            content: 'Let us meet at the Central Library tomorrow.',
            timestamp: Date.now()
        }));
    });

    // 67. Unauthorized message creation denial (nonparticipant): DENIED
    await runTest("67. Nonparticipant (alumni_1) attempting to send message in conv_s1_s2 -> DENIED", async () => {
        await testing.assertFails(alumni1Db.doc('conversations/conv_s1_s2/messages/msg_unauth').set({
            senderUid: 'alumni_1',
            content: 'Intruder message',
            timestamp: Date.now()
        }));
    });

    // 68. Any user attempting to update or delete a message: DENIED
    await runTest("68. Attempting to update or delete message (messages are permanent) -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('conversations/conv_s1_s2/messages/msg_1').update({
            content: 'Tampered message text'
        }));
        await testing.assertFails(student1Db.doc('conversations/conv_s1_s2/messages/msg_1').delete());
    });

    // 69. Participant updating conversation last message metadata: ALLOWED
    await runTest("69. Participant (student_1) updating conversation lastMessage metadata -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('conversations/conv_s1_s2').update({
            participantUids: ['student_1', 'student_2'],
            lastMessage: 'Let us meet at the Central Library tomorrow.',
            lastMessageTime: Date.now(),
            lastSenderUid: 'student_1',
            updatedAt: Date.now()
        }));
    });

    // 70. Direct message notification creation for valid participant: ALLOWED
    await runTest("70. Participant creating direct message notification for recipient -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('users/student_2/notifications/notif_msg_1').set({
            recipientUid: 'student_2',
            senderUid: 'student_1',
            read: false,
            title: 'New Message from Samun',
            message: 'Let us meet at the Central Library tomorrow.',
            type: 'MESSAGE',
            referenceId: 'conv_s1_s2',
            timestamp: Date.now()
        }));
    });

    // 71. Unrelated user attempting to create message notification: DENIED
    await runTest("71. Unrelated user (faculty_1) attempting message notification for conv_s1_s2 -> DENIED", async () => {
        await testing.assertFails(faculty1Db.doc('users/student_2/notifications/notif_fake_msg').set({
            recipientUid: 'student_2',
            senderUid: 'faculty_1',
            read: false,
            title: 'Fake Message Notification',
            message: 'Spam note',
            type: 'MESSAGE',
            referenceId: 'conv_s1_s2',
            timestamp: Date.now()
        }));
    });

    // 72. User reading their own notifications: ALLOWED
    await runTest("72. User (student_2) reading own notifications collection -> ALLOWED", async () => {
        await testing.assertSucceeds(student2Db.collection('users/student_2/notifications').get());
    });

    // 73. User attempting to read another user's notifications: DENIED
    await runTest("73. User (student_1) reading student_2 notifications -> DENIED", async () => {
        await testing.assertFails(student1Db.collection('users/student_2/notifications').get());
    });

    // 74. Recipient marking notification as read (affectedKeys: ['read']): ALLOWED
    await runTest("74. Recipient (student_2) marking notification as read -> ALLOWED", async () => {
        await testing.assertSucceeds(student2Db.doc('users/student_2/notifications/notif_msg_1').update({
            read: true
        }));
    });

    // 75. Recipient attempting to tamper with sender or message: DENIED
    await runTest("75. Recipient attempting to alter notification sender/message -> DENIED", async () => {
        await testing.assertFails(student2Db.doc('users/student_2/notifications/notif_msg_1').update({
            read: true,
            senderUid: 'hacked_sender'
        }));
    });

    // 76. Recipient dismissing/deleting their notification: ALLOWED
    await runTest("76. Recipient (student_2) deleting their notification -> ALLOWED", async () => {
        await testing.assertSucceeds(student2Db.doc('users/student_2/notifications/notif_msg_1').delete());
    });

    // -------------------------------------------------------------------------
    // BATCH 6: REFERENCE REQUESTS, DEMO WALLET, TRANSACTIONS & ESCROW (77 - 107)
    // -------------------------------------------------------------------------
    console.log('\n--- BATCH 6: REFERENCE REQUESTS, DEMO WALLET, TRANSACTIONS & ESCROW SECURITY RULES ---');

    // 77. Student creates valid reference request: ALLOWED
    await runTest("77. Student (student_1) creating valid reference request -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('reference_requests/ref_s1_a1').set({
            studentUid: 'student_1',
            studentName: 'Samun Sadab Wafi',
            studentRoll: '2307003',
            studentDept: 'CSE',
            alumniUid: 'alumni_1',
            targetCompany: 'Google Inc.',
            purpose: 'Software Engineer Referral',
            personalMessage: 'I would be grateful for an internal referral.',
            status: 'PENDING',
            createdAt: Date.now()
        }));
    });

    // 78. Student creating reference request with oneself as referee: DENIED
    await runTest("78. Student creating reference request to themselves -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('reference_requests/ref_self').set({
            studentUid: 'student_1',
            alumniUid: 'student_1',
            targetCompany: 'Self Referral Corp',
            status: 'PENDING',
            createdAt: Date.now()
        }));
    });

    // 79. Unrelated user (student_2) attempting to read student_1 reference request: DENIED
    await runTest("79. Unrelated user (student_2) attempting to read student_1 reference request -> DENIED", async () => {
        await testing.assertFails(student2Db.doc('reference_requests/ref_s1_a1').get());
    });

    // 80. Assigned alumni (alumni_1) reading reference request: ALLOWED
    await runTest("80. Assigned alumni (alumni_1) reading reference request -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.doc('reference_requests/ref_s1_a1').get());
    });

    // 81. Assigned alumni accepting reference request: ALLOWED
    await runTest("81. Assigned alumni (alumni_1) accepting reference request -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.doc('reference_requests/ref_s1_a1').update({
            status: 'ACCEPTED',
            responseNotes: 'Request accepted, drafting letter.',
            updatedAt: Date.now()
        }));
    });

    // 82. Assigned alumni completing recommendation letter: ALLOWED
    await runTest("82. Assigned alumni completing recommendation letter -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.doc('reference_requests/ref_s1_a1').update({
            status: 'COMPLETED',
            recommendationText: 'Samun is an exceptional engineer with strong systems problem-solving skills.',
            responseNotes: 'Official letter uploaded.',
            updatedAt: Date.now()
        }));
    });

    // 83. Unrelated user (faculty_1) attempting to modify reference request: DENIED
    await runTest("83. Unrelated user (faculty_1) attempting to modify reference request -> DENIED", async () => {
        await testing.assertFails(faculty1Db.doc('reference_requests/ref_s1_a1').update({
            status: 'REJECTED',
            updatedAt: Date.now()
        }));
    });

    // 84. Student (student_1) cancelling their own pending reference request: ALLOWED
    await runTest("84. Student (student_1) cancelling their own pending reference request -> ALLOWED", async () => {
        // Create another pending request to test cancellation
        await testing.assertSucceeds(student1Db.doc('reference_requests/ref_to_cancel').set({
            studentUid: 'student_1',
            alumniUid: 'alumni_1',
            targetCompany: 'Another Corp',
            status: 'PENDING',
            createdAt: Date.now()
        }));
        await testing.assertSucceeds(student1Db.doc('reference_requests/ref_to_cancel').update({
            status: 'CANCELLED',
            updatedAt: Date.now()
        }));
    });

    // 85. Attempting to delete a reference request: DENIED
    await runTest("85. Attempting to delete a reference request -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('reference_requests/ref_s1_a1').delete());
    });

    // 86. Owner reading their demo wallet: ALLOWED
    await runTest("86. Owner (student_1) reading their demo wallet doc -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('users/student_1/demo_wallet/main').get());
    });

    // 87. Another user (student_2) attempting to read student_1 demo wallet: DENIED
    await runTest("87. Another user (student_2) attempting to read student_1 demo wallet -> DENIED", async () => {
        await testing.assertFails(student2Db.doc('users/student_1/demo_wallet/main').get());
    });

    // 88. Owner provisioning new initial demo wallet with balance <= 10000: ALLOWED
    await runTest("88. Owner provisioning initial demo wallet with balance <= 10000 -> ALLOWED", async () => {
        await testing.assertSucceeds(alumni1Db.doc('users/alumni_1/demo_wallet/main').set({
            availableBalance: 10000,
            escrowBalance: 0,
            currency: 'DEMO_CREDITS',
            updatedAt: Date.now(),
            disclaimer: 'DEMO WALLET - NO REAL MONEY INVOLVED',
            demoNotice: 'DEMO MODE - All balances and transactions are simulated.'
        }));
    });

    // 89. Owner provisioning initial demo wallet with balance > 10000: DENIED
    await runTest("89. Owner provisioning initial demo wallet with balance > 10000 -> DENIED", async () => {
        await testing.assertFails(alumni2Db.doc('users/alumni_2/demo_wallet/main').set({
            availableBalance: 50000,
            escrowBalance: 0,
            currency: 'DEMO_CREDITS',
            updatedAt: Date.now(),
            disclaimer: 'DEMO WALLET - NO REAL MONEY INVOLVED',
            demoNotice: 'DEMO MODE - All balances and transactions are simulated.'
        }));
    });

    // 90. Balance limit enforcement on update (availableBalance > 100000): DENIED
    await runTest("90. Balance limit enforcement (availableBalance > 100000) -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('users/student_1/demo_wallet/main').update({
            availableBalance: 150000
        }));
    });

    // 91. Another user (faculty_1) attempting to update student_1 wallet: DENIED
    await runTest("91. Another user (faculty_1) attempting to update student_1 wallet -> DENIED", async () => {
        await testing.assertFails(faculty1Db.doc('users/student_1/demo_wallet/main').update({
            availableBalance: 5000
        }));
    });

    // 92. Attempting to delete a demo wallet: DENIED
    await runTest("92. Attempting to delete a demo wallet -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('users/student_1/demo_wallet/main').delete());
    });

    // 93. Owner reading their transaction history: ALLOWED
    await runTest("93. Owner (student_1) reading their transaction records -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('demo_transactions/tx_1').get());
    });

    // 94. Unrelated user (student_2) reading student_1 transaction: DENIED
    await runTest("94. Unrelated user (student_2) reading student_1 transaction -> DENIED", async () => {
        await testing.assertFails(student2Db.doc('demo_transactions/tx_1').get());
    });

    // 95. Owner creating valid demo transaction: ALLOWED
    await runTest("95. Owner (student_1) creating valid demo transaction (amount > 0) -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('demo_transactions/tx_s1_topup').set({
            userId: 'student_1',
            amount: 250,
            type: 'TOPUP',
            title: 'Simulated Top-Up',
            category: 'CREDIT',
            status: 'COMPLETED',
            timestamp: Date.now()
        }));
    });

    // 96. Creating transaction with non-positive amount: DENIED
    await runTest("96. Creating transaction with zero or negative amount -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('demo_transactions/tx_neg').set({
            userId: 'student_1',
            amount: -100,
            type: 'WITHDRAWAL',
            status: 'COMPLETED',
            timestamp: Date.now()
        }));
    });

    // 97. User attempting to modify or delete immutable transaction: DENIED
    await runTest("97. Attempting to modify or delete immutable transaction -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('demo_transactions/tx_s1_topup').update({
            amount: 5000
        }));
        await testing.assertFails(student1Db.doc('demo_transactions/tx_s1_topup').delete());
    });

    // 98. Employer creating demo escrow contract: ALLOWED
    await runTest("98. Employer (faculty_1) creating demo escrow for freelancer (student_1) -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.doc('demo_escrows/escrow_f1_s1').set({
            employerUid: 'faculty_1',
            freelancerUid: 'student_1',
            amount: 2000,
            currency: 'BDT',
            jobId: 201,
            title: 'Research System Implementation',
            status: 'HELD',
            createdAt: Date.now(),
            updatedAt: Date.now()
        }));
    });

    // 99. Employer creating demo escrow with self as freelancer: DENIED
    await runTest("99. Employer creating escrow with self as freelancer -> DENIED", async () => {
        await testing.assertFails(faculty1Db.doc('demo_escrows/escrow_self').set({
            employerUid: 'faculty_1',
            freelancerUid: 'faculty_1',
            amount: 1000,
            currency: 'BDT',
            status: 'HELD',
            createdAt: Date.now()
        }));
    });

    // 100. Freelancer reading their assigned escrow contract: ALLOWED
    await runTest("100. Freelancer (student_1) reading their assigned escrow contract -> ALLOWED", async () => {
        await testing.assertSucceeds(student1Db.doc('demo_escrows/escrow_f1_s1').get());
    });

    // 101. Unrelated user (student_2) reading the escrow contract: DENIED
    await runTest("101. Unrelated user (student_2) reading the escrow contract -> DENIED", async () => {
        await testing.assertFails(student2Db.doc('demo_escrows/escrow_f1_s1').get());
    });

    // 102. Employer releasing escrow (status: 'RELEASED'): ALLOWED
    await runTest("102. Employer (faculty_1) releasing held escrow -> ALLOWED", async () => {
        await testing.assertSucceeds(faculty1Db.doc('demo_escrows/escrow_f1_s1').update({
            status: 'RELEASED',
            resolutionNote: 'Deliverable verified, funds released to freelancer.',
            updatedAt: Date.now()
        }));
    });

    // 103. Employer refunding held escrow: ALLOWED
    await runTest("103. Employer refunding held escrow -> ALLOWED", async () => {
        // Create an escrow to test refund
        await testing.assertSucceeds(faculty1Db.doc('demo_escrows/escrow_to_refund').set({
            employerUid: 'faculty_1',
            freelancerUid: 'student_1',
            amount: 500,
            currency: 'BDT',
            jobId: 202,
            title: 'Short Term Task',
            status: 'HELD',
            createdAt: Date.now(),
            updatedAt: Date.now()
        }));
        await testing.assertSucceeds(faculty1Db.doc('demo_escrows/escrow_to_refund').update({
            status: 'REFUNDED',
            resolutionNote: 'Cancelled by employer before start.',
            updatedAt: Date.now()
        }));
    });

    // 104. Freelancer submitting deliverable note or dispute: ALLOWED
    await runTest("104. Freelancer (student_1) flagging escrow as disputed -> ALLOWED", async () => {
        // Create an escrow to test dispute
        await testing.assertSucceeds(faculty1Db.doc('demo_escrows/escrow_to_dispute').set({
            employerUid: 'faculty_1',
            freelancerUid: 'student_1',
            amount: 800,
            currency: 'BDT',
            jobId: 203,
            title: 'Disputed Task',
            status: 'HELD',
            createdAt: Date.now(),
            updatedAt: Date.now()
        }));
        await testing.assertSucceeds(student1Db.doc('demo_escrows/escrow_to_dispute').update({
            status: 'DISPUTED',
            submissionNote: 'Work completed according to specification.',
            updatedAt: Date.now()
        }));
    });

    // 105. Freelancer attempting to release escrow funds to self: DENIED
    await runTest("105. Freelancer attempting to release escrow funds to self -> DENIED", async () => {
        await testing.assertFails(student1Db.doc('demo_escrows/escrow_to_dispute').update({
            status: 'RELEASED',
            resolutionNote: 'Freelancer self-release attempt.',
            updatedAt: Date.now()
        }));
    });

    // 106. Attempting to tamper with immutable escrow amount or jobId: DENIED
    await runTest("106. Attempting to tamper with immutable escrow amount or jobId -> DENIED", async () => {
        await testing.assertFails(faculty1Db.doc('demo_escrows/escrow_to_dispute').update({
            amount: 999999
        }));
    });

    // 107. Attempting to delete an escrow contract: DENIED
    await runTest("107. Attempting to delete an escrow contract -> DENIED", async () => {
        await testing.assertFails(faculty1Db.doc('demo_escrows/escrow_f1_s1').delete());
    });

    console.log('\n========================================================================');
    console.log(`  FINAL RESULT: ${passedCount} PASSED, ${failedCount} FAILED (TOTAL: ${passedCount + failedCount})`);
    console.log('========================================================================');

    await testEnv.cleanup();

    if (failedCount > 0) {
        process.exit(1);
    }
}

runTestSuite().catch(err => {
    console.error('Fatal test execution error:', err);
    process.exit(1);
});
