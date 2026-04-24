package ma.portnet.demandservice.service;

import ma.portnet.demandservice.entity.DetailsFinanciers;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class FinancialCalculationService {

    private static final BigDecimal FRAIS_SWIFT    = new BigDecimal("30");
    private static final BigDecimal FRAIS_COURRIER = new BigDecimal("100");
    private static final BigDecimal COMM_RATE      = new BigDecimal("0.005"); // 0.5%
    private static final BigDecimal DIVISEUR       = new BigDecimal("36000");

    /**
     * Calcule les agios : (Nominal × Taux × Jours) / 36000
     */
    public BigDecimal calculateAgios(BigDecimal nominal, BigDecimal tauxAnnuel, int jours) {
        return nominal
                .multiply(tauxAnnuel)
                .multiply(BigDecimal.valueOf(jours))
                .divide(DIVISEUR, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule la commission de négociation : Nominal × 0.005
     */
    public BigDecimal calculateCommission(BigDecimal nominal) {
        return nominal.multiply(COMM_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcule le montant net à verser
     */
    public BigDecimal calculateMontantNet(BigDecimal nominal,
                                          BigDecimal tauxAnnuel,
                                          int jours) {
        BigDecimal agios      = calculateAgios(nominal, tauxAnnuel, jours);
        BigDecimal commission = calculateCommission(nominal);
        return nominal
                .subtract(agios)
                .subtract(commission)
                .subtract(FRAIS_SWIFT)
                .subtract(FRAIS_COURRIER)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Remplit et retourne un objet DetailsFinanciers complet
     */
    public DetailsFinanciers buildDetailsFinanciers(BigDecimal nominal,
                                                    BigDecimal tauxAnnuel,
                                                    int jours) {
        DetailsFinanciers details = new DetailsFinanciers();
        details.setMontantNominal(nominal);
        details.setTauxEscompte(tauxAnnuel);
        details.setAgios(calculateAgios(nominal, tauxAnnuel, jours));
        details.setCommissionNegociation(calculateCommission(nominal));
        details.setFraisSwift(FRAIS_SWIFT);
        details.setFraisCourrier(FRAIS_COURRIER);
        details.setMontantNet(calculateMontantNet(nominal, tauxAnnuel, jours));
        return details;
    }

    /**
     * Retourne un breakdown détaillé pour affichage frontend
     */
    public Map<String, BigDecimal> breakdown(BigDecimal nominal,
                                             BigDecimal tauxAnnuel,
                                             int jours) {
        BigDecimal agios      = calculateAgios(nominal, tauxAnnuel, jours);
        BigDecimal commission = calculateCommission(nominal);
        BigDecimal net        = nominal.subtract(agios).subtract(commission)
                .subtract(FRAIS_SWIFT).subtract(FRAIS_COURRIER);
        return Map.of(
                "montantNominal",      nominal,
                "agios",               agios,
                "commissionNegociation", commission,
                "fraisSwift",          FRAIS_SWIFT,
                "fraisCourrier",       FRAIS_COURRIER,
                "montantNet",          net.setScale(2, RoundingMode.HALF_UP)
        );
    }
}