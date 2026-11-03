package com.teligati.app.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SampleData {
    public static List<Opportunity> getOpportunities() {
        List<Opportunity> list = new ArrayList<>();
        list.add(new Opportunity(
                1,
                "Personal Technical Assistant for Automation",
                OpportunityCategory.ASSISTANT_WORK,
                "Alumni Tech Advisory (Munich)",
                "Remote",
                "Engr. Tanvir Ahmed (CSE '12)",
                "Staff Engineer @ BMW Munich (KUET Alumni)",
                "৳15,000 / month",
                "Flexible (10-12 hrs/week)",
                "Looking for a proactive KUET junior to help automate data processing scripts. Replacement for conventional tuitions with real engineering experience.",
                Arrays.asList("Python", "SQL", "Git"),
                3.25
        ));
        list.add(new Opportunity(
                2,
                "Research Assistant — Computer Vision for Autonomous Vehicles",
                OpportunityCategory.RESEARCH_COLLABORATION,
                "KUET Intelligent Systems Lab",
                "KUET Campus / Hybrid",
                "Dr. K. M. Azharul Hasan",
                "Professor & Head of Research • CSE Dept",
                "৳12,000 / month stipend",
                "6 months (Eligible for Co-Authorship)",
                "Seeking 3rd/4th-year students to assist in developing real-time YOLOv8 object detection on edge devices. Opportunity to publish IEEE papers.",
                Arrays.asList("Python", "PyTorch", "OpenCV"),
                3.50
        ));
        list.add(new Opportunity(
                3,
                "PhD Research Collaboration — Distributed Microservices",
                OpportunityCategory.RESEARCH_COLLABORATION,
                "Technical University of Munich / KUET Alumni",
                "Remote (Germany - Bangladesh)",
                "Dr. Niaz Morshed (CSE '10)",
                "Postdoctoral Researcher @ TU Munich",
                "€150 / month (~৳19,000)",
                "4 months",
                "Collaborate on benchmark evaluation and log analysis for cloud-native Kubernetes microservices.",
                Arrays.asList("Docker", "Go", "Java"),
                3.40
        ));
        list.add(new Opportunity(
                4,
                "Quick Bug Fix & Unit Test Script",
                OpportunityCategory.MICRO_TASKS,
                "Alumni Startup (Dhaka)",
                "Remote",
                "Fahim Shahriar (CSE '18)",
                "Co-Founder @ DevNexus",
                "৳1,500 (1-Day Task)",
                "Under 4 hours",
                "Write JUnit tests for an authentication module. Escrow payment released immediately upon GitHub PR approval.",
                Arrays.asList("Java", "JUnit 5", "Spring Boot"),
                null
        ));
        list.add(new Opportunity(
                5,
                "Software Engineer (Mobile & Cloud)",
                OpportunityCategory.FULLTIME_JOBS,
                "ABC Global Technologies",
                "Berlin, Germany / Remote",
                "Rahman Ahmed (CSE '14)",
                "Engineering Lead @ ABC Technologies",
                "€55,000 - €70,000 / year",
                "Full-time Permanent",
                "We prioritize KUET graduates! Looking for strong core computer science fundamentals and Java/Spring backend experience.",
                Arrays.asList("Java", "Spring Boot", "PostgreSQL"),
                3.30
        ));
        return list;
    }

    public static List<AlumniProfile> getAlumniList() {
        List<AlumniProfile> list = new ArrayList<>();
        list.add(new AlumniProfile(
                1, "Rahim Ahmed", "CSE", "2K12", "Google", "Senior Software Engineer",
                "Germany", "Munich", true, true, true,
                Arrays.asList("Distributed Systems", "Go", "Kubernetes"),
                "Working on Google Cloud infrastructure. Passionate about helping KUET juniors navigate international careers."
        ));
        list.add(new AlumniProfile(
                2, "Tanvir Hassan", "EEE", "2K15", "Tesla", "Power Electronics Engineer",
                "USA", "Austin, Texas", true, true, false,
                Arrays.asList("Hardware Design", "Power Electronics", "Embedded C"),
                "Former Robonix KUET leader. Glad to mentor students interested in EV powertrain systems."
        ));
        list.add(new AlumniProfile(
                3, "Nusrat Jahan", "CSE", "2K17", "Amazon", "Software Development Engineer II",
                "Canada", "Vancouver", true, true, true,
                Arrays.asList("AWS", "Java", "DynamoDB"),
                "Available for resume reviews, mock interviews, and personal assistant opportunities for KUETians."
        ));
        return list;
    }

    public static List<ProjectItem> getProjects() {
        List<ProjectItem> list = new ArrayList<>();
        list.add(new ProjectItem(
                1, "ForKn8 Interactive Platform", "Open Source Software",
                "Interactive collaboration platform for engineering developers with integrated build pipelines.",
                Arrays.asList("Java", "Spring Boot", "PostgreSQL", "Docker"),
                "Samun Sadab Wafi", "CSE 2K23", 4, true, "github.com/kuet/forkn8", ""
        ));
        list.add(new ProjectItem(
                2, "Defect Detection on Solar Arrays", "Research Paper",
                "Deep learning thermal imaging paper accepted for IEEE Student Conference. Looking for co-authors.",
                Arrays.asList("Python", "PyTorch", "Thermal Vision", "LaTeX"),
                "Dr. Aminul Islam", "KUET Robotics Lab", 3, true, "", "10.1109/KUET.2026.002"
        ));
        return list;
    }

    public static List<TransactionRecord> getTransactions() {
        List<TransactionRecord> list = new ArrayList<>();
        list.add(new TransactionRecord("TX-9021", "Technical Assistant Work (October)", "Assistant Work", 15000.0, "Oct 30, 2026", "Completed"));
        list.add(new TransactionRecord("TX-8842", "JUnit Module Bugfix Task", "Micro Task", 1500.0, "Oct 22, 2026", "Completed"));
        list.add(new TransactionRecord("TX-9105", "Automated Script Development", "Assistant Work", 3500.0, "Nov 02, 2026", "In Escrow"));
        return list;
    }
}
