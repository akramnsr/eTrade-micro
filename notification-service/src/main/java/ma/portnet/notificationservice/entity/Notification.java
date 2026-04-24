package ma.portnet.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.portnet.notificationservice.entity.enums.NotificationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @Column(name = "notification_id", length = 36)
    private String notificationId;

    // Référence externe vers demand-service
    @Column(name = "demand_id", nullable = false, length = 36)
    private String demandId;

    // Référence externe vers auth-service
    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "read_flag", nullable = false)
    private Boolean readFlag;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
        if (readFlag    == null) readFlag    = false;
    }
}