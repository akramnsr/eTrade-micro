// notification-service/.../config/NotificationEmailProperties.java
package ma.portnet.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.email")
public class NotificationEmailProperties {

    private String from = "eTrade <etrade@portnet.ma>";
    private boolean enabled = false;

    public String getFrom()            { return from; }
    public void setFrom(String from)   { this.from = from; }
    public boolean isEnabled()         { return enabled; }
    public void setEnabled(boolean e)  { this.enabled = e; }
}