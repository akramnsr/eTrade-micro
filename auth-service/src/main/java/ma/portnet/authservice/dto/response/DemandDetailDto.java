package ma.portnet.authservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DemandDetailDto(
        String demandId,
        String requestNumber,
        String status,
        String exporterId,
        String devise,
        BigDecimal montantNominal,
        LocalDate dateEcheance,
        LocalDateTime createdDate,
        LocalDateTime submittedDate,
        String description,
        Boolean isReadyForSubmission,
        List<Map<String, Object>> details,
        List<Map<String, Object>> historique,
        Map<String, Object> detailsFinanciers,
        Map<String, Object> decision
) {}