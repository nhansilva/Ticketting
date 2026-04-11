package com.ticketing.common.config.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration — optional, chỉ activate khi spring-boot-starter-amqp có trên classpath.
 *
 * Exchanges và queues định nghĩa ở đây là base — service có thể khai báo thêm.
 * Dùng Dead Letter Exchange (DLX) pattern để xử lý failed messages.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
public class RabbitMQConfig {

    // ── Exchange names ───────────────────────────────────────────────────

    public static final String TICKETING_EXCHANGE     = "ticketing.events";
    public static final String DEAD_LETTER_EXCHANGE   = "ticketing.dlx";

    // ── Queue names ──────────────────────────────────────────────────────

    public static final String NOTIFICATION_QUEUE     = "notification.queue";
    public static final String DEAD_LETTER_QUEUE      = "dead.letter.queue";

    // ── Routing keys ─────────────────────────────────────────────────────

    public static final String BOOKING_CREATED_KEY    = "booking.created";
    public static final String PAYMENT_COMPLETED_KEY  = "payment.completed";

    @Bean
    public TopicExchange ticketingExchange() {
        return ExchangeBuilder.topicExchange(TICKETING_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                // Failed messages → DLX
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .withArgument("x-message-ttl", 300_000) // 5 phút TTL
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange ticketingExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(ticketingExchange)
                .with("booking.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setExchange(TICKETING_EXCHANGE);
        return template;
    }
}
