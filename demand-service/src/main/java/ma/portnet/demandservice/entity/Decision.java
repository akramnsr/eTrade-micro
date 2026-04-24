package ma.portnet.demandservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.portnet.demandservice.entity.enums.CategorieRisquePays;
import ma.portnet.demandservice.entity.enums.TypeDecision;

import java.time.LocalDateTime;

@Entity
@Table(name = "decision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Decision {

    @Id
    @Column(name = "decision_id", length = 36)
    private String decisionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private DemandAchatTraite demande;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type")
    private TypeDecision decisionType;

    @Column(name = "decision_date")
    private LocalDateTime decisionDate;

    // Référence externe vers auth-service
    @Column(name = "decision_by", length = 36)
    private String decisionBy;

    // Scores de risque (0-100)
    @Column(name = "overall_risk_score")
    private Integer overallRiskScore;

    @Column(name = "bank_rating_score")
    private Integer bankRatingScore;

    @Column(name = "country_risk_score")
    private Integer countryRiskScore;

    @Column(name = "documentary_risk_score")
    private Integer documentaryRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "country_risk_category")
    private CategorieRisquePays countryRiskCategory;

    @Column(name = "conditions", columnDefinition = "TEXT")
    private String conditions;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    // Score global calculé
    public Integer calculateOverallRisk() {
        if (bankRatingScore == null && countryRiskScore == null
                && documentaryRiskScore == null) return null;

        int bank       = bankRatingScore       != null ? bankRatingScore       : 0;
        int country    = countryRiskScore      != null ? countryRiskScore      : 0;
        int documentary = documentaryRiskScore != null ? documentaryRiskScore  : 0;

        // Pondération : banque 40%, pays 35%, documentaire 25%
        return (int) Math.round(bank * 0.40 + country * 0.35 + documentary * 0.25);
    }
}