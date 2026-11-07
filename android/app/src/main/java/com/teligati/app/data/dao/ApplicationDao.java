package com.teligati.app.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.teligati.app.data.entity.JobApplicationEntity;
import java.util.List;

@Dao
public interface ApplicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertJobApplication(JobApplicationEntity app);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertJobApplications(List<JobApplicationEntity> apps);

    @Query("SELECT * FROM job_applications ORDER BY appliedAt DESC")
    List<JobApplicationEntity> getAllJobApplications();

    @Query("SELECT * FROM job_applications WHERE applicantRoll = :studentRoll ORDER BY appliedAt DESC")
    List<JobApplicationEntity> getApplicationsByStudent(String studentRoll);

    @Query("SELECT * FROM job_applications WHERE jobId = :jobId ORDER BY appliedAt DESC")
    List<JobApplicationEntity> getApplicationsForJob(long jobId);

    @Query("SELECT COUNT(*) FROM job_applications WHERE jobId = :jobId AND applicantRoll = :studentRoll")
    int checkApplicationExists(long jobId, String studentRoll);

    @Query("SELECT ja.* FROM job_applications ja INNER JOIN job_postings jp ON ja.jobId = jp.id WHERE jp.postedByUserId = :alumniId OR jp.postedByName = :alumniName OR :alumniId = 'ALL' ORDER BY ja.appliedAt DESC")
    List<JobApplicationEntity> getJobApplicationsForAlumni(String alumniId, String alumniName);

    @Query("DELETE FROM job_applications WHERE jobId = :jobId")
    void deleteApplicationsForJob(long jobId);

    @Query("SELECT * FROM job_applications WHERE id = :id LIMIT 1")
    JobApplicationEntity getApplicationById(long id);

    @Query("UPDATE job_applications SET status = :status WHERE id = :id")
    void updateJobApplicationStatus(long id, String status);

    @Update
    void updateJobApplication(JobApplicationEntity app);
}
