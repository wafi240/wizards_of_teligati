package com.teligati.app.model;

public enum OpportunityCategory {
    ALL("All Opportunities"),
    ASSISTANT_WORK("🤝 Assistant Work"),
    RESEARCH_COLLABORATION("🔬 Research & Papers"),
    MICRO_TASKS("⚡ Micro-Tasks (৳100+)"),
    FULLTIME_JOBS("💼 Jobs & Internships");

    private final String displayName;

    OpportunityCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
