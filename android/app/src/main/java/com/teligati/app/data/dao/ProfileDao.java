package com.teligati.app.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.teligati.app.data.entity.AlumniProfileEntity;
import com.teligati.app.data.entity.StudentProfileEntity;
import com.teligati.app.data.entity.TeacherProfileEntity;
import java.util.List;

@Dao
public interface ProfileDao {
    // Student Profiles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStudentProfile(StudentProfileEntity profile);

    @Query("SELECT * FROM student_profiles WHERE roll = :roll LIMIT 1")
    StudentProfileEntity getStudentProfile(String roll);

    @Update
    void updateStudentProfile(StudentProfileEntity profile);

    // Alumni Profiles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAlumniProfile(AlumniProfileEntity profile);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAlumniProfiles(List<AlumniProfileEntity> profiles);

    @Query("SELECT * FROM alumni_profiles ORDER BY id ASC")
    List<AlumniProfileEntity> getAllAlumniProfiles();

    @Query("SELECT * FROM alumni_profiles WHERE id = :id LIMIT 1")
    AlumniProfileEntity getAlumniProfileById(long id);

    @Query("SELECT * FROM alumni_profiles WHERE userId = :userId LIMIT 1")
    AlumniProfileEntity getAlumniProfileByUserId(String userId);

    @Update
    void updateAlumniProfile(AlumniProfileEntity profile);

    // Teacher Profiles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTeacherProfile(TeacherProfileEntity profile);

    @Query("SELECT * FROM teacher_profiles WHERE userId = :userId LIMIT 1")
    TeacherProfileEntity getTeacherProfileByUserId(String userId);

    @Update
    void updateTeacherProfile(TeacherProfileEntity profile);
}
