package com.teligati.app.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.teligati.app.data.entity.ReferenceRequestEntity;
import java.util.List;

@Dao
public interface ReferenceRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRequest(ReferenceRequestEntity request);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRequests(List<ReferenceRequestEntity> requests);

    @Query("SELECT * FROM reference_requests ORDER BY requestedAt DESC")
    List<ReferenceRequestEntity> getAllRequests();

    @Query("SELECT * FROM reference_requests WHERE studentRoll = :studentRoll ORDER BY requestedAt DESC")
    List<ReferenceRequestEntity> getRequestsByStudent(String studentRoll);

    @Query("SELECT * FROM reference_requests WHERE alumniId = :alumniId ORDER BY requestedAt DESC")
    List<ReferenceRequestEntity> getRequestsForAlumni(String alumniId);

    @Query("SELECT * FROM reference_requests WHERE alumniId = :alumniId OR alumniId = :alumniName OR :alumniId = 'ALL' ORDER BY requestedAt DESC")
    List<ReferenceRequestEntity> getRequestsForAlumni(String alumniId, String alumniName);

    @Query("SELECT * FROM reference_requests WHERE id = :id LIMIT 1")
    ReferenceRequestEntity getRequestById(long id);

    @Query("UPDATE reference_requests SET status = :status WHERE id = :id")
    void updateRequestStatus(long id, String status);

    @Update
    void updateRequest(ReferenceRequestEntity request);
}
