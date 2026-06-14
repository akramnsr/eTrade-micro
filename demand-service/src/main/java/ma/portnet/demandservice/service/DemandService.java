// demand-service/.../service/DemandService.java
package ma.portnet.demandservice.service;

import lombok.extern.slf4j.Slf4j;
import ma.portnet.demandservice.dto.request.CreateDemandRequest;
import ma.portnet.demandservice.dto.request.UpdateDemandRequest;
import ma.portnet.demandservice.dto.response.DemandResponse;
import ma.portnet.demandservice.dto.response.DemandSummaryResponse;
import ma.portnet.demandservice.entity.*;
import ma.portnet.demandservice.entity.enums.ProductType;
import ma.portnet.demandservice.entity.enums.StatusDemande;
import ma.portnet.demandservice.exception.InvalidStateTransitionException;
import ma.portnet.demandservice.exception.ResourceNotFoundException;
import ma.portnet.demandservice.mapper.DemandMapper;
import ma.portnet.demandservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DemandService {

    // Nombre de documents OBLIGATOIRES requis pour soumettre (TRAITE, FACTURE, B/L)
    private static final int REQUIRED_MANDATORY_DOCS = 3;

    // ⚠️ Source de vérité : demand-service décide ce qui est obligatoire,
    // pas document-service. Le flag `mandatory` envoyé par Feign est ignoré.
    private static final Set<String> MANDATORY_DOCUMENT_TYPES = Set.of(
            "TRAITE_ACCEPTEE",
            "FACTURE_COMMERCIALE",
            "CONNAISSEMENT"
    );

    private final DecisionRepository          decisionRepository;
    private final DemandRepository            demandRepository;
    private final DetailTraiteRepository      detailTraiteRepository;
    private final DetailsFinanciersRepository financiersRepository;
    private final HistoriqueStatutsRepository historiqueRepository;
    private final DocumentReferenceRepository docRefRepository;
    private final StateMachineService         stateMachineService;
    private final FinancialCalculationService calculationService;
    private final DemandMapper                mapper;

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    public DemandService(
            DecisionRepository decisionRepository,
            DemandRepository            demandRepository,
            DetailTraiteRepository      detailTraiteRepository,
            DetailsFinanciersRepository financiersRepository,
            HistoriqueStatutsRepository historiqueRepository,
            DocumentReferenceRepository docRefRepository,
            StateMachineService         stateMachineService,
            FinancialCalculationService calculationService,
            DemandMapper                mapper
    ) {
        this.decisionRepository    = decisionRepository;
        this.demandRepository      = demandRepository;
        this.detailTraiteRepository = detailTraiteRepository;
        this.financiersRepository  = financiersRepository;
        this.historiqueRepository  = historiqueRepository;
        this.docRefRepository      = docRefRepository;
        this.stateMachineService   = stateMachineService;
        this.calculationService    = calculationService;
        this.mapper                = mapper;
    }

    // ── CREATE ────────────────────────────────────────────────

    @Transactional
    public DemandResponse createDemand(CreateDemandRequest request, String exporterId, String exporterEmail) {
        String demandId      = UUID.randomUUID().toString();
        String requestNumber = generateRequestNumber();

        DemandAchatTraite demand = DemandAchatTraite.builder()
                .demandId(demandId)
                .requestNumber(requestNumber)
                .status(StatusDemande.DRAFT)
                .exporterId(exporterId)
                .exporterEmail(exporterEmail)
                .devise(request.devise())
                .montantNominal(request.montantNominal())
                .dateEcheance(request.dateEcheance())
                .description(request.description())
                .productType(request.productType() != null         // ← AJOUT
                        ? request.productType()
                        : ProductType.EXPORT_BILL_PURCHASE)
                .billContext(request.billContext())           // ← AJOUT
                .lcDcReference(request.lcDcReference())       // ← AJOUT
                .purchaseType(request.purchaseType()) //   fallback rétro-compat
                .isReadyForSubmission(false)
                .createdDate(LocalDateTime.now())
                .build();

        demandRepository.save(demand);

        if (request.details() != null) {
            for (CreateDemandRequest.DetailTraiteRequest dtReq : request.details()) {
                DetailTraite dt = DetailTraite.builder()
                        .draftId(UUID.randomUUID().toString())
                        .demande(demand)
                        .draftNumber(dtReq.draftNumber())
                        .acceptorCountry(dtReq.acceptorCountry())
                        .acceptorName(dtReq.acceptorName())
                        .acceptorBank(dtReq.acceptorBank())
                        .nominalAmount(dtReq.nominalAmount())
                        .currencyCode(dtReq.currencyCode())
                        .acceptanceDate(dtReq.acceptanceDate())
                        .maturityDate(dtReq.maturityDate())
                        .build();
                detailTraiteRepository.save(dt);
                demand.getDetails().add(dt);
            }
        }

        stateMachineService.logInitialCreation(demand, exporterId);
        log.info("Demande créée : {} par exporteur {}", requestNumber, exporterId);
        return mapper.toResponse(demand);
    }

    // ── READ ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DemandResponse getDemandById(String demandId) {
        return mapper.toResponse(findDemandOrThrow(demandId));
    }

    @Transactional(readOnly = true)
    public List<DemandSummaryResponse> listDemands(
            String userId, List<String> roles, String statusFilter
    ) {
        List<DemandAchatTraite> demands;

        if (roles.contains("EXPORTATEUR")) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                demands = demandRepository
                        .findByExporterIdAndStatusOrderByCreatedDateDesc(userId, parseStatus(statusFilter));
            } else {
                demands = demandRepository.findByExporterIdOrderByCreatedDateDesc(userId);
            }
        } else if (roles.contains("BANQUE_EXPORTATEUR")) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                demands = demandRepository
                        .findByStatusOrderByCreatedDateDesc(parseStatus(statusFilter));
            } else {
                demands = demandRepository.findAllVisibleToBank();
            }
        } else if (roles.contains("BANQUE_IMPORTATEUR")) {
            demands = demandRepository
                    .findByStatusOrderByCreatedDateDesc(StatusDemande.PRESENTATION_AT_MATURITY);
        } else {
            demands = statusFilter != null && !statusFilter.isBlank()
                    ? demandRepository.findByStatusOrderByCreatedDateDesc(parseStatus(statusFilter))
                    : demandRepository.findAll();
        }

        return mapper.toSummaryList(demands);
    }

    // ── UPDATE ────────────────────────────────────────────────

    @Transactional
    public DemandResponse updateDemand(
            String demandId, UpdateDemandRequest request, String exporterId
    ) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        if (!demand.getExporterId().equals(exporterId)) {
            throw new InvalidStateTransitionException(
                    "Vous n'êtes pas autorisé à modifier cette demande");
        }
        if (demand.getStatus() != StatusDemande.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Impossible de modifier une demande avec le statut : " + demand.getStatus());
        }

        if (request.devise()         != null) demand.setDevise(request.devise());
        if (request.montantNominal() != null) demand.setMontantNominal(request.montantNominal());
        if (request.dateEcheance()   != null) demand.setDateEcheance(request.dateEcheance());
        if (request.description()    != null) demand.setDescription(request.description());

        demandRepository.save(demand);
        log.info("Demande mise à jour : {}", demandId);
        return mapper.toResponse(demand);
    }

    // ── DELETE ────────────────────────────────────────────────

    // ── DELETE ────────────────────────────────────────────────

    @Transactional
    public void deleteDemand(String demandId, String exporterId, List<String> roles) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);
        boolean isAdmin = roles != null && roles.contains("ADMINISTRATEUR");

        if (!isAdmin) {
            // 1. Vérifier que l'utilisateur est bien propriétaire
            if (!demand.getExporterId().equals(exporterId)) {
                throw new InvalidStateTransitionException(
                        "Vous n'êtes pas autorisé à supprimer cette demande");
            }
            // 2. Vérifier que le statut est DRAFT
            if (demand.getStatus() != StatusDemande.DRAFT) {
                throw new InvalidStateTransitionException(
                        "Seules les demandes en brouillon peuvent être supprimées.");
            }
        }

        // Nettoyage manuel des entités liées (FK constraints)
        try {
            docRefRepository.deleteAll(
                    docRefRepository.findAll().stream()
                            .filter(r -> r.getDemande() != null
                                    && demandId.equals(r.getDemande().getDemandId()))
                            .toList()
            );
        } catch (Exception e) { log.warn("Suppression refs docs : {}", e.getMessage()); }

        try {
            decisionRepository.findByDemande_DemandId(demandId)
                    .ifPresent(decisionRepository::delete);
        } catch (Exception e) { log.warn("Suppression décision : {}", e.getMessage()); }

        if (demand.getDetailsFinanciers() != null) {
            financiersRepository.delete(demand.getDetailsFinanciers());
            demand.setDetailsFinanciers(null);
        }

        demandRepository.delete(demand);
        log.info("Demande supprimée : {} (admin={})", demandId, isAdmin);
    }
    @Transactional
    public DemandResponse forceStatus(String demandId, String adminUserId, String newStatus, String reason) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);
        StatusDemande target   = parseStatus(newStatus);
        StatusDemande previous = demand.getStatus();
        demand.setStatus(target);

        stateMachineService.logForcedTransition(
                demand, previous, target, adminUserId,
                "Intervention admin : " + (reason != null ? reason : "non spécifiée"));

        demandRepository.save(demand);
        log.warn("Statut forcé par admin {} : {} {} → {}", adminUserId, demandId, previous, target);
        return mapper.toResponse(demand);
    }

    // ── RÉFÉRENCES DOCUMENTS (appelé par document-service — DS-05) ──

    /** DS-05 : la demande est modifiable tant qu'elle est en DRAFT. */
    @Transactional(readOnly = true)
    public boolean isModifiable(String demandId) {
        return demandRepository.findById(demandId)
                .map(d -> d.getStatus() == StatusDemande.DRAFT)
                .orElse(false);
    }

    /**
     * DS-05 : enregistre la référence du document sur la demande (upsert par type).
     *
     * ⚠️ Le paramètre `mandatory` reçu de document-service est ignoré.
     * La règle métier "ce document est-il obligatoire ?" appartient à demand-service
     * et est calculée à partir du type de document via MANDATORY_DOCUMENT_TYPES.
     * Cela garantit que /submit ne dépend pas de ce que document-service a envoyé.
     */
    @Transactional
    public void registerDocumentReference(String demandId, String documentId,
                                          String documentType, boolean mandatory) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        // Source de vérité côté demand-service (on ignore le flag entrant)
        boolean isMandatory = MANDATORY_DOCUMENT_TYPES.contains(documentType);

        docRefRepository.findByDemande_DemandIdAndDocumentType(demandId, documentType)
                .ifPresent(docRefRepository::delete);

        DocumentReference ref = DocumentReference.builder()
                .referenceId(UUID.randomUUID().toString())
                .demande(demand)
                .documentId(documentId)
                .documentType(documentType)
                .mandatory(isMandatory)
                .registeredDate(LocalDateTime.now())
                .build();

        docRefRepository.save(ref);
        log.info("Référence doc enregistrée : demande={} type={} mandatory={} (flag entrant ignoré={})",
                demandId, documentType, isMandatory, mandatory);
    }

    @Transactional
    public void removeDocumentReference(String demandId, String documentId) {
        docRefRepository.deleteByDemande_DemandIdAndDocumentId(demandId, documentId);
        log.info("Référence doc supprimée : demande={} doc={}", demandId, documentId);
    }

    // ── TRANSITIONS DE STATUT ─────────────────────────────────

    @Transactional
    public DemandResponse submitDemand(String demandId, String exporterId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        if (!demand.getExporterId().equals(exporterId)) {
            throw new InvalidStateTransitionException(
                    "Seul l'exportateur propriétaire peut soumettre cette demande");
        }

        // DS-06 : vérification EN LOCAL des documents obligatoires
        // (les références ont été enregistrées par document-service à l'upload — DS-05)
        long mandatoryCount = docRefRepository.countByDemande_DemandIdAndMandatoryTrue(demandId);
        if (mandatoryCount < REQUIRED_MANDATORY_DOCS) {
            throw new InvalidStateTransitionException(
                    "Documents obligatoires manquants : traite acceptée, " +
                            "facture commerciale et connaissement requis");
        }

        demand.setSubmittedDate(LocalDateTime.now());
        demand.setIsReadyForSubmission(true);

        stateMachineService.transition(
                demand, StatusDemande.SUBMITTED, exporterId,
                "Demande soumise par l'exportateur");

        demandRepository.save(demand);
        log.info("Demande soumise : {}", demandId);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse startAnalysis(String demandId, String bankUserId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);
        stateMachineService.transition(
                demand, StatusDemande.IN_ANALYSIS, bankUserId,
                "Analyse démarrée par la banque exportateur");
        demandRepository.save(demand);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse approveDemand(String demandId, String bankUserId, String conditions) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        // DS-08 : calcul des détails financiers définitifs AU MOMENT de l'approbation
        if (demand.getMontantNominal() != null && !demand.getDetails().isEmpty()) {
            DetailTraite detail = demand.getDetails().get(0);
            int jours = detail.calculateDaysToMaturity();
            BigDecimal tauxDefaut = new BigDecimal("0.08");

            DetailsFinanciers financiers = demand.getDetailsFinanciers();

            if (financiers == null) {
                financiers = calculationService.buildDetailsFinanciers(
                        demand.getMontantNominal(),
                        tauxDefaut,
                        jours
                );

                if (financiers.getFinancialId() == null) {
                    financiers.setFinancialId(UUID.randomUUID().toString());
                }
            }

            financiers.setDemande(demand);

            // IMPORTANT : récupérer l'instance retournée par save()
            financiers = financiersRepository.save(financiers);
            demand.setDetailsFinanciers(financiers);
        }

        // DS-08 : enregistrer la Decision
        Decision decision = decisionRepository.findByDemande_DemandId(demandId)
                .orElse(Decision.builder()
                        .decisionId(UUID.randomUUID().toString())
                        .demande(demand)
                        .build());

        decision.setDecisionType(ma.portnet.demandservice.entity.enums.TypeDecision.APPROVE);
        decision.setDecisionDate(LocalDateTime.now());
        decision.setDecisionBy(bankUserId);
        decision.setConditions(conditions);

        // Même principe : récupérer l'instance retournée par save()
        decision = decisionRepository.save(decision);
        demand.setDecision(decision);

        stateMachineService.transition(
                demand,
                StatusDemande.APPROVED,
                bankUserId,
                "Demande approuvée — " + conditions
        );

        demandRepository.save(demand);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse rejectDemand(String demandId, String bankUserId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("La raison du rejet est obligatoire");
        }
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        // DS-09 : enregistrer la décision de rejet avec motif (table Decision)
        Decision decision = decisionRepository.findByDemande_DemandId(demandId)
                .orElse(Decision.builder()
                        .decisionId(UUID.randomUUID().toString())
                        .demande(demand)
                        .build());
        decision.setDecisionType(ma.portnet.demandservice.entity.enums.TypeDecision.REJECT);
        decision.setDecisionDate(LocalDateTime.now());
        decision.setDecisionBy(bankUserId);
        decision.setRecommendation(reason);     // motif + alternatives
        decisionRepository.save(decision);
        demand.setDecision(decision);

        stateMachineService.transition(
                demand, StatusDemande.REJECTED, bankUserId,
                "Demande rejetée : " + reason);

        demandRepository.save(demand);
        log.info("Demande rejetée : {} — raison : {}", demandId, reason);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse financeDemand(String demandId, String bankUserId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        if (demand.getMontantNominal() != null && !demand.getDetails().isEmpty()) {
            DetailTraite detail     = demand.getDetails().get(0);
            int          jours      = detail.calculateDaysToMaturity();
            BigDecimal   tauxDefaut = new BigDecimal("0.08");

            DetailsFinanciers financiers = calculationService.buildDetailsFinanciers(
                    demand.getMontantNominal(), tauxDefaut, jours);
            financiers.setFinancialId(UUID.randomUUID().toString());
            financiers.setDemande(demand);
            financiers.setDateFinancement(LocalDateTime.now());
            financiersRepository.save(financiers);
            demand.setDetailsFinanciers(financiers);
        }

        stateMachineService.transition(
                demand, StatusDemande.FINANCED, bankUserId,
                "Financement accordé — fonds versés à l'exportateur");
        demandRepository.save(demand);
        log.info("Demande financée : {}", demandId);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse financeDemand(String demandId, String bankUserId,
                                        String transferReference, java.time.LocalDate valueDate) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        // Réutilise les détails financiers calculés à l'approbation (DS-08), sinon les calcule
        if (demand.getDetailsFinanciers() == null
                && demand.getMontantNominal() != null && !demand.getDetails().isEmpty()) {
            DetailTraite detail     = demand.getDetails().get(0);
            int          jours      = detail.calculateDaysToMaturity();
            DetailsFinanciers fin   = calculationService.buildDetailsFinanciers(
                    demand.getMontantNominal(), new BigDecimal("0.08"), jours);
            fin.setFinancialId(UUID.randomUUID().toString());
            fin.setDemande(demand);
            financiersRepository.save(fin);
            demand.setDetailsFinanciers(fin);
        }

        // DS-10 : référence du virement + date de valeur + date de financement
        if (demand.getDetailsFinanciers() != null) {
            demand.getDetailsFinanciers().setDateFinancement(LocalDateTime.now());
            financiersRepository.save(demand.getDetailsFinanciers());
        }
        demand.setTransferReference(transferReference);
        demand.setValueDate(valueDate);

        stateMachineService.transition(
                demand, StatusDemande.FINANCED, bankUserId,
                "Financement accordé — virement réf. " + transferReference);

        demandRepository.save(demand);
        log.info("Demande financée : {} (réf. virement={})", demandId, transferReference);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse presentDemand(String demandId, String bankUserId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);
        stateMachineService.transition(
                demand, StatusDemande.PRESENTATION_AT_MATURITY, bankUserId,
                "Traite présentée à l'échéance à la banque importatrice");
        demandRepository.save(demand);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse settleDemand(String demandId, String bankImportUserId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);
        stateMachineService.transition(
                demand, StatusDemande.SETTLED, bankImportUserId,
                "Montant nominal réglé par la banque importatrice");
        demandRepository.save(demand);
        log.info("Demande réglée : {}", demandId);
        return mapper.toResponse(demand);
    }

    // ── CALCUL FINANCIER (aperçu) ─────────────────────────────

    public Map<String, Object> calculatePreview(BigDecimal nominal, BigDecimal taux, int jours) {
        Map<String, BigDecimal> breakdown = calculationService.breakdown(nominal, taux, jours);
        return Map.of(
                "nominal", nominal, "taux", taux, "jours", jours,
                "agios", breakdown.get("agios"),
                "commissionNegociation", breakdown.get("commissionNegociation"),
                "fraisSwift", breakdown.get("fraisSwift"),
                "fraisCourrier", breakdown.get("fraisCourrier"),
                "montantNet", breakdown.get("montantNet"));
    }

    public Map<String, Object> getDashboardStats() {
        List<Object[]> counts    = demandRepository.countByStatus();
        BigDecimal totalFinanced = demandRepository.sumMontantNetFinanced();
        Map<String, Long> byStatus = new java.util.HashMap<>();
        for (Object[] row : counts) byStatus.put(row[0].toString(), (Long) row[1]);
        return Map.of("byStatus", byStatus,
                "totalFinanced", totalFinanced != null ? totalFinanced : BigDecimal.ZERO);
    }

    // ── HELPERS ───────────────────────────────────────────────

    private DemandAchatTraite findDemandOrThrow(String demandId) {
        return demandRepository.findById(demandId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable : " + demandId));
    }

    private String generateRequestNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String num  = "EBP-" + year + "-" + String.format("%06d", COUNTER.getAndIncrement());
        while (demandRepository.existsByRequestNumber(num)) {
            num = "EBP-" + year + "-" + String.format("%06d", COUNTER.getAndIncrement());
        }
        return num;
    }

    private StatusDemande parseStatus(String status) {
        try {
            return StatusDemande.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut invalide : " + status);
        }
    }
}