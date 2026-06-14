package ma.portnet.demandservice.dto.response;

import ma.portnet.demandservice.entity.enums.StatusDemande;
import ma.portnet.demandservice.entity.enums.TypeDecision;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DemandResponse(
        String        demandId,
        String        requestNumber,
        StatusDemande status,
        String        exporterId,
        String        bankExporterId,
        String        devise,
        BigDecimal    montantNominal,
        LocalDate     dateEcheance,
        String        description,

        // ─── Nouveaux champs ─────────────────
        String        productType,
        String        billContext,
        String        lcDcReference,
        String        purchaseType,

        Boolean       isReadyForSubmission,
        LocalDateTime createdDate,
        LocalDateTime submittedDate,
        LocalDateTime lastModifiedDate,

        List<DetailTraiteResponse> details,
        FinanciersResponse detailsFinanciers,
        DecisionResponse decision,
        List<HistoriqueResponse> historique

) {
    public record DetailTraiteResponse(
            String     draftId,
            String     draftNumber,
            String     acceptorCountry,
            String     acceptorName,
            String     acceptorBank,
            BigDecimal nominalAmount,
            String     currencyCode,
            LocalDate  acceptanceDate,
            LocalDate  maturityDate,
            int        daysUntilMaturity
    ) {}

    public record FinanciersResponse(
            String     financialId,
            BigDecimal montantNominal,
            BigDecimal montantNet,
            BigDecimal agios,
            BigDecimal fraisSwift,
            BigDecimal fraisCourrier,
            BigDecimal commissionNegociation,
            BigDecimal tauxEscompte,
            LocalDateTime dateFinancement
    ) {}

    public record DecisionResponse(
            String        decisionId,
            TypeDecision  decisionType,
            LocalDateTime decisionDate,
            String        decisionBy,
            Integer       overallRiskScore,
            Integer       bankRatingScore,
            Integer       countryRiskScore,
            Integer       documentaryRiskScore,
            String        conditions,
            String        recommendation
    ) {}

    public record HistoriqueResponse(
            String        historyId,
            String        fromStatus,
            String        toStatus,
            String        changedBy,
            LocalDateTime changeDate,
            String        reason,
            String        comment
    ) {}
}