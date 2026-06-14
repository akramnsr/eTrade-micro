// notification-service/.../config/RabbitConfig.java
package ma.portnet.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

@Configuration
public class RabbitConfig {

    // Exchange principal
    @Bean
    public TopicExchange etradeEventsExchange() {
        return new TopicExchange(RabbitConstants.EXCHANGE, true, false);
    }

    // Queue principale avec routage vers la DLQ en cas d'échec
    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(RabbitConstants.QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitConstants.DLX)
                .withArgument("x-dead-letter-routing-key", RabbitConstants.DLQ)
                .build();
    }

    @Bean
    public Binding notificationsBinding() {
        return BindingBuilder.bind(notificationsQueue())
                .to(etradeEventsExchange())
                .with(RabbitConstants.ROUTING_KEY);
    }

    // Dead-letter exchange + queue (messages qui échouent après retries)
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitConstants.DLX, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitConstants.DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitConstants.DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }
}