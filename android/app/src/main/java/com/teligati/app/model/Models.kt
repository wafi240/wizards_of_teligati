package com.teligati.app.model

enum class OpportunityCategory(val displayName: String) {
    ALL("All Opportunities"),
    ASSISTANT_WORK("🤝 Assistant Work"),
    RESEARCH_COLLABORATION("🔬 Research & Papers"),
    MICRO_TASKS("⚡ Micro-Tasks (৳100+)"),
    FULLTIME_JOBS("💼 Jobs & Internships")
}

data class Opportunity(
    val id: Long,
    val title: String,
    val category: OpportunityCategory,
    val organization: String,
    val location: String,
    val postedByName: String,
    val posterRole: String,
    val compensation: String,
    val duration: String,
    val description: String,
    val skillsRequired: List<String>,
    val preferredClubs: List<String> = emptyList(),
    val minCgpa: Double? = null,
    val isRemote: Boolean = true,
    var isApplied: Boolean = false
)

data class AlumniProfile(
    val id: Long,
    val name: String,
    val department: String,
    val batch: String,
    val currentCompany: String,
    val currentPosition: String,
    val country: String,
    val city: String,
    val isVerified: Boolean = true,
    val isAvailableForMentorship: Boolean = true,
    val isActivelyHiring: Boolean = true,
    val skills: List<String> = emptyList(),
    val bio: String = ""
)

data class FacultyProfile(
    val id: Long,
    val name: String,
    val designation: String,
    val department: String,
    val university: String,
    val researchLab: String,
    val researchAreas: List<String>,
    val openAssistantPositions: Int,
    val publicationsCount: Int,
    val isAcceptingStudents: Boolean = true
)

data class StudentPortfolio(
    val name: String,
    val roll: String,
    val department: String,
    val batch: String,
    val cgpa: Double,
    val academicYear: String,
    val clubs: List<String>,
    val leadershipRoles: List<String>,
    val skills: List<String>,
    val achievements: List<String>,
    val personalProjects: List<String>,
    val availableHoursPerWeek: Int,
    val earningsAvailable: Double,
    val earningsPendingEscrow: Double
)

data class ProjectItem(
    val id: Long,
    val name: String,
    val type: String,
    val description: String,
    val technologies: List<String>,
    val ownerName: String,
    val ownerDept: String,
    val contributorsCount: Int,
    val isOpenForContribution: Boolean,
    val githubUrl: String = "",
    val paperDoi: String = ""
)

data class TransactionRecord(
    val id: String,
    val title: String,
    val category: String,
    val amount: Double,
    val date: String,
    val status: String
)

object SampleData {
    val studentPortfolio = StudentPortfolio(
        name = "Samun Sadab Wafi",
        roll = "2307003",
        department = "Computer Science & Engineering",
        batch = "2K23",
        cgpa = 3.84,
        academicYear = "2nd Year Undergraduate",
        clubs = listOf("SGIPC (Programming Club)", "KUET Career Club", "IEEE KUET Student Branch"),
        leadershipRoles = listOf("Competitive Programming Executive @ SGIPC", "Android Workshop Coordinator"),
        skills = listOf("Kotlin", "Jetpack Compose", "Python", "Data Structures & Algorithms", "Git", "Spring Boot"),
        achievements = listOf("Dean's List Award (1st Year)", "KUET Intra-University Programming Contest - Rank 8", "Hackathon Finalist"),
        personalProjects = listOf("Wizards of Teligati Android App", "ForKn8 Interactive Platform", "Smart Campus Transit Tracker"),
        availableHoursPerWeek = 15,
        earningsAvailable = 18500.0,
        earningsPendingEscrow = 3500.0
    )

    val opportunities = mutableListOf(
        Opportunity(
            id = 1,
            title = "Personal Technical Assistant for Automation",
            category = OpportunityCategory.ASSISTANT_WORK,
            organization = "Alumni Tech Advisory (Munich)",
            location = "Remote",
            postedByName = "Engr. Tanvir Ahmed (CSE '12)",
            posterRole = "Staff Engineer @ BMW Munich (KUET Alumni)",
            compensation = "৳15,000 / month",
            duration = "Flexible (10-12 hrs/week)",
            description = "Looking for a proactive KUET junior to help automate data processing scripts, organize technical documentation, and coordinate technical research. Ideal replacement for conventional tuitions with real engineering experience.",
            skillsRequired = listOf("Python", "Excel / Google Sheets", "Basic SQL", "Good Communication"),
            preferredClubs = listOf("SGIPC", "KUET Career Club"),
            minCgpa = 3.25
        ),
        Opportunity(
            id = 2,
            title = "Research Assistant — Computer Vision for Autonomous Vehicles",
            category = OpportunityCategory.RESEARCH_COLLABORATION,
            organization = "KUET Intelligent Systems Lab",
            location = "KUET Campus / Hybrid",
            postedByName = "Dr. K. M. Azharul Hasan",
            posterRole = "Professor & Head of Research • CSE Dept",
            compensation = "৳12,000 / month stipend",
            duration = "6 months (Eligible for Co-Authorship)",
            description = "Seeking 3rd/4th-year students to assist in developing real-time YOLOv8 object detection on edge devices. Opportunity to publish papers in IEEE/Springer international conferences.",
            skillsRequired = listOf("Python", "PyTorch / OpenCV", "Machine Learning", "Research Writing"),
            preferredClubs = listOf("Robonix KUET", "SGIPC"),
            minCgpa = 3.50
        ),
        Opportunity(
            id = 3,
            title = "PhD Research Collaboration — Distributed Microservices",
            category = OpportunityCategory.RESEARCH_COLLABORATION,
            organization = "Technical University of Munich / KUET Alumni",
            location = "Remote (Germany - Bangladesh)",
            postedByName = "Dr. Niaz Morshed (CSE '10)",
            posterRole = "Postdoctoral Researcher @ TU Munich",
            compensation = "€150 / month (~৳19,000)",
            duration = "4 months",
            description = "Collaborate on benchmark evaluation and log analysis for cloud-native Kubernetes microservices. You will receive direct guidance for German graduate school applications.",
            skillsRequired = listOf("Docker", "Go or Java", "Linux", "Data Analysis"),
            minCgpa = 3.40
        ),
        Opportunity(
            id = 4,
            title = "Quick Bug Fix & Unit Test Script",
            category = OpportunityCategory.MICRO_TASKS,
            organization = "Alumni Startup (Dhaka)",
            location = "Remote",
            postedByName = "Fahim Shahriar (CSE '18)",
            posterRole = "Co-Founder @ DevNexus",
            compensation = "৳1,500 (1-Day Task)",
            duration = "Under 4 hours",
            description = "Write JUnit tests for a Spring Boot authentication module and verify token validation edge cases. Escrow payment released immediately upon GitHub PR approval.",
            skillsRequired = listOf("Java", "JUnit 5", "Spring Boot"),
            minCgpa = null
        ),
        Opportunity(
            id = 5,
            title = "Data Labeling & Scraping for Research Dataset",
            category = OpportunityCategory.MICRO_TASKS,
            organization = "KUET EEE Research Group",
            location = "Remote / Campus",
            postedByName = "Engr. Sadia Afrin (EEE '19)",
            posterRole = "Lecturer • Dept of EEE",
            compensation = "৳800 (Quick Task)",
            duration = "2 hours",
            description = "Clean and categorize a 500-sample solar irradiance dataset in CSV format according to given guidelines.",
            skillsRequired = listOf("Python", "Pandas", "Attention to Detail"),
            minCgpa = null
        ),
        Opportunity(
            id = 6,
            title = "Software Engineer (Mobile & Cloud)",
            category = OpportunityCategory.FULLTIME_JOBS,
            organization = "ABC Global Technologies",
            location = "Berlin, Germany (Visa Sponsored) / Remote",
            postedByName = "Rahman Ahmed (CSE '14)",
            posterRole = "Engineering Lead @ ABC Technologies",
            compensation = "€55,000 - €70,000 / year",
            duration = "Full-time Permanent",
            description = "We prioritize KUET graduates! Looking for strong core computer science fundamentals, Kotlin/Android or Java/Spring backend experience.",
            skillsRequired = listOf("Kotlin", "Jetpack Compose", "Coroutines", "Clean Architecture"),
            preferredClubs = listOf("SGIPC"),
            minCgpa = 3.30
        )
    )

    val alumniList = listOf(
        AlumniProfile(
            id = 1,
            name = "Rahim Ahmed",
            department = "CSE",
            batch = "2K12",
            currentCompany = "Google",
            currentPosition = "Senior Software Engineer",
            country = "Germany",
            city = "Munich",
            isVerified = true,
            isAvailableForMentorship = true,
            isActivelyHiring = true,
            skills = listOf("Distributed Systems", "Go", "C++", "Kubernetes"),
            bio = "Working on Google Cloud infrastructure. Passionate about helping KUET juniors navigate international careers and higher studies in Europe."
        ),
        AlumniProfile(
            id = 2,
            name = "Tanvir Hassan",
            department = "EEE",
            batch = "2K15",
            currentCompany = "Tesla",
            currentPosition = "Power Electronics Engineer",
            country = "USA",
            city = "Austin, Texas",
            isVerified = true,
            isAvailableForMentorship = true,
            isActivelyHiring = false,
            skills = listOf("Hardware Design", "Power Electronics", "Embedded C", "Simulink"),
            bio = "Former Robonix KUET leader. Glad to mentor students interested in EV powertrain systems and US graduate school scholarships."
        ),
        AlumniProfile(
            id = 3,
            name = "Nusrat Jahan",
            department = "CSE",
            batch = "2K17",
            currentCompany = "Amazon",
            currentPosition = "Software Development Engineer II",
            country = "Canada",
            city = "Vancouver",
            isVerified = true,
            isAvailableForMentorship = true,
            isActivelyHiring = true,
            skills = listOf("AWS", "Java", "Kotlin", "DynamoDB"),
            bio = "Available for resume reviews, mock interviews, and personal assistant opportunities for KUETians."
        ),
        AlumniProfile(
            id = 4,
            name = "Mahmudul Hasan",
            department = "ME",
            batch = "2K13",
            currentCompany = "Siemens Energy",
            currentPosition = "Thermal Systems Specialist",
            country = "Germany",
            city = "Berlin",
            isVerified = true,
            isAvailableForMentorship = true,
            isActivelyHiring = true,
            skills = listOf("Thermodynamics", "Ansys Fluent", "CAD"),
            bio = "Active KUET Alumni Germany Chapter member. Looking for technical assistants for CAD modeling."
        )
    )

    val facultyList = listOf(
        FacultyProfile(
            id = 1,
            name = "Prof. Dr. K. M. Azharul Hasan",
            designation = "Professor",
            department = "Computer Science & Engineering",
            university = "KUET",
            researchLab = "Big Data & Intelligent Computing Lab",
            researchAreas = listOf("Data Engineering", "Machine Learning", "Cloud Systems"),
            openAssistantPositions = 2,
            publicationsCount = 85,
            isAcceptingStudents = true
        ),
        FacultyProfile(
            id = 2,
            name = "Prof. Dr. Aminul Islam",
            designation = "Professor & Ex-KUETian",
            department = "Mechanical Engineering",
            university = "KUET & Monash Collaboration",
            researchLab = "Robotics, Automation & Mechatronics Center",
            researchAreas = listOf("Autonomous Rovers", "SLAM", "Industrial Automation"),
            openAssistantPositions = 3,
            publicationsCount = 62,
            isAcceptingStudents = true
        ),
        FacultyProfile(
            id = 3,
            name = "Dr. Niaz Morshed (Alumni Faculty)",
            designation = "Postdoctoral Researcher & PhD Mentor",
            department = "CSE '10 Alumni",
            university = "Technical University of Munich (Germany)",
            researchLab = "Cloud & Distributed Systems Group",
            researchAreas = listOf("Microservices", "Fault Tolerance", "Distributed Consensus"),
            openAssistantPositions = 1,
            publicationsCount = 28,
            isAcceptingStudents = true
        )
    )

    val projectsList = listOf(
        ProjectItem(
            id = 1,
            name = "ForKn8 Interactive Platform",
            type = "Open Source Software",
            description = "Interactive collaboration platform for engineering developers with integrated build pipelines.",
            technologies = listOf("Kotlin", "Spring Boot", "PostgreSQL", "Docker"),
            ownerName = "Samun Sadab Wafi",
            ownerDept = "CSE 2K23",
            contributorsCount = 4,
            isOpenForContribution = true,
            githubUrl = "github.com/kuet/forkn8"
        ),
        ProjectItem(
            id = 2,
            name = "Real-time Defect Detection on Solar Arrays",
            type = "Research Paper Project",
            description = "Deep learning thermal imaging paper accepted for IEEE Student Conference. Looking for co-authors to conduct validation experiments.",
            technologies = listOf("Python", "PyTorch", "Thermal Vision", "LaTeX"),
            ownerName = "Dr. Aminul Islam / Sadia Afrin",
            ownerDept = "KUET Robotics Lab",
            contributorsCount = 3,
            isOpenForContribution = true,
            paperDoi = "10.1109/KUET.2026.002"
        ),
        ProjectItem(
            id = 3,
            name = "KUET Campus Ride & Guild Hub",
            type = "Student Club Project",
            description = "Centralized shuttle tracking and club event announcements platform built by KUET students.",
            technologies = listOf("Kotlin", "Jetpack Compose", "Firebase"),
            ownerName = "KUET Career & App Club",
            ownerDept = "Inter-Department",
            contributorsCount = 5,
            isOpenForContribution = true,
            githubUrl = "github.com/kuet/campus-hub"
        )
    )

    val transactions = listOf(
        TransactionRecord("TX-9021", "Technical Assistant Work (October)", "Assistant Work", 15000.0, "Oct 30, 2026", "Completed"),
        TransactionRecord("TX-8842", "JUnit Module Bugfix Task", "Micro Task", 1500.0, "Oct 22, 2026", "Completed"),
        TransactionRecord("TX-8610", "Dataset Labeling & Formatting", "Micro Task", 800.0, "Oct 15, 2026", "Completed"),
        TransactionRecord("TX-9105", "Automated Script Development", "Assistant Work", 3500.0, "Nov 02, 2026", "In Escrow")
    )
}
