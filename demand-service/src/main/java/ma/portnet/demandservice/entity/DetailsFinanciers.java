package ma.portnet.demandservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "details_financiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailsFinanciers {

    @Id
    @Column(name = "financial_id", length = 36)
    private String financialId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private DemandAchatTraite demande;

    @Column(name = "montant_nominal", precision = 18, scale = 2)
    private BigDecimal montantNominal;

    @Column(name = "montant_net", precision = 18, scale = 2)
    private BigDecimal montantNet;

    @Column(name = "agios", precision = 18, scale = 2)
    private BigDecimal agios;

    @Column(name = "frais_swift", precision = 18, scale = 2)
    private BigDecimal fraisSwift;

    @Column(name = "frais_courrier", precision = 18, scale = 2)
    private BigDecimal fraisCourrier;

    @Column(name = "commission_negociation", precision = 18, scale = 2)
    private BigDecimal commissionNegociation;

    @Column(name = "taux_escompte", precision = 8, scale = 4)
    private BigDecimal tauxEscompte;

    @Column(name = "date_financement")
    private LocalDateTime dateFinancement;
}