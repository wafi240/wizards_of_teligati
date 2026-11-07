package com.teligati.app.data;

import com.teligati.app.data.entity.*;
import com.teligati.app.model.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataMapper {

    // 1. JobPostingEntity <-> Opportunity
    public static Opportunity toOpportunity(JobPostingEntity e) {
        if (e == null) return null;
        OpportunityCategory cat;
        try {
            cat = OpportunityCategory.valueOf(e.getCategory());
        } catch (Exception ex) {
            cat = OpportunityCategory.ASSISTANT_WORK;
        }

        List<String> skills = new ArrayList<>();
        if (e.getSkillsRequired() != null && !e.getSkillsRequired().isEmpty()) {
            skills = Arrays.asList(e.getSkillsRequired().split("\\s*,\\s*"));
        }

        Opportunity opp = new Opportunity(
                e.getId(),
                e.getTitle(),
                cat,
                e.getOrganization(),
                e.getLocation(),
                e.getPostedByName(),
                e.getPosterRole(),
                e.getCompensation(),
                e.getDuration(),
                e.getDescription(),
                skills,
                e.getMinCgpa()
        );
        opp.setApplied(e.isApplied());
        opp.setPostedByUserId(e.getPostedByUserId());
        return opp;
    }

    public static List<Opportunity> toOpportunityList(List<JobPostingEntity> list) {
        List<Opportunity> result = new ArrayList<>();
        if (list != null) {
            for (JobPostingEntity e : list) {
                result.add(toOpportunity(e));
            }
        }
        return result;
    }

    // 2. ResearchPaperEntity <-> ProjectItem
    public static ProjectItem toProjectItem(ResearchPaperEntity e) {
        if (e == null) return null;
        List<String> techs = new ArrayList<>();
        if (e.getTechnologies() != null && !e.getTechnologies().isEmpty()) {
            techs = Arrays.asList(e.getTechnologies().split("\\s*,\\s*"));
        }

        return new ProjectItem(
                e.getId(),
                e.getTitle(),
                e.getType(),
                e.getDescription(),
                techs,
                e.getOwnerName(),
                e.getOwnerDept(),
                e.getContributorsCount(),
                e.isOpenForContribution(),
                e.getGithubUrl(),
                e.getPaperDoi()
        );
    }

    public static List<ProjectItem> toProjectItemList(List<ResearchPaperEntity> list) {
        List<ProjectItem> result = new ArrayList<>();
        if (list != null) {
            for (ResearchPaperEntity e : list) {
                result.add(toProjectItem(e));
            }
        }
        return result;
    }

    public static List<ProjectItem> toProjectList(List<ResearchPaperEntity> list) {
        return toProjectItemList(list);
    }

    // 3. AlumniProfileEntity <-> AlumniProfile
    public static AlumniProfile toAlumniProfile(AlumniProfileEntity e) {
        if (e == null) return null;
        List<String> skills = new ArrayList<>();
        if (e.getSkills() != null && !e.getSkills().isEmpty()) {
            skills = Arrays.asList(e.getSkills().split("\\s*,\\s*"));
        }

        return new AlumniProfile(
                e.getId(),
                e.getName(),
                e.getDepartment(),
                e.getBatch(),
                e.getCurrentCompany(),
                e.getCurrentPosition(),
                e.getCountry(),
                e.getCity(),
                e.isVerified(),
                e.isAvailableForMentorship(),
                e.isActivelyHiring(),
                skills,
                e.getBio()
        );
    }

    public static List<AlumniProfile> toAlumniProfileList(List<AlumniProfileEntity> list) {
        List<AlumniProfile> result = new ArrayList<>();
        if (list != null) {
            for (AlumniProfileEntity e : list) {
                result.add(toAlumniProfile(e));
            }
        }
        return result;
    }

    public static List<AlumniProfile> toAlumniList(List<AlumniProfileEntity> list) {
        return toAlumniProfileList(list);
    }

    public static AlumniProfile fromDirectoryResponse(com.teligati.app.network.model.AlumniDirectoryResponse r) {
        if (r == null) return null;
        List<String> skills = new ArrayList<>();
        if (r.getSkills() != null && !r.getSkills().isEmpty()) {
            skills = Arrays.asList(r.getSkills().split("\\s*,\\s*"));
        }
        return new AlumniProfile(
                r.getUserId() != null ? r.getUserId() : 0L,
                r.getName(),
                r.getDepartment(),
                r.getBatch(),
                r.getCurrentCompany(),
                r.getCurrentPosition(),
                r.getCountry(),
                r.getCity(),
                r.isVerified(),
                r.isAvailableForMentorship(),
                r.isActivelyHiring(),
                skills,
                r.getBio()
        );
    }

    public static List<AlumniProfile> fromDirectoryResponseList(List<com.teligati.app.network.model.AlumniDirectoryResponse> list) {
        List<AlumniProfile> result = new ArrayList<>();
        if (list != null) {
            for (com.teligati.app.network.model.AlumniDirectoryResponse r : list) {
                result.add(fromDirectoryResponse(r));
            }
        }
        return result;
    }

    // 4. ReferenceRequestEntity <-> ReferenceRequest
    public static ReferenceRequest toReferenceRequest(ReferenceRequestEntity e) {
        if (e == null) return null;
        return new ReferenceRequest(
                e.getId(),
                e.getStudentName(),
                e.getStudentRoll(),
                e.getStudentDept(),
                e.getTargetCompany(),
                e.getMessage(),
                e.getStatus(),
                e.getRecipientRole(),
                e.getRecipientName(),
                e.getPurpose(),
                e.getTargetProgram(),
                e.getDeadline(),
                e.getRecommendationText(),
                e.getResponseNotes()
        );
    }

    public static List<ReferenceRequest> toReferenceRequestList(List<ReferenceRequestEntity> list) {
        List<ReferenceRequest> result = new ArrayList<>();
        if (list != null) {
            for (ReferenceRequestEntity e : list) {
                result.add(toReferenceRequest(e));
            }
        }
        return result;
    }

    // 5. ResearchProposalEntity <-> ResearchProposal
    public static ResearchProposal toResearchProposal(ResearchProposalEntity e) {
        if (e == null) return null;
        return new ResearchProposal(
                e.getId(),
                e.getStudentName(),
                e.getStudentRoll(),
                e.getTopic(),
                e.getProposalAbstract(),
                e.getDegreeType(),
                e.getStatus(),
                e.getTeacherId(),
                "",
                e.getFeedback(),
                e.getSubmittedAt()
        );
    }

    public static List<ResearchProposal> toResearchProposalList(List<ResearchProposalEntity> list) {
        List<ResearchProposal> result = new ArrayList<>();
        if (list != null) {
            for (ResearchProposalEntity e : list) {
                result.add(toResearchProposal(e));
            }
        }
        return result;
    }

    public static List<ResearchProposal> toProposalList(List<ResearchProposalEntity> list) {
        return toResearchProposalList(list);
    }

    // 6. ResearchApplicationEntity <-> StudentApplicant
    public static StudentApplicant toStudentApplicant(ResearchApplicationEntity e) {
        if (e == null) return null;
        return new StudentApplicant(
                e.getId(),
                e.getStudentName(),
                e.getStudentRoll(),
                e.getDepartment(),
                e.getCgpa(),
                e.getSkills(),
                e.getLabInterest(),
                e.getStatus()
        );
    }

    public static List<StudentApplicant> toStudentApplicantList(List<ResearchApplicationEntity> list) {
        List<StudentApplicant> result = new ArrayList<>();
        if (list != null) {
            for (ResearchApplicationEntity e : list) {
                result.add(toStudentApplicant(e));
            }
        }
        return result;
    }

    // 7. TransactionEntity <-> TransactionRecord
    public static TransactionRecord toTransactionRecord(TransactionEntity e) {
        if (e == null) return null;
        return new TransactionRecord(
                e.getId(),
                e.getTitle(),
                e.getCategory(),
                e.getAmount(),
                e.getDate(),
                e.getStatus()
        );
    }

    public static List<TransactionRecord> toTransactionRecordList(List<TransactionEntity> list) {
        List<TransactionRecord> result = new ArrayList<>();
        if (list != null) {
            for (TransactionEntity e : list) {
                result.add(toTransactionRecord(e));
            }
        }
        return result;
    }

    // ==========================================
    // 8. Network DTO to Room Entity Mappings
    // ==========================================

    public static JobPostingEntity toJobEntity(com.teligati.app.network.model.JobNetworkDto dto) {
        if (dto == null) return null;
        return new JobPostingEntity(
                dto.getId(),
                dto.getTitle(),
                dto.getCategory(),
                dto.getOrganization(),
                dto.getLocation(),
                dto.getPostedByName(),
                dto.getPosterRole(),
                dto.getCompensation(),
                dto.getDuration(),
                dto.getDescription(),
                dto.getSkillsRequired(),
                dto.getMinCgpa(),
                dto.isApplied(),
                String.valueOf(dto.getPostedByUserId()),
                System.currentTimeMillis()
        );
    }

    public static ResearchPaperEntity toPaperEntity(com.teligati.app.network.model.ProjectNetworkDto dto) {
        if (dto == null) return null;
        return new ResearchPaperEntity(
                dto.getId(),
                dto.getName(),
                dto.getType(),
                dto.getDescription(),
                dto.getTechnologies(),
                dto.getOwnerName(),
                dto.getOwnerDept(),
                String.valueOf(dto.getOwnerId()),
                dto.getContributorsCount(),
                dto.isOpenForContribution(),
                dto.getGithubUrl(),
                dto.getPaperDoi(),
                System.currentTimeMillis()
        );
    }

    public static ResearchPaperEntity toPaperEntity(com.teligati.app.network.model.ResearchPaperNetworkDto dto) {
        if (dto == null) return null;
        return new ResearchPaperEntity(
                dto.getId(),
                dto.getTitle(),
                "Research Paper",
                dto.getAbstractText(),
                dto.getResearchField(),
                dto.getTeacherName() != null ? dto.getTeacherName() : dto.getAuthors(),
                dto.getTeacherDept() != null ? dto.getTeacherDept() : "Dept of CSE",
                String.valueOf(dto.getTeacherId()),
                1,
                true,
                dto.getReferenceUrl() != null ? dto.getReferenceUrl() : "https://kuet.ac.bd",
                dto.getDoi(),
                System.currentTimeMillis()
        );
    }

    public static List<ResearchPaperEntity> toPaperEntityListFromPapers(List<com.teligati.app.network.model.ResearchPaperNetworkDto> list) {
        List<ResearchPaperEntity> result = new ArrayList<>();
        if (list != null) {
            for (com.teligati.app.network.model.ResearchPaperNetworkDto dto : list) {
                result.add(toPaperEntity(dto));
            }
        }
        return result;
    }

    public static JobApplicationEntity toJobApplicationEntity(com.teligati.app.network.model.ApplicationNetworkDto dto) {
        if (dto == null) return null;
        return new JobApplicationEntity(
                dto.getId(),
                dto.getJobId(),
                dto.getJobTitle(),
                dto.getApplicantRoll(),
                dto.getApplicantName(),
                dto.getApplicantRoll(),
                dto.getApplicantDept(),
                dto.getApplicantCgpa(),
                dto.getApplicantSkills(),
                dto.getStatement(),
                dto.getStatus(),
                System.currentTimeMillis()
        );
    }

    public static ResearchApplicationEntity toResearchApplicationEntity(com.teligati.app.network.model.ApplicationNetworkDto dto) {
        if (dto == null) return null;
        return new ResearchApplicationEntity(
                dto.getId(),
                dto.getProjectId(),
                dto.getProjectTitle(),
                dto.getApplicantRoll(),
                dto.getApplicantName(),
                dto.getApplicantDept(),
                dto.getApplicantCgpa(),
                dto.getApplicantSkills(),
                dto.getStatement(),
                dto.getStatus(),
                System.currentTimeMillis()
        );
    }

    public static ResearchProposalEntity toProposalEntity(com.teligati.app.network.model.ResearchProposalNetworkDto dto) {
        if (dto == null) return null;
        long submittedTime = System.currentTimeMillis();
        if (dto.getCreatedAt() != null && !dto.getCreatedAt().trim().isEmpty()) {
            try {
                java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dto.getCreatedAt().trim());
                submittedTime = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (Exception ignored) {
                try {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                    java.util.Date d = sdf.parse(dto.getCreatedAt().trim());
                    if (d != null) submittedTime = d.getTime();
                } catch (Exception ignored2) {}
            }
        }
        return new ResearchProposalEntity(
                dto.getId(),
                dto.getStudentRoll(),
                dto.getStudentName(),
                dto.getTopic(),
                dto.getProposalAbstract(),
                dto.getDegreeType(),
                dto.getStatus(),
                String.valueOf(dto.getFacultyId()),
                dto.getFeedback(),
                submittedTime
        );
    }

    public static TransactionEntity toTransactionEntity(com.teligati.app.network.model.TransactionNetworkDto dto, String currentUserId) {
        if (dto == null) return null;
        String txId = dto.getTransactionId() != null ? dto.getTransactionId() : ("TX-" + dto.getId());
        String title = dto.getTitle() != null ? dto.getTitle() : "Demo Transaction";
        String cat = dto.getCategory() != null ? dto.getCategory() : "Demo";
        String date = dto.getTimestamp() != null ? dto.getTimestamp() : "Recent";
        String status = dto.getStatus() != null ? dto.getStatus() : "Completed";
        String uId = currentUserId != null ? currentUserId : (dto.getUserId() != null ? String.valueOf(dto.getUserId()) : "system");
        return new TransactionEntity(txId, title, cat, dto.getAmount(), date, status, uId);
    }

    public static List<TransactionEntity> toTransactionEntityList(List<com.teligati.app.network.model.TransactionNetworkDto> list, String currentUserId) {
        List<TransactionEntity> result = new ArrayList<>();
        if (list != null) {
            for (com.teligati.app.network.model.TransactionNetworkDto dto : list) {
                result.add(toTransactionEntity(dto, currentUserId));
            }
        }
        return result;
    }

    public static ReferenceRequestEntity toReferenceRequestEntity(com.teligati.app.network.model.ReferenceNetworkDto dto) {
        if (dto == null) return null;
        return new ReferenceRequestEntity(
                dto.getId() != null ? dto.getId() : 0L,
                dto.getStudentRoll(),
                dto.getStudentName(),
                dto.getStudentDept(),
                dto.getTargetInstitution(),
                dto.getPersonalMessage(),
                dto.getStatus(),
                dto.getRecipientId() != null ? String.valueOf(dto.getRecipientId()) : "",
                System.currentTimeMillis(),
                dto.getRecipientRole(),
                dto.getRecipientName(),
                dto.getPurpose(),
                dto.getTargetProgram(),
                dto.getDeadline(),
                dto.getRecommendationText(),
                dto.getResponseNotes()
        );
    }

    public static List<ReferenceRequestEntity> toReferenceRequestEntityList(List<com.teligati.app.network.model.ReferenceNetworkDto> list) {
        List<ReferenceRequestEntity> result = new ArrayList<>();
        if (list != null) {
            for (com.teligati.app.network.model.ReferenceNetworkDto dto : list) {
                result.add(toReferenceRequestEntity(dto));
            }
        }
        return result;
    }

    public static ConversationEntity toConversationEntity(com.teligati.app.network.model.ConversationNetworkDto dto) {
        if (dto == null) return null;
        return new ConversationEntity(
                dto.getId() != null ? dto.getId() : 0L,
                dto.getOtherParticipantId() != null ? dto.getOtherParticipantId() : 0L,
                dto.getOtherParticipantName() != null ? dto.getOtherParticipantName() : "User",
                dto.getOtherParticipantEmail(),
                dto.getOtherParticipantRole(),
                dto.getOtherParticipantDept(),
                dto.getLastMessageText(),
                System.currentTimeMillis(),
                dto.getUnreadCount()
        );
    }

    public static List<ConversationEntity> toConversationEntityList(List<com.teligati.app.network.model.ConversationNetworkDto> list) {
        List<ConversationEntity> result = new ArrayList<>();
        if (list != null) {
            for (com.teligati.app.network.model.ConversationNetworkDto dto : list) {
                result.add(toConversationEntity(dto));
            }
        }
        return result;
    }

    public static MessageEntity toMessageEntity(com.teligati.app.network.model.MessageNetworkDto dto) {
        if (dto == null) return null;
        return new MessageEntity(
                dto.getId() != null ? dto.getId() : 0L,
                dto.getConversationId() != null ? dto.getConversationId() : 0L,
                dto.getSenderId() != null ? String.valueOf(dto.getSenderId()) : "",
                dto.getSenderName() != null ? dto.getSenderName() : "User",
                dto.getRecipientId() != null ? String.valueOf(dto.getRecipientId()) : "",
                dto.getRecipientName() != null ? dto.getRecipientName() : "User",
                dto.getContent() != null ? dto.getContent() : "",
                System.currentTimeMillis(),
                "Just now",
                dto.isRead()
        );
    }

    public static List<MessageEntity> toMessageEntityList(List<com.teligati.app.network.model.MessageNetworkDto> list) {
        List<MessageEntity> result = new ArrayList<>();
        if (list != null) {
            for (com.teligati.app.network.model.MessageNetworkDto dto : list) {
                result.add(toMessageEntity(dto));
            }
        }
        return result;
    }
}
