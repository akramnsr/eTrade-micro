package ma.portnet.documentservice.dto.request;

import java.time.LocalDateTime;

public record DocumentDemandGroupResponse(
        String demandId,
        Long documentCount,
        LocalDateTime lastUploadedDate
) {
}