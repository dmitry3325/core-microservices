package com.corems.common.queue.clients;

import com.corems.common.exception.ServiceException;
import com.corems.common.exception.handler.DefaultExceptionReasonCodes;
import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.config.QueueProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class RabbitMqClient implements QueueClient {

    private final RabbitTemplate rabbitTemplate;
    private final QueueProperties.RabbitMqProperties props;

    public static void validate(QueueProperties.RabbitMqProperties rabbitProp) {
        if (rabbitProp == null) {
            throw new IllegalArgumentException("RabbitMQ provider config missing under queue.providers.rabbitMq");
        }

        if (rabbitProp.getHost() == null || rabbitProp.getHost().isBlank()) {
            throw new IllegalArgumentException("RabbitMQ host missing (queue.providers.rabbitMq.host)");
        }

        // Allow either a defaultQueue OR at least one requiredQueue to be configured.
        boolean hasDefault = rabbitProp.getDefaultQueue() != null && !rabbitProp.getDefaultQueue().isBlank();
        boolean hasRequired = rabbitProp.getRequiredQueues() != null && !rabbitProp.getRequiredQueues().isEmpty();
        if (!hasDefault && !hasRequired) {
            throw new IllegalArgumentException("RabbitMQ must define either defaultQueue or requiredQueues (queue.providers.rabbitMq.defaultQueue or queue.providers.rabbitMq.requiredQueues)");
        }

        if (rabbitProp.getPort() <= 0 || rabbitProp.getPort() > 65535) {
            throw new IllegalArgumentException("RabbitMQ port is invalid: " + rabbitProp.getPort());
        }
    }

    public static RabbitMqClient createRabbitMqClient(QueueProperties.RabbitMqProperties rabbitProps, MessageConverter converter) {
        validate(rabbitProps);

        final RabbitTemplate rt = getRabbitTemplate(rabbitProps, converter);
        final List<String> queuesToCheck = getQueues(rabbitProps);

        RabbitMqClient rabbitMqClient = new RabbitMqClient(rt, rabbitProps);

        if (rabbitProps.getExchange() != null && !rabbitProps.getExchange().isBlank()) {
            try {
                rt.execute(channel -> {
                    channel.exchangeDeclarePassive(rabbitProps.getExchange());
                    return null;
                });
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to verify RabbitMQ exchange '" + rabbitProps.getExchange() + "': " + ex.getMessage(), ex);
            }
            log.info("Verified RabbitMQ exchange: {}", rabbitProps.getExchange());
        }

        rt.execute(channel -> {
            for (String q : queuesToCheck) {
                try {
                    channel.queueDeclarePassive(q);
                } catch (Exception ex) {
                    throw new IllegalStateException("Failed to verify RabbitMQ queue: " + q, ex);
                }
                log.info("Verified RabbitMQ queue: {}", q);
            }
            return null;
        });

        return rabbitMqClient;
    }

    private static List<String> getQueues(QueueProperties.RabbitMqProperties rabbitProps) {
        List<String> queuesToCheck = new ArrayList<>();
        if (rabbitProps.getDefaultQueue() != null && !rabbitProps.getDefaultQueue().isBlank()) {
            queuesToCheck.add(rabbitProps.getDefaultQueue());
        }
        if (rabbitProps.getRequiredQueues() != null && !rabbitProps.getRequiredQueues().isEmpty()) {
            for (String q : rabbitProps.getRequiredQueues()) {
                if (q != null && !q.isBlank() && !queuesToCheck.contains(q)) {
                    queuesToCheck.add(q);
                }
            }
        }
        return queuesToCheck;
    }

    private static RabbitTemplate getRabbitTemplate(QueueProperties.RabbitMqProperties rabbitProps, MessageConverter converter) {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(rabbitProps.getHost(), rabbitProps.getPort());
        if (rabbitProps.getUsername() != null && !rabbitProps.getUsername().isBlank()) {
            connectionFactory.setUsername(rabbitProps.getUsername());
        }
        if (rabbitProps.getPassword() != null && !rabbitProps.getPassword().isBlank()) {
            connectionFactory.setPassword(rabbitProps.getPassword());
        }

        RabbitTemplate rt = new RabbitTemplate(connectionFactory);
        rt.setMessageConverter(converter);
        return rt;
    }

    public RabbitMqClient(RabbitTemplate rabbitTemplate, QueueProperties.RabbitMqProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @Override
    public QueueProperties.RabbitMqProperties getProperties() {
        return props;
    }

    @Override
    public void send(QueueMessage message) throws ServiceException {
        send(props.getDefaultQueue(), message);
    }

    @Override
    public void send(String destination, QueueMessage message) throws ServiceException {
        String exchange = props.getExchange() == null ? "" : props.getExchange();
        String dest = (destination == null || destination.isEmpty()) ? props.getDefaultQueue() : destination;
        try {
            rabbitTemplate.convertAndSend(exchange, dest, message);
            log.info("Sent message to exchange='{}' queue='{}' id={}", exchange, dest, message.getId());
        } catch (Exception e) {
            log.error("Failed to send message id={}", message.getId(), e);
            throw ServiceException.of(DefaultExceptionReasonCodes.SERVER_ERROR, "Failed to send message.");
        }
    }

    @Override
    public Optional<QueueMessage> poll() {
        try {
            QueueMessage msg = (QueueMessage) rabbitTemplate.receiveAndConvert(props.getDefaultQueue(), props.getPollIntervalMs());
            return Optional.ofNullable(msg);
        } catch (Exception e) {
            log.error("Failed to poll destination={}", props.getDefaultQueue(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<QueueMessage> poll(String destination) {
        try {
            QueueMessage msg = (QueueMessage) rabbitTemplate.receiveAndConvert(destination, props.getPollIntervalMs());
            return Optional.ofNullable(msg);
        } catch (Exception e) {
            log.error("Failed to poll destination={}", destination, e);
            return Optional.empty();
        }
    }
}
