// notification-service/.../service/EmailService.java
package ma.portnet.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import ma.portnet.notificationservice.config.NotificationEmailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationEmailProperties props;

    public EmailService(JavaMailSender mailSender,
                        NotificationEmailProperties props) {
        this.mailSender = mailSender;
        this.props      = props;
    }

    /**
     * Envoie un email simple. Non bloquant pour le traitement de la notification :
     * un échec d'envoi est loggé mais ne fait pas échouer le listener.
     */
    public void send(String to, String subject, String body) {
        if (!props.isEnabled()) {
            log.info("[EMAIL DÉSACTIVÉ] À: {} | Sujet: {}", to, subject);
            return;
        }
        if (to == null || to.isBlank()) {
            log.warn("Email non envoyé : destinataire absent (sujet={})", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(props.getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email envoyé à {} (sujet={})", to, subject);
        } catch (Exception e) {
            log.warn("Échec d'envoi de l'email à {} : {}", to, e.getMessage());
        }
    }
}