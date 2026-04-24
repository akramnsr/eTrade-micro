package ma.portnet.demandservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "detail_traite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailTraite {

    @Id
    @Column(name = "draft_id", length = 36)
    private String draftId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private DemandAchatTraite demande;

    @Column(name = "draft_number", length = 100)
    private String draftNumber;

    @Column(name = "acceptor_country", length = 3)
    private String acceptorCountry;

    @Column(name = "acceptor_name", length = 200)
    private String acceptorName;

    @Column(name = "acceptor_bank", length = 200)
    private String acceptorBank;

    // Référence externe optionnelle vers auth-service
    @Column(name = "acceptor_bank_aba", length = 36)
    private String agreedBankAccord;

    @Column(name = "nominal_amount", precision = 18, scale = 2)
    private BigDecimal nominalAmount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "acceptance_date")
    private LocalDate acceptanceDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    // Nombre de jours jusqu'à l'échéance (calculé)
    public int getDaysUntilMaturity() {
        if (maturityDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS
                .between(java.time.LocalDate.now(), maturityDate);
    }

    // Nombre de jours entre acceptation et échéance
    public int calculateDaysToMaturity() {
        if (acceptanceDate == null || maturityDate == null) return 0;
        return (int) java.time.temporal.ChronoUnit.DAYS
                .between(acceptanceDate, maturityDate);
    }
}