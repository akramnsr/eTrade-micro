package ma.portnet.authservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DemandSummaryDto(
        String        demandId,
        String        requestNumber,
        String        status,
        String        exporterId,
        String        devise,
        BigDecimal    montantNominal,
        LocalDate     dateEcheance,
        LocalDateTime createdDate,
        LocalDateTime submittedDate,
        int           nombreTraites
) {}