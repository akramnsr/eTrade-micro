package ma.portnet.documentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.portnet.documentservice.entity.enums.DocumentStatus;
import ma.portnet.documentservice.entity.enums.TypeDocument;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @Column(name = "document_id", length = 36)
    private String documentId;

    // Référence externe vers demand-service — PAS de @ManyToOne
    @Column(name = "demand_id", nullable = false, length = 36)
    private String demandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private TypeDocument documentType;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    // Chemin dans MinIO : bucket/demandId/documentType/filename
    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "uploaded_date", nullable = false)
    private LocalDateTime uploadedDate;

    // Référence externe vers auth-service
    @Column(name = "uploaded_by", nullable = false, length = 36)
    private String uploadedBy;

    @Column(name = "mandatory_flag", nullable = false)
    private Boolean mandatoryFlag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DocumentStatus status;

    @Column(name = "validated_by", length = 36)
    private String validatedBy;

    @Column(name = "validation_date")
    private LocalDateTime validationDate;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @PrePersist
    protected void onCreate() {
        if (this.uploadedDate == null) this.uploadedDate = LocalDateTime.now();
        if (this.status == null)       this.status       = DocumentStatus.UPLOADED;
        if (this.mandatoryFlag == null) this.mandatoryFlag = false;
    }
}