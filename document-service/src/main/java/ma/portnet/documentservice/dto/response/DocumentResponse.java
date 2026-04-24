package ma.portnet.documentservice.dto.response;

import ma.portnet.documentservice.entity.enums.DocumentStatus;
import ma.portnet.documentservice.entity.enums.TypeDocument;

import java.time.LocalDateTime;

public record DocumentResponse(
        String documentId,
        String demandId,
        TypeDocument documentType,
        String originalName,
        Long fileSize,
        String mimeType,
        LocalDateTime uploadedDate,
        String uploadedBy,
        boolean mandatoryFlag,
        DocumentStatus status,
        String rejectionReason
) {}