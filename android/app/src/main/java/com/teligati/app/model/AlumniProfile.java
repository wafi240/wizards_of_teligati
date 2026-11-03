package com.teligati.app.model;

import java.util.List;

public class AlumniProfile {
    private long id;
    private String name;
    private String department;
    private String batch;
    private String currentCompany;
    private String currentPosition;
    private String country;
    private String city;
    private boolean isVerified;
    private boolean isAvailableForMentorship;
    private boolean isActivelyHiring;
    private List<String> skills;
    private String bio;

    public AlumniProfile(long id, String name, String department, String batch, String currentCompany,
                         String currentPosition, String country, String city, boolean isVerified,
                         boolean isAvailableForMentorship, boolean isActivelyHiring, List<String> skills, String bio) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.batch = batch;
        this.currentCompany = currentCompany;
        this.currentPosition = currentPosition;
        this.country = country;
        this.city = city;
        this.isVerified = isVerified;
        this.isAvailableForMentorship = isAvailableForMentorship;
        this.isActivelyHiring = isActivelyHiring;
        this.skills = skills;
        this.bio = bio;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getBatch() { return batch; }
    public String getCurrentCompany() { return currentCompany; }
    public String getCurrentPosition() { return currentPosition; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public boolean isVerified() { return isVerified; }
    public boolean isAvailableForMentorship() { return isAvailableForMentorship; }
    public boolean isActivelyHiring() { return isActivelyHiring; }
    public List<String> getSkills() { return skills; }
    public String getBio() { return bio; }
}
