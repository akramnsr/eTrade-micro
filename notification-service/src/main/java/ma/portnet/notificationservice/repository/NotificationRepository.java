package ma.portnet.notificationservice.repository;

import ma.portnet.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByRecipientIdOrderByCreatedDateDesc(String recipientId);

    List<Notification> findByRecipientIdAndReadFlagFalseOrderByCreatedDateDesc(
            String recipientId
    );

    long countByRecipientIdAndReadFlagFalse(String recipientId);

    List<Notification> findByDemandIdOrderByCreatedDateDesc(String demandId);
}