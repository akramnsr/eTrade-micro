package ma.portnet.demandservice.service;

import lombok.extern.slf4j.Slf4j;
import ma.portnet.demandservice.dto.request.CreateDemandRequest;
import ma.portnet.demandservice.dto.request.UpdateDemandRequest;
import ma.portnet.demandservice.dto.response.DemandResponse;
import ma.portnet.demandservice.dto.response.DemandSummaryResponse;
import ma.portnet.demandservice.entity.*;
import ma.portnet.demandservice.entity.enums.StatusDemande;
import ma.portnet.demandservice.exception.InvalidStateTransitionException;
import ma.portnet.demandservice.exception.ResourceNotFoundException;
import ma.portnet.demandservice.mapper.DemandMapper;
import ma.portnet.demandservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DemandService {

    private final DemandRepository            demandRepository;
    private final DetailTraiteRepository      detailTraiteRepository;
    private final DetailsFinanciersRepository financiersRepository;
    private final HistoriqueStatutsRepository historiqueRepository;
    private final StateMachineService         stateMachineService;
    private final FinancialCalculationService calculationService;
    private final DocumentClient              documentClient;
    private final DemandMapper                mapper;

    // Compteur thread-safe pour les numéros de demande
    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    public DemandService(
            DemandRepository            demandRepository,
            DetailTraiteRepository      detailTraiteRepository,
            DetailsFinanciersRepository financiersRepository,
            HistoriqueStatutsRepository historiqueRepository,
            StateMachineService         stateMachineService,
            FinancialCalculationService calculationService,
            DocumentClient              documentClient,
            DemandMapper                mapper
    ) {
        this.demandRepository     = demandRepository;
        this.detailTraiteRepository = detailTraiteRepository;
        this.financiersRepository = financiersRepository;
        this.historiqueRepository = historiqueRepository;
        this.stateMachineService  = stateMachineService;
        this.calculationService   = calculationService;
        this.documentClient       = documentClient;
        this.mapper               = mapper;
    }

    // ── CREATE ────────────────────────────────────────────────

    @Transactional
    public DemandResponse createDemand(CreateDemandRequest request, String exporterId) {
        String demandId       = UUID.randomUUID().toString();
        String requestNumber  = generateRequestNumber();

        // Créer la demande principale
        DemandAchatTraite demand = DemandAchatTraite.builder()
                .demandId(demandId)
                .requestNumber(requestNumber)
                .status(StatusDemande.DRAFT)
                .exporterId(exporterId)
                .devise(request.devise())
                .montantNominal(request.montantNominal())
                .dateEcheance(request.dateEcheance())
                .description(request.description())
                .isReadyForSubmission(false)
                .createdDate(LocalDateTime.now())
                .build();

        demandRepository.save(demand);

        // Créer les détails des traites
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

        // Enregistrer la création dans l'historique
        stateMachineService.logInitialCreation(demand, exporterId);

        log.info("Demande créée : {} par exporteur {}", requestNumber, exporterId);
        return mapper.toResponse(demand);
    }

    // ── READ ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DemandResponse getDemandById(String demandId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);
        return mapper.toResponse(demand);
    }

    @Transactional(readOnly = true)
    public List<DemandSummaryResponse> listDemands(
            String userId, List<String> roles, String statusFilter
    ) {
        List<DemandAchatTraite> demands;

        // L'exportateur ne voit que ses propres demandes
        if (roles.contains("EXPORTATEUR")) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                StatusDemande status = parseStatus(statusFilter);
                demands = demandRepository
                        .findByExporterIdAndStatusOrderByCreatedDateDesc(userId, status);
            } else {
                demands = demandRepository.findByExporterIdOrderByCreatedDateDesc(userId);
            }
        }
        // La banque exportateur voit toutes les demandes soumises
        else if (roles.contains("BANQUE_EXPORTATEUR")) {
            if (statusFilter != null && !statusFilter.isBlank()) {
                demands = demandRepository
                        .findByStatusOrderByCreatedDateDesc(parseStatus(statusFilter));
            } else {
                demands = demandRepository.findAllVisibleToBank();
            }
        }
        // La banque importateur voit les demandes en phase finale
        else if (roles.contains("BANQUE_IMPORTATEUR")) {
            demands = demandRepository
                    .findByStatusOrderByCreatedDateDesc(StatusDemande.PRESENTATION_AT_MATURITY);
        }
        // Admin : tout voir
        else {
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

        // Seul l'exportateur propriétaire peut modifier
        if (!demand.getExporterId().equals(exporterId)) {
            throw new InvalidStateTransitionException(
                    "Vous n'êtes pas autorisé à modifier cette demande"
            );
        }

        // Modification uniquement en DRAFT
        if (demand.getStatus() != StatusDemande.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Impossible de modifier une demande avec le statut : " + demand.getStatus()
            );
        }

        // Appliquer les modifications (seulement les champs non-null)
        if (request.devise()         != null) demand.setDevise(request.devise());
        if (request.montantNominal() != null) demand.setMontantNominal(request.montantNominal());
        if (request.dateEcheance()   != null) demand.setDateEcheance(request.dateEcheance());
        if (request.description()    != null) demand.setDescription(request.description());

        demandRepository.save(demand);
        log.info("Demande mise à jour : {}", demandId);
        return mapper.toResponse(demand);
    }

    // ── DELETE ────────────────────────────────────────────────

    @Transactional
    public void deleteDemand(String demandId, String exporterId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        if (!demand.getExporterId().equals(exporterId)) {
            throw new InvalidStateTransitionException(
                    "Vous n'êtes pas autorisé à supprimer cette demande"
            );
        }

        if (demand.getStatus() != StatusDemande.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Impossible de supprimer une demande soumise"
            );
        }

        demandRepository.delete(demand);
        log.info("Demande supprimée : {}", demandId);
    }

    // ── TRANSITIONS DE STATUT ─────────────────────────────────

    @Transactional
    public DemandResponse submitDemand(String demandId, String exporterId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        if (!demand.getExporterId().equals(exporterId)) {
            throw new InvalidStateTransitionException(
                    "Seul l'exportateur propriétaire peut soumettre cette demande"
            );
        }

        // Vérifier les documents obligatoires via document-service
        boolean hasDocuments = documentClient.hasRequiredDocuments(demandId);
        if (!hasDocuments) {
            throw new InvalidStateTransitionException(
                    "Documents obligatoires manquants : traite acceptée, " +
                            "facture commerciale et connaissement requis"
            );
        }

        demand.setSubmittedDate(LocalDateTime.now());
        demand.setIsReadyForSubmission(true);

        stateMachineService.transition(
                demand, StatusDemande.SUBMITTED, exporterId,
                "Demande soumise par l'exportateur"
        );

        demandRepository.save(demand);
        log.info("Demande soumise : {}", demandId);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse startAnalysis(String demandId, String bankUserId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        stateMachineService.transition(
                demand, StatusDemande.IN_ANALYSIS, bankUserId,
                "Analyse démarrée par la banque exportateur"
        );

        demandRepository.save(demand);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse approveDemand(
            String demandId, String bankUserId, String conditions
    ) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        stateMachineService.transition(
                demand, StatusDemande.APPROVED, bankUserId,
                "Demande approuvée — " + conditions
        );

        demandRepository.save(demand);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse rejectDemand(
            String demandId, String bankUserId, String reason
    ) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("La raison du rejet est obligatoire");
        }

        DemandAchatTraite demand = findDemandOrThrow(demandId);

        stateMachineService.transition(
                demand, StatusDemande.REJECTED, bankUserId,
                "Demande rejetée : " + reason
        );

        demandRepository.save(demand);
        log.info("Demande rejetée : {} — raison : {}", demandId, reason);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse financeDemand(String demandId, String bankUserId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        // Calculer et persister les détails financiers
        if (demand.getMontantNominal() != null && !demand.getDetails().isEmpty()) {
            DetailTraite detail    = demand.getDetails().get(0);
            int          jours     = detail.calculateDaysToMaturity();
            BigDecimal   tauxDefaut = new BigDecimal("0.08"); // 8% annuel par défaut

            DetailsFinanciers financiers = calculationService.buildDetailsFinanciers(
                    demand.getMontantNominal(), tauxDefaut, jours
            );
            financiers.setFinancialId(UUID.randomUUID().toString());
            financiers.setDemande(demand);
            financiers.setDateFinancement(LocalDateTime.now());
            financiersRepository.save(financiers);
            demand.setDetailsFinanciers(financiers);
        }

        stateMachineService.transition(
                demand, StatusDemande.FINANCED, bankUserId,
                "Financement accordé — fonds versés à l'exportateur"
        );

        demandRepository.save(demand);
        log.info("Demande financée : {}", demandId);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse presentDemand(String demandId, String bankUserId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        stateMachineService.transition(
                demand, StatusDemande.PRESENTATION_AT_MATURITY, bankUserId,
                "Traite présentée à l'échéance à la banque importatrice"
        );

        demandRepository.save(demand);
        return mapper.toResponse(demand);
    }

    @Transactional
    public DemandResponse settleDemand(String demandId, String bankImportUserId) {
        DemandAchatTraite demand = findDemandOrThrow(demandId);

        stateMachineService.transition(
                demand, StatusDemande.SETTLED, bankImportUserId,
                "Montant nominal réglé par la banque importatrice"
        );

        demandRepository.save(demand);
        log.info("Demande réglée : {}", demandId);
        return mapper.toResponse(demand);
    }

    // ── CALCUL FINANCIER (aperçu sans créer de demande) ───────

    public Map<String, Object> calculatePreview(
            BigDecimal nominal, BigDecimal taux, int jours
    ) {
        Map<String, BigDecimal> breakdown = calculationService.breakdown(nominal, taux, jours);
        return Map.of(
                "nominal",             nominal,
                "taux",                taux,
                "jours",               jours,
                "agios",               breakdown.get("agios"),
                "commissionNegociation", breakdown.get("commissionNegociation"),
                "fraisSwift",          breakdown.get("fraisSwift"),
                "fraisCourrier",       breakdown.get("fraisCourrier"),
                "montantNet",          breakdown.get("montantNet")
        );
    }

    // ── STATS DASHBOARD ───────────────────────────────────────

    public Map<String, Object> getDashboardStats() {
        List<Object[]> counts    = demandRepository.countByStatus();
        BigDecimal totalFinanced = demandRepository.sumMontantNetFinanced();

        Map<String, Long> byStatus = new java.util.HashMap<>();
        for (Object[] row : counts) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }

        return Map.of(
                "byStatus",      byStatus,
                "totalFinanced", totalFinanced != null ? totalFinanced : BigDecimal.ZERO
        );
    }

    // ── HELPERS PRIVÉS ────────────────────────────────────────

    private DemandAchatTraite findDemandOrThrow(String demandId) {
        return demandRepository.findById(demandId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Demande introuvable : " + demandId)
                );
    }

    private String generateRequestNumber() {
        String year  = String.valueOf(LocalDateTime.now().getYear());
        String seq   = String.format("%06d", COUNTER.getAndIncrement());
        String num   = "EBP-" + year + "-" + seq;

        // Garantir l'unicité en base
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