// document-service/.../service/DocumentService.java
package ma.portnet.documentservice.service;

import lombok.extern.slf4j.Slf4j;
import ma.portnet.documentservice.client.DemandFeignClient;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DocumentService {

    private static final Set<TypeDocument> MANDATORY_TYPES = Set.of(
            TypeDocument.TRAITE_ACCEPTEE,
            TypeDocument.FACTURE_COMMERCIALE,
            TypeDocument.CONNAISSEMENT
    );

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/tiff"
    );

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final DocumentRepository  documentRepository;
    private final MinioStorageService storageService;
    private final DemandFeignClient   demandClient;

    public DocumentService(DocumentRepository documentRepository,
                           MinioStorageService storageService,
                           DemandFeignClient demandClient) {
        this.documentRepository = documentRepository;
        this.storageService     = storageService;
        this.demandClient       = demandClient;
    }

    // ── Upload (DS-05) ────────────────────────────────────────

    @Transactional
    public DocumentResponse upload(String demandId, MultipartFile file,
                                   String documentTypeStr, String uploadedBy) {
        TypeDocument docType = parseDocumentType(documentTypeStr);
        validateFile(file);

        // DS-05 etape 1 : verifier aupres de demand-service que la demande
        // existe ET est modifiable
        Boolean modifiable = demandClient.isModifiable(demandId);
        if (modifiable == null || !modifiable) {
            throw new InvalidFileException(
                    "La demande n'existe pas ou n'est plus modifiable (statut DRAFT requis)");
        }

        String extension = getExtension(file.getOriginalFilename());
        String fileName  = UUID.randomUUID() + "." + extension;
        String storagePath = storageService.store(file, demandId, docType.name(), fileName);

        // Remplacement : un seul doc par type par demande
        documentRepository.findByDemandIdAndDocumentType(demandId, docType)
                .ifPresent(existing -> {
                    if (existing.getStoragePath() != null) {
                        try { storageService.delete(existing.getStoragePath()); }
                        catch (Exception e) { log.warn("Ancien fichier non supprime : {}", e.getMessage()); }
                    }
                    documentRepository.delete(existing);
                });

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

        // DS-05 etape 2 : enregistrer la reference du document sur la demande
        // VERSION DIAGNOSTIQUE : logs explicites avant/apres + try/catch
        // Le throw final est essentiel : si la callback echoue, l'upload entier
        // doit etre annule (rollback @Transactional) pour rester coherent.
        Map<String, Object> payload = new HashMap<>();
        payload.put("documentId",   document.getDocumentId());
        payload.put("documentType", docType.name());
        payload.put("mandatory",    MANDATORY_TYPES.contains(docType));

        log.info("[DS-05] Appel Feign registerReference : demande={} doc={} type={} payload={}",
                demandId, document.getDocumentId(), docType.name(), payload);
        try {
            demandClient.registerReference(demandId, payload);
            log.info("[DS-05] Feign registerReference OK : demande={} doc={} type={}",
                    demandId, document.getDocumentId(), docType.name());
        } catch (Exception e) {
            log.error("[DS-05] Feign registerReference ECHEC : demande={} type={} exception={} message={}",
                    demandId, docType.name(), e.getClass().getName(), e.getMessage(), e);
            throw new RuntimeException(
                    "Impossible d'enregistrer la reference du document sur la demande : "
                            + e.getMessage(), e);
        }

        log.info("Document uploade complet : demandId={} type={} by={} size={}",
                demandId, docType, uploadedBy, file.getSize());
        return toResponse(document);
    }

    // ── Lister ────────────────────────────────────────────────

    public List<DocumentResponse> listByDemand(String demandId) {
        return documentRepository.findByDemandIdOrderByUploadedDateDesc(demandId)
                .stream().map(this::toResponse).toList();
    }

    // ── Telecharger ───────────────────────────────────────────

    public ResponseEntity<byte[]> download(String documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document introuvable : " + documentId));
        try (InputStream stream = storageService.download(document.getStoragePath())) {
            byte[] content = stream.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getOriginalName() + "\"")
                    .contentType(MediaType.parseMediaType(
                            document.getMimeType() != null ? document.getMimeType() : "application/octet-stream"))
                    .body(content);
        } catch (Exception e) {
            throw new ResourceNotFoundException("Impossible de telecharger le fichier");
        }
    }

    // ── URL presignee ─────────────────────────────────────────

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

        if (document.getStoragePath() != null) {
            storageService.delete(document.getStoragePath());
        }
        documentRepository.delete(document);

        // Retirer la reference cote demand-service (non bloquant)
        try {
            demandClient.removeReference(document.getDemandId(), document.getDocumentId());
        } catch (Exception e) {
            log.warn("Suppression de reference cote demand-service echouee : {}", e.getMessage());
        }

        log.info("Document supprime : {} par {}", documentId, requestedBy);
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

    // ── Verification documents obligatoires ──

    public boolean hasRequiredDocuments(String demandId) {
        long count = documentRepository.countValidMandatoryDocuments(demandId);
        log.debug("Verification docs obligatoires demandId={} : {}/3", demandId, count);
        return count >= MANDATORY_TYPES.size();
    }

    public DocumentCheckResult checkDocuments(String demandId) {
        List<Document> docs = documentRepository.findByDemandIdAndMandatoryFlagTrue(demandId);
        List<String> present = docs.stream()
                .filter(d -> d.getStatus() != DocumentStatus.REJECTED)
                .map(d -> d.getDocumentType().name()).toList();
        List<String> missing = MANDATORY_TYPES.stream().map(Enum::name)
                .filter(t -> !present.contains(t)).toList();
        return new DocumentCheckResult(missing.isEmpty(), present, missing);
    }

    public record DocumentCheckResult(boolean allPresent,
                                      List<String> presentTypes,
                                      List<String> missingTypes) {}

    // ── Helpers ───────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new InvalidFileException("Le fichier est vide");
        if (file.getSize() > MAX_FILE_SIZE) throw new InvalidFileException("Fichier trop grand (max 10 Mo)");
        if (file.getContentType() == null || !ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new InvalidFileException("Type de fichier non accepte. Formats : PDF, JPEG, PNG, TIFF");
        }
    }

    private TypeDocument parseDocumentType(String typeStr) {
        try { return TypeDocument.valueOf(typeStr.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new InvalidFileException("Type de document invalide : " + typeStr);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getDocumentId(), d.getDemandId(), d.getDocumentType(),
                d.getOriginalName(), d.getFileSize(), d.getMimeType(),
                d.getUploadedDate(), d.getUploadedBy(), d.getMandatoryFlag(),
                d.getStatus(), d.getRejectionReason());
    }
    // ── Lister tous les documents d'un utilisateur ────────────

    public List<DocumentResponse> listByUser(String uploadedBy) {
        return documentRepository.findByUploadedByOrderByUploadedDateDesc(uploadedBy)
                .stream().map(this::toResponse).toList();
    }
}