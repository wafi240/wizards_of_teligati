package com.teligati.app.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.teligati.app.data.entity.JobPostingEntity;
import java.util.List;

@Dao
public interface JobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertJob(JobPostingEntity job);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertJobs(List<JobPostingEntity> jobs);

    @Query("SELECT * FROM job_postings ORDER BY createdAt DESC")
    List<JobPostingEntity> getAllJobs();

    @Query("SELECT * FROM job_postings WHERE category = :category ORDER BY createdAt DESC")
    List<JobPostingEntity> getJobsByCategory(String category);

    @Query("SELECT * FROM job_postings WHERE category IN (:categories) ORDER BY createdAt DESC")
    List<JobPostingEntity> getJobsByCategories(List<String> categories);

    @Query("SELECT * FROM job_postings WHERE postedByUserId = :posterId ORDER BY createdAt DESC")
    List<JobPostingEntity> getJobsByPoster(String posterId);

    @Query("SELECT * FROM job_postings WHERE id = :id LIMIT 1")
    JobPostingEntity getJobById(long id);

    @Query("UPDATE job_postings SET isApplied = :isApplied WHERE id = :id")
    void setJobApplied(long id, boolean isApplied);

    @Update
    void updateJob(JobPostingEntity job);

    @Query("DELETE FROM job_postings WHERE id = :id")
    void deleteJob(long id);
}
