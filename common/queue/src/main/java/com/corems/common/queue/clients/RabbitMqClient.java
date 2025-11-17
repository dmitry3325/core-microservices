package com.corems.common.queue.clients;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.config.QueueProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import java.util.Optional;

public class RabbitMqClient implements QueueClient {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqClient.class);

    private final RabbitTemplate rabbitTemplate;
    private final QueueProperties.RabbitMqProperties props;

    public static void validate(QueueProperties.RabbitMqProperties rabbitProp) {
        if (rabbitProp == null) {
            throw new IllegalArgumentException("RabbitMQ provider config missing under queue.providers.rabbitMq");
        }

        if (rabbitProp.getHost() == null || rabbitProp.getHost().isBlank()) {
            throw new IllegalArgumentException("RabbitMQ host missing (queue.providers.rabbitMq.host)");
        }

        if (rabbitProp.getDefaultQueue() == null || rabbitProp.getDefaultQueue().isBlank()) {
            throw new IllegalArgumentException("RabbitMQ defaultQueue missing (queue.providers.rabbitMq.defaultQueue)");
        }

        if (rabbitProp.getPort() <= 0 || rabbitProp.getPort() > 65535) {
            throw new IllegalArgumentException("RabbitMQ port is invalid: " + rabbitProp.getPort());
        }

        if (rabbitProp.getPollTimeoutMs() < 0) {
            throw new IllegalArgumentException("RabbitMQ pollTimeoutMs must be non-negative: " + rabbitProp.getPollTimeoutMs());
        }
    }

    public static RabbitMqClient createRabbitMqClient(QueueProperties.RabbitMqProperties rabbitProps, MessageConverter converter) {
        validate(rabbitProps);

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitProps.getHost(), rabbitProps.getPort());
        if (rabbitProps.getUsername() != null && !rabbitProps.getUsername().isBlank()) {
            connectionFactory.setUsername(rabbitProps.getUsername());
        }
        if (rabbitProps.getPassword() != null && !rabbitProps.getPassword().isBlank()) {
            connectionFactory.setPassword(rabbitProps.getPassword());
        }

        RabbitTemplate rt = new RabbitTemplate(connectionFactory);
        rt.setMessageConverter(converter);

        return new RabbitMqClient(rt, rabbitProps);
    }

    public RabbitMqClient(RabbitTemplate rabbitTemplate, QueueProperties.RabbitMqProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @Override
    public <T> void send(QueueMessage<T> message) throws ServiceException {
        send(props.getRoutingKey(), message);
    }

    @Override
    public <T> void send(String destination, QueueMessage<T> message) throws ServiceException {
        String exchange = props.getExchange() == null ? "" : props.getExchange();
        String dest = (destination == null || destination.isEmpty()) ? props.getDefaultQueue() : destination;
        try {
            rabbitTemplate.convertAndSend(exchange, dest, message);
            log.debug("Sent message to exchange='{}' routing='{}' id={}", exchange, dest, message.getId());
        } catch (Exception e) {
            log.error("Failed to send message id={}", message.getId(), e);
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Failed to send message.");
        }
    }

    @Override
    public <T> Optional<QueueMessage<T>> poll(String destination) {
        try {
            // TODO check it
            @SuppressWarnings("unchecked")
            QueueMessage<T> msg = (QueueMessage<T>) rabbitTemplate.receiveAndConvert(destination, props.getPollTimeoutMs());
            return Optional.ofNullable(msg);
        } catch (Exception e) {
            log.error("Failed to poll destination={}", destination, e);
            return Optional.empty();
        }
    }
}
