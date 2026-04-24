package ma.portnet.documentservice.service;

import lombok.extern.slf4j.Slf4j;
import ma.portnet.documentservice.dto.response.DocumentResponse;
import ma.portnet.documentservice.entity.Document;
import ma.portnet.documentservice.entity.enums.DocumentStatus;
import ma.portnet.documentservice.entity.enums.TypeDocument;
import ma.portnet.documentservice.exception.InvalidFileException;
import ma.portnet.documentservice.exception.ResourceNotFoundException;
import ma.portnet.documentservice.repository.DocumentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {

    // Les 3 types obligatoires pour pouvoir soumettre une demande EBP
    private static final Set<TypeDocument> MANDATORY_TYPES = Set.of(
            TypeDocument.TRAITE_ACCEPTEE,
            TypeDocument.FACTURE_COMMERCIALE,
            TypeDocument.CONNAISSEMENT
    );

    // Types MIME acceptés
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/tiff"
    );

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 Mo

    private final DocumentRepository documentRepository;
    private final MinioStorageService storageService;

    public DocumentService(DocumentRepository documentRepository,
                           MinioStorageService storageService) {
        this.documentRepository = documentRepository;
        this.storageService     = storageService;
    }

    // ── Upload ────────────────────────────────────────────────

    @Transactional
    public DocumentResponse upload(String demandId,
                                   MultipartFile file,
                                   String documentTypeStr,
                                   String uploadedBy) {
        // Valider le type de document
        TypeDocument docType = parseDocumentType(documentTypeStr);

        // Valider le fichier
        validateFile(file);

        // Générer un nom unique pour éviter les conflits
        String extension = getExtension(file.getOriginalFilename());
        String fileName  = UUID.randomUUID() + "." + extension;

        // Stocker dans MinIO
        String storagePath = storageService.store(file, demandId, docType.name(), fileName);

        // Si un document du même type existe déjà pour cette demande,
        // le remplacer (un seul doc par type par demande)
        documentRepository.findByDemandIdAndDocumentType(demandId, docType)
                .ifPresent(existing -> {
                    // Supprimer l'ancien fichier MinIO
                    if (existing.getStoragePath() != null) {
                        try { storageService.delete(existing.getStoragePath()); }
                        catch (Exception e) { log.warn("Ancien fichier non supprimé : {}", e.getMessage()); }
                    }
                    documentRepository.delete(existing);
                });

        // Persister en base
        Document document = Document.builder()
                .documentId(UUID.randomUUID().toString())
                .demandId(demandId)
                .documentType(docType)
                .fileName(fileName)
                .originalName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .storagePath(storagePath)
                .uploadedDate(LocalDateTime.now())
                .uploadedBy(uploadedBy)
                .mandatoryFlag(MANDATORY_TYPES.contains(docType))
                .status(DocumentStatus.UPLOADED)
                .build();

        documentRepository.save(document);
        log.info("Document uploadé : demandId={} type={} by={}", demandId, docType, uploadedBy);

        return toResponse(document);
    }

    // ── Lister ────────────────────────────────────────────────

    public List<DocumentResponse> listByDemand(String demandId) {
        return documentRepository
                .findByDemandIdOrderByUploadedDateDesc(demandId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Télécharger ───────────────────────────────────────────

    public ResponseEntity<byte[]> download(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + documentId));

        try (InputStream stream = storageService.download(document.getStoragePath())) {
            byte[] content = stream.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(
                            document.getMimeType() != null ? document.getMimeType() : "application/octet-stream"
                    ))
                    .body(content);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Impossible de télécharger le fichier");
        }
    }

    // ── URL présignée ─────────────────────────────────────────

    public String getPresignedUrl(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + documentId));
        return storageService.generatePresignedUrl(document.getStoragePath());
    }

    // ── Supprimer ─────────────────────────────────────────────

    @Transactional
    public void delete(String documentId, String requestedBy) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + documentId));

        // Supprimer de MinIO
        if (document.getStoragePath() != null) {
            storageService.delete(document.getStoragePath());
        }

        documentRepository.delete(document);
        log.info("Document supprimé : {} par {}", documentId, requestedBy);
    }

    // ── Validation banque ─────────────────────────────────────

    @Transactional
    public DocumentResponse validate(String documentId, String validatedBy) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + documentId));

        document.setStatus(DocumentStatus.VALIDATED);
        document.setValidatedBy(validatedBy);
        document.setValidationDate(LocalDateTime.now());
        documentRepository.save(document);

        return toResponse(document);
    }

    @Transactional
    public DocumentResponse reject(String documentId, String rejectedBy, String reason) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + documentId));

        document.setStatus(DocumentStatus.REJECTED);
        document.setValidatedBy(rejectedBy);
        document.setValidationDate(LocalDateTime.now());
        document.setRejectionReason(reason);
        documentRepository.save(document);

        return toResponse(document);
    }

    // ── Vérification documents obligatoires ───────────────────
    // Endpoint appelé par demand-service avant d'autoriser le SUBMIT

    public boolean hasRequiredDocuments(String demandId) {
        long count = documentRepository.countValidMandatoryDocuments(demandId);
        boolean hasAll = count >= MANDATORY_TYPES.size();

        log.debug("Vérification documents obligatoires demandId={} : {}/{} → {}",
                demandId, count, MANDATORY_TYPES.size(), hasAll ? "OK" : "INCOMPLET");

        return hasAll;
    }

    public DocumentCheckResult checkDocuments(String demandId) {
        List<Document> docs = documentRepository.findByDemandIdAndMandatoryFlagTrue(demandId);

        List<String> present = docs.stream()
                .filter(d -> d.getStatus() != DocumentStatus.REJECTED)
                .map(d -> d.getDocumentType().name())
                .toList();

        List<String> missing = MANDATORY_TYPES.stream()
                .map(Enum::name)
                .filter(t -> !present.contains(t))
                .toList();

        return new DocumentCheckResult(missing.isEmpty(), present, missing);
    }

    public record DocumentCheckResult(
            boolean allPresent,
            List<String> presentTypes,
            List<String> missingTypes
    ) {}

    // ── Helpers privés ────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Le fichier est vide");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("Fichier trop grand (max 10 Mo)");
        }
        if (file.getContentType() == null || !ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException(
                    "Type de fichier non accepté. Formats acceptés : PDF, JPEG, PNG, TIFF"
            );
        }
    }

    private TypeDocument parseDocumentType(String typeStr) {
        try {
            return TypeDocument.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidFileException("Type de document invalide : " + typeStr);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getDocumentId(),
                d.getDemandId(),
                d.getDocumentType(),
                d.getOriginalName(),
                d.getFileSize(),
                d.getMimeType(),
                d.getUploadedDate(),
                d.getUploadedBy(),
                d.getMandatoryFlag(),
                d.getStatus(),
                d.getRejectionReason()
        );
    }
}