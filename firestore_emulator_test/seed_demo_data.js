/**
 * WIZARDS OF TELIGATI — PHASE 7 DEMO DATA POPULATOR
 * Populates pristine, comprehensive, idempotent demonstration data
 * across all collections and subcollections in Firestore.
 */

const testing = require('@firebase/rules-unit-testing');
const fs = require('fs');

const PROJECT_ID = 'wizards-of-teligati';
const HOST = '127.0.0.1';
const PORT = 8081;

async function seedDemoData() {
    process.env.FIRESTORE_EMULATOR_HOST = `${HOST}:${PORT}`;
    console.log(`[Seed] Connecting to Firestore Emulator at ${HOST}:${PORT} for project '${PROJECT_ID}'...`);

    const testEnv = await testing.initializeTestEnvironment({
        projectId: PROJECT_ID,
        firestore: {
            host: HOST,
            port: PORT,
            rules: fs.readFileSync('firestore.rules', 'utf8')
        }
    });

    console.log('[Seed] Inserting comprehensive demo dataset with admin privileges (rules bypassed for pristine setup)...');

    await testEnv.withSecurityRulesDisabled(async (context) => {
        const adminDb = context.firestore();

        // 1. Pre-authorize Faculty Email
        console.log(' -> Seeding authorized_faculty/azharul@cse.kuet.ac.bd');
        await adminDb.doc('authorized_faculty/azharul@cse.kuet.ac.bd').set({
            email: 'azharul@cse.kuet.ac.bd',
            name: 'Prof. Dr. K. M. Azharul Hasan',
            department: 'Computer Science and Engineering',
            authorizedAt: 1730000000000
        }, { merge: true });

        // 2. Private Users (users/{uid})
        console.log(' -> Seeding private user accounts (Student, Teacher, Alumni)');
        await adminDb.doc('users/demo_student_uid').set({
            uid: 'demo_student_uid',
            email: '2307003@stud.kuet.ac.bd',
            role: 'STUDENT',
            name: 'Samun Sadab Wafi',
            department: 'CSE',
            roll: '2307003',
            kuetId: '2307003',
            batch: '2K23',
            cgpa: '3.84',
            createdAt: 1730000000000
        }, { merge: true });

        await adminDb.doc('users/demo_teacher_uid').set({
            uid: 'demo_teacher_uid',
            email: 'azharul@cse.kuet.ac.bd',
            role: 'TEACHER',
            name: 'Prof. Dr. K. M. Azharul Hasan',
            department: 'Dept of CSE',
            designation: 'Professor & Department Head',
            lab: 'Distributed Systems & Cloud Computing Lab',
            researchArea: 'Distributed Database, Cloud Systems',
            createdAt: 1730000000000
        }, { merge: true });

        await adminDb.doc('users/demo_alumni_uid').set({
            uid: 'demo_alumni_uid',
            email: 'tanvir.munich@kuet.ac.bd',
            role: 'ALUMNI',
            name: 'Engr. Tanvir Ahmed',
            department: 'CSE \'15',
            batch: '2K15',
            company: 'BMW Group (Munich, Germany)',
            position: 'Senior Robotics Engineer',
            country: 'Germany',
            createdAt: 1730000000000
        }, { merge: true });

        // 3. Public Profiles (public_profiles/{uid})
        console.log(' -> Seeding sanitized public directories');
        await adminDb.doc('public_profiles/demo_student_uid').set({
            uid: 'demo_student_uid',
            name: 'Samun Sadab Wafi',
            role: 'STUDENT',
            department: 'CSE',
            batch: '2K23'
        }, { merge: true });

        await adminDb.doc('public_profiles/demo_teacher_uid').set({
            uid: 'demo_teacher_uid',
            name: 'Prof. Dr. K. M. Azharul Hasan',
            role: 'TEACHER',
            department: 'Dept of CSE',
            designation: 'Professor & Department Head',
            lab: 'Distributed Systems & Cloud Computing Lab',
            researchArea: 'Distributed Database, Cloud Systems'
        }, { merge: true });

        await adminDb.doc('public_profiles/demo_alumni_uid').set({
            uid: 'demo_alumni_uid',
            name: 'Engr. Tanvir Ahmed',
            role: 'ALUMNI',
            department: 'CSE \'15',
            batch: '2K15',
            company: 'BMW Group (Munich, Germany)',
            position: 'Senior Robotics Engineer'
        }, { merge: true });

        // 4. Research Papers (research_papers/{paperId})
        console.log(' -> Seeding research papers');
        await adminDb.doc('research_papers/demo_paper_1').set({
            ownerUid: 'demo_teacher_uid',
            title: 'High-Throughput Consensus Protocols in Edge-Cloud Systems',
            abstract: 'Investigates Byzantine Fault Tolerant algorithms optimized for heterogeneous wireless sensor mesh clusters with sub-second finality.',
            authors: 'K. M. Azharul Hasan, S. S. Wafi',
            department: 'CSE',
            researchArea: 'Distributed Systems',
            paperUrl: 'https://kuet.ac.bd/cse/papers/edge-cloud.pdf',
            publishedDate: 1730100000000,
            createdAt: 1730100000000
        }, { merge: true });

        await adminDb.doc('research_papers/demo_paper_2').set({
            ownerUid: 'demo_teacher_uid',
            title: 'Scalable Distributed Graph Indexing on Heterogeneous Accelerators',
            abstract: 'GPU-accelerated partitioning algorithms for billion-node topological graph analytics in large-scale academic knowledge engines.',
            authors: 'K. M. Azharul Hasan, M. Rahman',
            department: 'CSE',
            researchArea: 'Cloud Computing & Databases',
            paperUrl: 'https://kuet.ac.bd/cse/papers/graph-index.pdf',
            publishedDate: 1730200000000,
            createdAt: 1730200000000
        }, { merge: true });

        // 5. Research Proposals (research_proposals/{proposalId})
        console.log(' -> Seeding student research proposals');
        await adminDb.doc('research_proposals/demo_prop_1').set({
            studentUid: 'demo_student_uid',
            studentName: 'Samun Sadab Wafi',
            studentRoll: '2307003',
            studentDept: 'CSE',
            facultyUid: 'demo_teacher_uid',
            facultyName: 'Prof. Dr. K. M. Azharul Hasan',
            topic: 'Decentralized Micro-Escrow Protocols over Edge Networks',
            proposalAbstract: 'Formal verification of Byzantine-resistant multi-party atomic swaps for distributed campus compute node resource exchange.',
            degreeType: 'B.Sc. Thesis',
            status: 'ACCEPTED',
            feedback: 'Promising thesis scope. Please schedule initial lab sync on Monday morning.',
            submittedAt: 1730300000000,
            updatedAt: 1730350000000
        }, { merge: true });

        await adminDb.doc('research_proposals/demo_prop_2').set({
            studentUid: 'demo_student_uid',
            studentName: 'Samun Sadab Wafi',
            studentRoll: '2307003',
            studentDept: 'CSE',
            facultyUid: 'demo_teacher_uid',
            facultyName: 'Prof. Dr. K. M. Azharul Hasan',
            topic: 'Automated Vulnerability Detection in Android Smart Contracts',
            proposalAbstract: 'Static analysis framework targeting bytecode deobfuscation and state-reentrancy vulnerability prevention.',
            degreeType: 'Undergraduate Research Project',
            status: 'REVISION_REQUESTED',
            feedback: 'Methodology needs formal threat modeling section. Revise and resubmit by Friday.',
            submittedAt: 1730400000000,
            updatedAt: 1730450000000
        }, { merge: true });

        // 6. Jobs & Opportunities (jobs/{jobId})
        console.log(' -> Seeding professional and assistant opportunities');
        await adminDb.doc('jobs/demo_job_bmw').set({
            postedByUid: 'demo_alumni_uid',
            postedByName: 'Engr. Tanvir Ahmed',
            postedByRole: 'ALUMNI',
            title: 'Autonomous Driving Perception Intern',
            organization: 'BMW Group',
            location: 'Munich, Germany (Hybrid / Remote Option)',
            category: 'PROFESSIONAL',
            compensation: '€1,800/month',
            description: 'C++ and ROS2 middleware development for multi-camera surround vision perception stack.',
            status: 'OPEN',
            deadline: '2026-12-15',
            createdAt: 1730250000000
        }, { merge: true });

        await adminDb.doc('jobs/demo_job_lab_asst').set({
            postedByUid: 'demo_teacher_uid',
            postedByName: 'Prof. Dr. K. M. Azharul Hasan',
            postedByRole: 'TEACHER',
            title: 'Graduate Systems Lab Assistant',
            organization: 'KUET CSE Distributed Systems Lab',
            location: 'CSE Building Room 302',
            category: 'ASSISTANT',
            compensation: '৳12,000/month [Demo]',
            description: 'Assist with hardware cluster orchestration, Proxmox hypervisor configuration, and grading distributed computing labs.',
            status: 'OPEN',
            deadline: '2026-11-30',
            createdAt: 1730260000000
        }, { merge: true });

        // 7. Job Applications (job_applications/{appId})
        console.log(' -> Seeding job applications');
        await adminDb.doc('job_applications/demo_app_bmw').set({
            applicantUid: 'demo_student_uid',
            applicantName: 'Samun Sadab Wafi',
            applicantRoll: '2307003',
            applicantDept: 'CSE',
            applicantEmail: '2307003@stud.kuet.ac.bd',
            jobId: 'demo_job_bmw',
            jobTitle: 'Autonomous Driving Perception Intern',
            posterUid: 'demo_alumni_uid',
            statement: 'Extensive hands-on experience in ROS2 nodes and real-time point-cloud registration during Intra-KUET robotics competitions.',
            status: 'ACCEPTED',
            posterNotes: 'Strong robotics background verified. Technical interview cleared.',
            appliedAt: 1730320000000,
            updatedAt: 1730380000000
        }, { merge: true });

        await adminDb.doc('job_applications/demo_app_asst').set({
            applicantUid: 'demo_student_uid',
            applicantName: 'Samun Sadab Wafi',
            applicantRoll: '2307003',
            applicantDept: 'CSE',
            applicantEmail: '2307003@stud.kuet.ac.bd',
            jobId: 'demo_job_lab_asst',
            jobTitle: 'Graduate Systems Lab Assistant',
            posterUid: 'demo_teacher_uid',
            statement: 'Experienced in Linux kernel tracing and Docker cluster automation.',
            status: 'PENDING',
            appliedAt: 1730420000000,
            updatedAt: 1730420000000
        }, { merge: true });

        // 8. Reference Requests (reference_requests/{requestId})
        console.log(' -> Seeding reference requests');
        await adminDb.doc('reference_requests/demo_ref_bmw').set({
            studentUid: 'demo_student_uid',
            studentName: 'Samun Sadab Wafi',
            studentRoll: '2307003',
            studentDept: 'CSE',
            alumniUid: 'demo_alumni_uid',
            alumniName: 'Engr. Tanvir Ahmed',
            targetCompany: 'BMW Group',
            targetInstitution: 'BMW Group',
            targetProgram: 'Robotics Engineering',
            purpose: 'JOB_REFERRAL',
            message: 'Hello Engr. Tanvir, I would be deeply grateful for your referral and mentorship for the Perception Intern opening.',
            status: 'COMPLETED',
            recommendationText: 'I enthusiastically recommend Samun Sadab Wafi. His foundation in systems programming and KUET competitive track record makes him an exemplary candidate.',
            responseNotes: 'Official letter delivered.',
            createdAt: 1730330000000,
            requestedAt: 1730330000000,
            updatedAt: 1730390000000
        }, { merge: true });

        // 9. Conversations and Messages
        console.log(' -> Seeding conversations and messages');
        await adminDb.doc('conversations/demo_conv_student_alumni').set({
            participantUids: ['demo_student_uid', 'demo_alumni_uid'],
            lastMessage: 'Thank you for the recommendation letter!',
            lastMessageTime: 1730400000000,
            lastSenderUid: 'demo_student_uid',
            unreadCounts: {
                'demo_student_uid': 0,
                'demo_alumni_uid': 1
            },
            createdAt: 1730340000000,
            updatedAt: 1730400000000
        }, { merge: true });

        await adminDb.doc('conversations/demo_conv_student_alumni/messages/msg_1').set({
            senderUid: 'demo_alumni_uid',
            senderName: 'Engr. Tanvir Ahmed',
            content: 'Hi Samun, saw your application for the BMW perception opening. Your systems projects look solid.',
            timestamp: 1730350000000
        }, { merge: true });

        await adminDb.doc('conversations/demo_conv_student_alumni/messages/msg_2').set({
            senderUid: 'demo_student_uid',
            senderName: 'Samun Sadab Wafi',
            content: 'Thank you for the recommendation letter!',
            timestamp: 1730400000000
        }, { merge: true });

        // 10. Notifications
        console.log(' -> Seeding event notifications');
        await adminDb.doc('users/demo_student_uid/notifications/demo_notif_1').set({
            recipientUid: 'demo_student_uid',
            senderUid: 'demo_teacher_uid',
            senderName: 'Prof. Dr. K. M. Azharul Hasan',
            title: 'Proposal Decision: ACCEPTED',
            message: 'Your research proposal was reviewed and accepted.',
            type: 'PROPOSAL_DECISION',
            referenceId: 'demo_prop_1',
            read: false,
            createdAt: 1730350000000
        }, { merge: true });

        await adminDb.doc('users/demo_student_uid/notifications/demo_notif_2').set({
            recipientUid: 'demo_student_uid',
            senderUid: 'demo_alumni_uid',
            senderName: 'Engr. Tanvir Ahmed',
            title: 'Reference Request: COMPLETED',
            message: 'Your recommendation letter for BMW Group is now ready.',
            type: 'REFERENCE_DECISION',
            referenceId: 'demo_ref_bmw',
            read: false,
            createdAt: 1730390000000
        }, { merge: true });

        await adminDb.doc('users/demo_student_uid/notifications/demo_notif_3').set({
            recipientUid: 'demo_student_uid',
            senderUid: 'demo_alumni_uid',
            senderName: 'Engr. Tanvir Ahmed',
            title: 'Job Application Accepted',
            message: 'You have been selected for Autonomous Driving Perception Intern.',
            type: 'APPLICATION_DECISION',
            referenceId: 'demo_app_bmw',
            read: true,
            createdAt: 1730385000000
        }, { merge: true });

        // 11. Demo Wallets
        console.log(' -> Seeding demo simulation wallets');
        await adminDb.doc('users/demo_student_uid/demo_wallet/main').set({
            availableBalance: 8500.0,
            escrowBalance: 1500.0,
            currency: 'DEMO_CREDITS',
            updatedAt: 1730450000000,
            disclaimer: 'DEMO WALLET - NO REAL MONEY INVOLVED',
            demoNotice: 'DEMO MODE - All balances and transactions are simulated.'
        }, { merge: true });

        await adminDb.doc('users/demo_teacher_uid/demo_wallet/main').set({
            availableBalance: 10000.0,
            escrowBalance: 0.0,
            currency: 'DEMO_CREDITS',
            updatedAt: 1730450000000,
            disclaimer: 'DEMO WALLET - NO REAL MONEY INVOLVED',
            demoNotice: 'DEMO MODE - All balances and transactions are simulated.'
        }, { merge: true });

        await adminDb.doc('users/demo_alumni_uid/demo_wallet/main').set({
            availableBalance: 10000.0,
            escrowBalance: 0.0,
            currency: 'DEMO_CREDITS',
            updatedAt: 1730450000000,
            disclaimer: 'DEMO WALLET - NO REAL MONEY INVOLVED',
            demoNotice: 'DEMO MODE - All balances and transactions are simulated.'
        }, { merge: true });

        // 12. Demo Transactions
        console.log(' -> Seeding demo transactions');
        await adminDb.doc('demo_transactions/demo_tx_1').set({
            userId: 'demo_student_uid',
            amount: 1000.0,
            type: 'WITHDRAWAL',
            title: 'Simulated Withdrawal (Demo Bank)',
            category: 'DEBIT',
            status: 'COMPLETED',
            timestamp: 1730440000000,
            disclaimer: 'DEMO WALLET - NO REAL MONEY INVOLVED'
        }, { merge: true });

        await adminDb.doc('demo_transactions/demo_tx_2').set({
            userId: 'demo_student_uid',
            amount: 500.0,
            type: 'TOPUP',
            title: 'Simulated Top-Up (bKash Demo)',
            category: 'CREDIT',
            status: 'COMPLETED',
            timestamp: 1730410000000,
            disclaimer: 'DEMO WALLET - NO REAL MONEY INVOLVED'
        }, { merge: true });

        // 13. Demo Escrows
        console.log(' -> Seeding demo escrows');
        await adminDb.doc('demo_escrows/demo_escrow_1').set({
            employerUid: 'demo_teacher_uid',
            freelancerUid: 'demo_student_uid',
            amount: 1500.0,
            currency: 'BDT',
            jobId: 101,
            title: 'Distributed Systems Benchmark Implementation',
            description: 'Simulated Milestone Payment for cluster telemetry benchmark.',
            status: 'HELD',
            createdAt: 1730420000000,
            updatedAt: 1730420000000
        }, { merge: true });
    });

    console.log('[Seed] All demo fixtures successfully seeded into Firestore Emulator!');
    await testEnv.cleanup();
}

seedDemoData().catch(err => {
    console.error('Fatal seeding error:', err);
    process.exit(1);
});
