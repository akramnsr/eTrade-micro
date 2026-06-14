// demand-service/.../entity/DocumentReference.java
package ma.portnet.demandservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_reference")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentReference {

    @Id
    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    private DemandAchatTraite demande;

    // Référence externe vers document-service — PAS de @ManyToOne
    @Column(name = "document_id", nullable = false, length = 36)
    private String documentId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory;

    @Column(name = "registered_date", nullable = false)
    private LocalDateTime registeredDate;

    @PrePersist
    protected void onCreate() {
        if (this.registeredDate == null) this.registeredDate = LocalDateTime.now();
        if (this.mandatory == null)      this.mandatory = false;
    }
}