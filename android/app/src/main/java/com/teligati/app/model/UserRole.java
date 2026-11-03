package com.teligati.app.model;

public enum UserRole {
    STUDENT("KUET Student", "2307003", "CSE 2K23"),
    ALUMNI("KUET Alumni", "tanvir.munich@kuet.ac.bd", "BMW Group (Munich, Germany)"),
    FACULTY("KUET Faculty / Teacher", "azharul@cse.kuet.ac.bd", "Professor • CSE Dept");

    private final String roleTitle;
    private final String defaultId;
    private final String defaultOrg;

    UserRole(String roleTitle, String defaultId, String defaultOrg) {
        this.roleTitle = roleTitle;
        this.defaultId = defaultId;
        this.defaultOrg = defaultOrg;
    }

    public String getRoleTitle() { return roleTitle; }
    public String getDefaultId() { return defaultId; }
    public String getDefaultOrg() { return defaultOrg; }
}
