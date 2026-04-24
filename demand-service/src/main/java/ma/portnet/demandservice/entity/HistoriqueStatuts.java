package ma.portnet.demandservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.portnet.demandservice.entity.enums.StatusDemande;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_statuts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoriqueStatuts {

    @Id
    @Column(name = "history_id", length = 36)
    private String historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demand_id", nullable = false)
    private DemandAchatTraite demande;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private StatusDemande fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private StatusDemande toStatus;

    // Référence externe vers auth-service
    @Column(name = "changed_by", length = 36)
    private String changedBy;

    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @PrePersist
    protected void onCreate() {
        if (this.changeDate == null) this.changeDate = LocalDateTime.now();
    }

    public String getTransitionDescription() {
        String from = fromStatus != null ? fromStatus.name() : "INITIAL";
        return from + " → " + toStatus.name();
    }

    public Duration getDurationInStatus() {
        return Duration.between(changeDate, LocalDateTime.now());
    }
}