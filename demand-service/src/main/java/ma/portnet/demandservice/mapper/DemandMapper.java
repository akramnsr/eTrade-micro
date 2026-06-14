package ma.portnet.demandservice.mapper;

import ma.portnet.demandservice.dto.response.DemandResponse;
import ma.portnet.demandservice.dto.response.DemandSummaryResponse;
import ma.portnet.demandservice.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DemandMapper {

    // ── DemandAchatTraite → DemandResponse (réponse complète) ─

    public DemandResponse toResponse(DemandAchatTraite d) {
        if (d == null) return null;

        return new DemandResponse(
                d.getDemandId(),
                d.getRequestNumber(),
                d.getStatus(),
                d.getExporterId(),
                d.getBankExporterId(),
                d.getDevise(),
                d.getMontantNominal(),
                d.getDateEcheance(),
                d.getDescription(),

                // ─── Nouveaux champs métier ───────────────────────
                d.getProductType() != null ? d.getProductType().name() : null,
                d.getBillContext(),
                d.getLcDcReference(),
                d.getPurchaseType(),

                d.getIsReadyForSubmission(),
                d.getCreatedDate(),
                d.getSubmittedDate(),
                d.getLastModifiedDate(),
                mapDetails(d.getDetails()),
                mapFinanciers(d.getDetailsFinanciers()),
                mapDecision(d.getDecision()),
                mapHistorique(d.getHistorique())
        );
    }

    // ── DemandAchatTraite → DemandSummaryResponse (liste) ────

    public DemandSummaryResponse toSummary(DemandAchatTraite d) {
        if (d == null) return null;

        return new DemandSummaryResponse(
                d.getDemandId(),
                d.getRequestNumber(),
                d.getStatus(),
                d.getExporterId(),
                d.getDevise(),
                d.getMontantNominal(),
                d.getDateEcheance(),
                d.getCreatedDate(),
                d.getSubmittedDate(),
                d.getDetails() != null ? d.getDetails().size() : 0
        );
    }

    public List<DemandSummaryResponse> toSummaryList(List<DemandAchatTraite> demands) {
        if (demands == null) return Collections.emptyList();
        return demands.stream().map(this::toSummary).toList();
    }

    // ── Helpers privés ────────────────────────────────────────

    private List<DemandResponse.DetailTraiteResponse> mapDetails(List<DetailTraite> details) {
        if (details == null) return Collections.emptyList();
        return details.stream()
                .map(dt -> new DemandResponse.DetailTraiteResponse(
                        dt.getDraftId(),
                        dt.getDraftNumber(),
                        dt.getAcceptorCountry(),
                        dt.getAcceptorName(),
                        dt.getAcceptorBank(),
                        dt.getNominalAmount(),
                        dt.getCurrencyCode(),
                        dt.getAcceptanceDate(),
                        dt.getMaturityDate(),
                        dt.getDaysUntilMaturity()
                ))
                .toList();
    }

    private DemandResponse.FinanciersResponse mapFinanciers(DetailsFinanciers f) {
        if (f == null) return null;
        return new DemandResponse.FinanciersResponse(
                f.getFinancialId(),
                f.getMontantNominal(),
                f.getMontantNet(),
                f.getAgios(),
                f.getFraisSwift(),
                f.getFraisCourrier(),
                f.getCommissionNegociation(),
                f.getTauxEscompte(),
                f.getDateFinancement()
        );
    }

    private DemandResponse.DecisionResponse mapDecision(Decision dec) {
        if (dec == null) return null;
        return new DemandResponse.DecisionResponse(
                dec.getDecisionId(),
                dec.getDecisionType(),
                dec.getDecisionDate(),
                dec.getDecisionBy(),
                dec.calculateOverallRisk(),
                dec.getBankRatingScore(),
                dec.getCountryRiskScore(),
                dec.getDocumentaryRiskScore(),
                dec.getConditions(),
                dec.getRecommendation()
        );
    }

    private List<DemandResponse.HistoriqueResponse> mapHistorique(List<HistoriqueStatuts> hist) {
        if (hist == null) return Collections.emptyList();
        return hist.stream()
                .map(h -> new DemandResponse.HistoriqueResponse(
                        h.getHistoryId(),
                        h.getFromStatus() != null ? h.getFromStatus().name() : null,
                        h.getToStatus().name(),
                        h.getChangedBy(),
                        h.getChangeDate(),
                        h.getReason(),
                        h.getComment()
                ))
                .toList();
    }
}