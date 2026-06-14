// demand-service/.../event/StatusChangeEvent.java
package ma.portnet.demandservice.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record StatusChangeEvent(
        String        demandId,
        String        exporterId,
        String        newStatus,
        String        recipientEmail,   // ← Option C
        LocalDateTime occurredAt
) implements Serializable {}