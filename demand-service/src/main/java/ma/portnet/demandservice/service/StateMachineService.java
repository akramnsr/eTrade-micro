package ma.portnet.demandservice.service;

import lombok.extern.slf4j.Slf4j;
import ma.portnet.demandservice.entity.DemandAchatTraite;
import ma.portnet.demandservice.entity.HistoriqueStatuts;
import ma.portnet.demandservice.entity.enums.StatusDemande;
import ma.portnet.demandservice.exception.InvalidStateTransitionException;
import ma.portnet.demandservice.repository.HistoriqueStatutsRepository;
import org.springframework.stereotype.Service;
import ma.portnet.demandservice.service.NotificationAsyncService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class StateMachineService {

    private final HistoriqueStatutsRepository historiqueRepository;
    private final NotificationAsyncService    notificationClient;
    // Matrice des transitions autorisées
    private static final Map<StatusDemande, Set<StatusDemande>> TRANSITIONS = Map.of(
            StatusDemande.DRAFT,
            Set.of(StatusDemande.SUBMITTED),
            StatusDemande.SUBMITTED,
            Set.of(StatusDemande.IN_ANALYSIS),
            StatusDemande.IN_ANALYSIS,
            Set.of(StatusDemande.APPROVED, StatusDemande.REJECTED),
            StatusDemande.APPROVED,
            Set.of(StatusDemande.FINANCED),
            StatusDemande.FINANCED,
            Set.of(StatusDemande.PRESENTATION_AT_MATURITY),
            StatusDemande.PRESENTATION_AT_MATURITY,
            Set.of(StatusDemande.SETTLED),
            StatusDemande.REJECTED,
            Set.of(),
            StatusDemande.SETTLED,
            Set.of()
    );

    public StateMachineService(
            HistoriqueStatutsRepository historiqueRepository,
            NotificationAsyncService    notificationClient
    ) {
        this.historiqueRepository = historiqueRepository;
        this.notificationClient   = notificationClient;
    }
    public void logForcedTransition(
            DemandAchatTraite demand,
            StatusDemande from,
            StatusDemande to,
            String userId,
            String reason
    ) {
        HistoriqueStatuts log = HistoriqueStatuts.builder()
                .historyId(UUID.randomUUID().toString())
                .demande(demand)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(userId)
                .changeDate(LocalDateTime.now())
                .reason(reason)
                .comment("Transition forcée")
                .build();

        historiqueRepository.save(log);
    }
    public void transition(
            DemandAchatTraite demand,
            StatusDemande     newStatus,
            String            changedById,
            String            reason
    ) {
        StatusDemande current = demand.getStatus();
        Set<StatusDemande> allowed = TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(newStatus)) {
            throw new InvalidStateTransitionException(
                    "Transition interdite : " + current + " → " + newStatus +
                            ". Transitions autorisées depuis " + current + " : " + allowed
            );
        }

        demand.setStatus(newStatus);
        saveHistorique(demand, current, newStatus, changedById, reason);

        // Notification asynchrone
        notificationClient.notifyStatusChange(
                demand.getDemandId(),
                demand.getExporterId(),
                newStatus.name()
        );

        log.info("Transition : demande={} {} → {} par {}",
                demand.getRequestNumber(), current, newStatus, changedById);
    }

    // Enregistrer la création initiale en DRAFT
    public void logInitialCreation(DemandAchatTraite demand, String createdById) {
        saveHistorique(demand, null, StatusDemande.DRAFT, createdById, "Demande créée");
    }

    // Enregistrer un commentaire sans changer de statut
    public void logComment(
            DemandAchatTraite demand,
            String            userId,
            String            comment
    ) {
        HistoriqueStatuts h = HistoriqueStatuts.builder()
                .historyId(UUID.randomUUID().toString())
                .demande(demand)
                .fromStatus(demand.getStatus())
                .toStatus(demand.getStatus())   // même statut
                .changedBy(userId)
                .changeDate(LocalDateTime.now())
                .reason("Commentaire")
                .comment(comment)
                .build();
        historiqueRepository.save(h);
    }

    private void saveHistorique(
            DemandAchatTraite demand,
            StatusDemande     from,
            StatusDemande     to,
            String            changedById,
            String            reason
    ) {
        HistoriqueStatuts h = HistoriqueStatuts.builder()
                .historyId(UUID.randomUUID().toString())
                .demande(demand)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedById)
                .changeDate(LocalDateTime.now())
                .reason(reason)
                .build();
        historiqueRepository.save(h);
        demand.getHistorique().add(h);
    }
}