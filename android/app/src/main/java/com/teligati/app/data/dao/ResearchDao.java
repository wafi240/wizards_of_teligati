package com.teligati.app.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.teligati.app.data.entity.ResearchApplicationEntity;
import com.teligati.app.data.entity.ResearchPaperEntity;
import com.teligati.app.data.entity.ResearchProposalEntity;
import java.util.List;

@Dao
public interface ResearchDao {
    // Research Papers
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPaper(ResearchPaperEntity paper);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPapers(List<ResearchPaperEntity> papers);

    @Query("SELECT * FROM research_papers ORDER BY createdAt DESC")
    List<ResearchPaperEntity> getAllPapers();

    @Query("SELECT * FROM research_papers WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    List<ResearchPaperEntity> getPapersByOwner(String ownerId);

    @Query("SELECT * FROM research_papers WHERE ownerId = :ownerId OR ownerName = :ownerName ORDER BY createdAt DESC")
    List<ResearchPaperEntity> getPapersForTeacher(String ownerId, String ownerName);

    @Query("DELETE FROM research_applications WHERE opportunityId = :oppId")
    void deleteApplicationsForOpportunity(long oppId);

    @Query("SELECT * FROM research_papers WHERE id = :id LIMIT 1")
    ResearchPaperEntity getPaperById(long id);

    @Update
    void updatePaper(ResearchPaperEntity paper);

    @Query("DELETE FROM research_papers WHERE id = :id")
    void deletePaper(long id);

    // Research Proposals
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertProposal(ResearchProposalEntity proposal);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProposals(List<ResearchProposalEntity> proposals);

    @Query("SELECT * FROM research_proposals ORDER BY submittedAt DESC")
    List<ResearchProposalEntity> getAllProposals();

    @Query("SELECT * FROM research_proposals WHERE studentRoll = :studentRoll ORDER BY submittedAt DESC")
    List<ResearchProposalEntity> getProposalsByStudent(String studentRoll);

    @Query("SELECT * FROM research_proposals WHERE studentRoll = :studentRoll OR studentRoll = :studentEmail OR :studentRoll = 'ALL' ORDER BY submittedAt DESC")
    List<ResearchProposalEntity> getProposalsByStudent(String studentRoll, String studentEmail);

    @Query("SELECT * FROM research_proposals WHERE teacherId = :teacherId OR teacherId = :teacherEmail OR :teacherId = 'ALL' ORDER BY submittedAt DESC")
    List<ResearchProposalEntity> getProposalsForTeacher(String teacherId, String teacherEmail);

    @Query("SELECT * FROM research_proposals WHERE teacherId = :teacherId OR teacherId = :teacherEmail OR teacherId = :backendId OR :teacherId = 'ALL' ORDER BY submittedAt DESC")
    List<ResearchProposalEntity> getProposalsForTeacher(String teacherId, String teacherEmail, String backendId);

    @Query("SELECT * FROM research_proposals WHERE id = :id LIMIT 1")
    ResearchProposalEntity getProposalById(long id);

    @Query("UPDATE research_proposals SET status = :status, feedback = :feedback WHERE id = :id")
    void updateProposalStatus(long id, String status, String feedback);

    // Research Applications (Applicants for lab/research)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertResearchApplication(ResearchApplicationEntity app);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertResearchApplications(List<ResearchApplicationEntity> apps);

    @Query("SELECT * FROM research_applications ORDER BY appliedAt DESC")
    List<ResearchApplicationEntity> getAllResearchApplications();

    @Query("SELECT * FROM research_applications WHERE studentRoll = :studentRoll ORDER BY appliedAt DESC")
    List<ResearchApplicationEntity> getResearchApplicationsByStudent(String studentRoll);

    @Query("SELECT ra.* FROM research_applications ra INNER JOIN research_papers rp ON ra.opportunityId = rp.id WHERE rp.ownerId = :teacherId OR rp.ownerName = :teacherName OR :teacherId = 'ALL' ORDER BY ra.appliedAt DESC")
    List<ResearchApplicationEntity> getResearchApplicationsForTeacher(String teacherId, String teacherName);

    @Query("SELECT COUNT(*) FROM research_applications WHERE opportunityId = :oppId AND studentRoll = :studentRoll")
    int checkResearchApplicationExists(long oppId, String studentRoll);

    @Query("UPDATE research_applications SET status = :status WHERE id = :id")
    void updateResearchApplicationStatus(long id, String status);
}
