package ma.portnet.demandservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.portnet.demandservice.entity.enums.StatusDemande;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demande_achat_traite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandAchatTraite {

    @Id
    @Column(name = "demand_id", length = 36)
    private String demandId;

    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusDemande status;

    // Référence externe vers auth-service — PAS de @ManyToOne
    @Column(name = "exporter_id", nullable = false, length = 36)
    private String exporterId;

    // Référence externe vers auth-service
    @Column(name = "bank_exporter_id", length = 36)
    private String bankExporterId;

    @Column(name = "montant_nominal", precision = 18, scale = 2)
    private BigDecimal montantNominal;

    @Column(name = "devise", length = 3)
    private String devise;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_ready_for_submission")
    private Boolean isReadyForSubmission;

    // Relations internes au service
    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetailTraite> details = new ArrayList<>();

    @OneToOne(mappedBy = "demande", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private DetailsFinanciers detailsFinanciers;

    @OneToOne(mappedBy = "demande", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private Decision decision;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HistoriqueStatuts> historique = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null)           this.createdDate = LocalDateTime.now();
        if (this.status == null)                this.status = StatusDemande.DRAFT;
        if (this.isReadyForSubmission == null)  this.isReadyForSubmission = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedDate = LocalDateTime.now();
    }
}