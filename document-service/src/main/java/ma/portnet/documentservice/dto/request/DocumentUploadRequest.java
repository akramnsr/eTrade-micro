package ma.portnet.document.dto.request;

import jakarta.validation.constraints.NotBlank;

public class DocumentUploadRequest {

    @NotBlank(message = "Le type de document est obligatoire")
    private String documentType;

    // Constructeurs
    public DocumentUploadRequest() {}

    public DocumentUploadRequest(String documentType) {
        this.documentType = documentType;
    }

    // Getter / Setter
    public String getDocumentType()              { return documentType; }
    public void setDocumentType(String t)        { this.documentType = t; }
}