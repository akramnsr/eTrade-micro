package ma.portnet.documentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.portnet.documentservice.dto.response.ApiResponse;
import ma.portnet.documentservice.dto.response.DocumentResponse;
import ma.portnet.documentservice.service.DocumentService;
import ma.portnet.documentservice.service.DocumentService.DocumentCheckResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Documents", description = "Gestion GED — upload, download, validation")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // ── Upload d'un fichier ───────────────────────────────────

    @PostMapping(value = "/{demandId}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader un document pour une demande",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @PathVariable String demandId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            Authentication auth
    ) {
        String uploadedBy = ((Jwt) auth.getPrincipal()).getSubject();
        DocumentResponse response = documentService.upload(demandId, file, documentType, uploadedBy);
        return ResponseEntity.ok(ApiResponse.ok("Document uploadé avec succès", response));
    }

    // ── Lister les documents d'une demande ────────────────────

    @GetMapping("/{demandId}")
    @Operation(summary = "Lister les documents d'une demande",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> list(
            @PathVariable String demandId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.listByDemand(demandId)));
    }

    // ── Télécharger un fichier ────────────────────────────────

    @GetMapping("/{demandId}/download/{documentId}")
    @Operation(summary = "Télécharger un document",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<byte[]> download(
            @PathVariable String demandId,
            @PathVariable String documentId
    ) {
        return documentService.download(documentId);
    }

    // ── URL présignée (lien temporaire) ───────────────────────

    @GetMapping("/{demandId}/url/{documentId}")
    @Operation(summary = "Obtenir une URL de téléchargement temporaire (1h)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<String>> getPresignedUrl(
            @PathVariable String demandId,
            @PathVariable String documentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getPresignedUrl(documentId)));
    }

    // ── Supprimer un document ─────────────────────────────────

    @DeleteMapping("/{demandId}/{documentId}")
    @Operation(summary = "Supprimer un document",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String demandId,
            @PathVariable String documentId,
            Authentication auth
    ) {
        String requestedBy = ((Jwt) auth.getPrincipal()).getSubject();
        documentService.delete(documentId, requestedBy);
        return ResponseEntity.ok(ApiResponse.ok("Document supprimé", null));
    }

    // ── Valider / Rejeter (banque) ────────────────────────────

    @PostMapping("/{demandId}/{documentId}/validate")
    @PreAuthorize("hasRole('BANQUE_EXPORTATEUR')")
    @Operation(summary = "Valider un document (banque)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DocumentResponse>> validate(
            @PathVariable String demandId,
            @PathVariable String documentId,
            Authentication auth
    ) {
        String validatedBy = ((Jwt) auth.getPrincipal()).getSubject();
        return ResponseEntity.ok(ApiResponse.ok(
                "Document validé", documentService.validate(documentId, validatedBy)
        ));
    }

    @PostMapping("/{demandId}/{documentId}/reject")
    @PreAuthorize("hasRole('BANQUE_EXPORTATEUR')")
    @Operation(summary = "Rejeter un document (banque)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DocumentResponse>> reject(
            @PathVariable String demandId,
            @PathVariable String documentId,
            @RequestBody java.util.Map<String, String> body,
            Authentication auth
    ) {
        String rejectedBy = ((Jwt) auth.getPrincipal()).getSubject();
        return ResponseEntity.ok(ApiResponse.ok(
                "Document rejeté",
                documentService.reject(documentId, rejectedBy, body.get("reason"))
        ));
    }

    // ── Vérification documents obligatoires ───────────────────
    // Appelé par demand-service avant d'autoriser le SUBMIT
    // URL publique — pas de token user nécessaire

    @GetMapping("/check/{demandId}")
    @Operation(summary = "Vérifier si tous les documents obligatoires sont présents")
    public ResponseEntity<Boolean> checkRequired(@PathVariable String demandId) {
        return ResponseEntity.ok(documentService.hasRequiredDocuments(demandId));
    }

    @GetMapping("/check/{demandId}/details")
    @Operation(summary = "Détail des documents présents et manquants",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<DocumentCheckResult>> checkDetails(
            @PathVariable String demandId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.checkDocuments(demandId)));
    }
}