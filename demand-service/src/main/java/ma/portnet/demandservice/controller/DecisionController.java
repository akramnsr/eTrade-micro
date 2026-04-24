package ma.portnet.demandservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import ma.portnet.demandservice.dto.request.DecisionRequest;
import ma.portnet.demandservice.dto.response.ApiResponse;
import ma.portnet.demandservice.dto.response.DemandResponse;
import ma.portnet.demandservice.entity.Decision;
import ma.portnet.demandservice.entity.DemandAchatTraite;
import ma.portnet.demandservice.entity.enums.StatusDemande;
import ma.portnet.demandservice.entity.enums.TypeDecision;
import ma.portnet.demandservice.exception.InvalidStateTransitionException;
import ma.portnet.demandservice.exception.ResourceNotFoundException;
import ma.portnet.demandservice.mapper.DemandMapper;
import ma.portnet.demandservice.repository.DecisionRepository;
import ma.portnet.demandservice.repository.DemandRepository;
import ma.portnet.demandservice.service.DemandService;
import ma.portnet.demandservice.service.StateMachineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/decisions")
@Tag(name = "Decisions", description = "Décisions de la banque sur les demandes EBP")
@SecurityRequirement(name = "bearerAuth")
public class DecisionController {

    private final DemandRepository   demandRepository;
    private final DecisionRepository decisionRepository;
    private final StateMachineService stateMachineService;
    private final DemandService      demandService;
    private final DemandMapper       mapper;

    public DecisionController(
            DemandRepository   demandRepository,
            DecisionRepository decisionRepository,
            StateMachineService stateMachineService,
            DemandService      demandService,
            DemandMapper       mapper
    ) {
        this.demandRepository   = demandRepository;
        this.decisionRepository = decisionRepository;
        this.stateMachineService = stateMachineService;
        this.demandService      = demandService;
        this.mapper             = mapper;
    }

    /**
     * Soumettre une décision complète avec scoring
     * Remplace approve/reject du DemandController avec plus de détails
     */
    @PostMapping("/{demandId}")
    @PreAuthorize("hasRole('BANQUE_EXPORTATEUR')")
    @Operation(summary = "Soumettre une décision avec scoring de risque")
    public ResponseEntity<ApiResponse<DemandResponse>> submitDecision(
            @PathVariable String demandId,
            @Valid @RequestBody DecisionRequest request,
            Authentication auth
    ) {
        String bankUserId = extractUserId(auth);

        DemandAchatTraite demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Demande introuvable : " + demandId
                ));

        // La demande doit être en analyse
        if (demand.getStatus() != StatusDemande.IN_ANALYSIS) {
            throw new InvalidStateTransitionException(
                    "Une décision ne peut être prise que sur une demande IN_ANALYSIS. " +
                            "Statut actuel : " + demand.getStatus()
            );
        }

        // Créer ou mettre à jour la décision
        Decision decision = decisionRepository
                .findByDemande_DemandId(demandId)
                .orElse(Decision.builder()
                        .decisionId(UUID.randomUUID().toString())
                        .demande(demand)
                        .build());

        decision.setDecisionType(request.decisionType());
        decision.setDecisionDate(LocalDateTime.now());
        decision.setDecisionBy(bankUserId);
        decision.setBankRatingScore(request.bankRatingScore());
        decision.setCountryRiskScore(request.countryRiskScore());
        decision.setDocumentaryRiskScore(request.documentaryRiskScore());
        decision.setCountryRiskCategory(request.countryRiskCategory());
        decision.setConditions(request.conditions());
        decision.setRecommendation(request.recommendation());
        decision.setOverallRiskScore(decision.calculateOverallRisk());

        decisionRepository.save(decision);
        demand.setDecision(decision);

        // Appliquer la transition selon le type de décision
        DemandResponse response = switch (request.decisionType()) {
            case APPROVE -> demandService.approveDemand(
                    demandId, bankUserId,
                    request.conditions() != null ? request.conditions() : ""
            );
            case REJECT -> demandService.rejectDemand(
                    demandId, bankUserId,
                    request.reason() != null ? request.reason() : "Décision de rejet"
            );
            case REQUEST_MORE_INFO -> {
                // Reste en IN_ANALYSIS, juste logger la demande d'info
                stateMachineService.logComment(
                        demand, bankUserId, "Informations complémentaires demandées"
                );
                demandRepository.save(demand);
                yield mapper.toResponse(demand);
            }
            case REQUEST_ADDITIONAL_GUARANTEE -> {
                stateMachineService.logComment(
                        demand, bankUserId, "Garanties supplémentaires demandées"
                );
                demandRepository.save(demand);
                yield mapper.toResponse(demand);
            }
        };

        return ResponseEntity.ok(ApiResponse.ok("Décision enregistrée", response));
    }

    /**
     * Obtenir la décision d'une demande
     */
    @GetMapping("/{demandId}")
    @Operation(summary = "Obtenir la décision d'une demande")
    public ResponseEntity<ApiResponse<DemandResponse.DecisionResponse>> getDecision(
            @PathVariable String demandId
    ) {
        Decision decision = decisionRepository
                .findByDemande_DemandId(demandId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucune décision pour la demande : " + demandId
                ));

        DemandResponse.DecisionResponse response = new DemandResponse.DecisionResponse(
                decision.getDecisionId(),
                decision.getDecisionType(),
                decision.getDecisionDate(),
                decision.getDecisionBy(),
                decision.calculateOverallRisk(),
                decision.getBankRatingScore(),
                decision.getCountryRiskScore(),
                decision.getDocumentaryRiskScore(),
                decision.getConditions(),
                decision.getRecommendation()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private String extractUserId(Authentication auth) {
        return ((Jwt) auth.getPrincipal()).getSubject();
    }
}