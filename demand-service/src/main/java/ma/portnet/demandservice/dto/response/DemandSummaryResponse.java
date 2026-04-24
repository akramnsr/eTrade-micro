package ma.portnet.demandservice.dto.response;

import ma.portnet.demandservice.entity.enums.StatusDemande;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DemandSummaryResponse(
        String        demandId,
        String        requestNumber,
        StatusDemande status,
        String        exporterId,
        String        devise,
        BigDecimal    montantNominal,
        LocalDate     dateEcheance,
        LocalDateTime createdDate,
        LocalDateTime submittedDate,
        int           nombreTraites
) {}