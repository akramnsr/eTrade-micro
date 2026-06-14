package ma.portnet.demandservice.config;

public final class RabbitConstants {
    private RabbitConstants() {}

    public static final String EXCHANGE     = "etrade.events";
    public static final String QUEUE        = "etrade.notifications.queue";
    public static final String ROUTING_KEY  = "demand.status.changed";

    public static final String DLX          = "etrade.events.dlx";
    public static final String DLQ          = "etrade.notifications.dlq";
}