package com.corems.common.queue.config;

import com.corems.common.queue.SupportedQueueProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueuePropertiesTest {

    @Test
    void defaultValues_AreSetCorrectly() {
        QueueProperties properties = new QueueProperties();
        
        assertFalse(properties.isEnabled());
        assertEquals(SupportedQueueProvider.RABBIT_MQ, properties.getProvider());
        assertNotNull(properties.getProviders());
        assertNotNull(properties.getProviders().getRabbitMq());
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        QueueProperties properties = new QueueProperties();
        
        properties.setEnabled(true);
        properties.setProvider(SupportedQueueProvider.RABBIT_MQ);
        
        assertTrue(properties.isEnabled());
        assertEquals(SupportedQueueProvider.RABBIT_MQ, properties.getProvider());
    }

    @Test
    void providers_CanBeModified() {
        QueueProperties properties = new QueueProperties();
        QueueProperties.Providers providers = new QueueProperties.Providers();
        
        properties.setProviders(providers);
        assertEquals(providers, properties.getProviders());
    }

    @Test
    void rabbitMqProperties_DefaultValues() {
        QueueProperties.RabbitMqProperties rabbitProps = new QueueProperties.RabbitMqProperties();
        
        assertEquals("localhost", rabbitProps.getHost());
        assertEquals(5672, rabbitProps.getPort());
        assertEquals("", rabbitProps.getUsername());
        assertEquals("", rabbitProps.getPassword());
        assertEquals("", rabbitProps.getExchange());
        assertEquals("", rabbitProps.getDefaultQueue());
        assertNotNull(rabbitProps.getRequiredQueues());
        assertTrue(rabbitProps.getRequiredQueues().isEmpty());
        assertEquals(1000L, rabbitProps.getPollIntervalMs());
        assertEquals(1, rabbitProps.getRetryCount());
    }

    @Test
    void rabbitMqProperties_SettersAndGetters() {
        QueueProperties.RabbitMqProperties rabbitProps = new QueueProperties.RabbitMqProperties();
        
        rabbitProps.setHost("rabbitmq.example.com");
        rabbitProps.setPort(5673);
        rabbitProps.setUsername("testuser");
        rabbitProps.setPassword("testpass");
        rabbitProps.setExchange("test-exchange");
        rabbitProps.setDefaultQueue("test-queue");
        rabbitProps.setRequiredQueues(List.of("queue1", "queue2"));
        rabbitProps.setPollIntervalMs(2000L);
        rabbitProps.setRetryCount(5);
        
        assertEquals("rabbitmq.example.com", rabbitProps.getHost());
        assertEquals(5673, rabbitProps.getPort());
        assertEquals("testuser", rabbitProps.getUsername());
        assertEquals("testpass", rabbitProps.getPassword());
        assertEquals("test-exchange", rabbitProps.getExchange());
        assertEquals("test-queue", rabbitProps.getDefaultQueue());
        assertEquals(List.of("queue1", "queue2"), rabbitProps.getRequiredQueues());
        assertEquals(2000L, rabbitProps.getPollIntervalMs());
        assertEquals(5, rabbitProps.getRetryCount());
    }

    @Test
    void rabbitMqProperties_ImplementsQueueClientProperties() {
        QueueProperties.RabbitMqProperties rabbitProps = new QueueProperties.RabbitMqProperties();
        
        assertTrue(rabbitProps instanceof QueueClientProperties);
    }

    @Test
    void providers_DefaultRabbitMqProperties() {
        QueueProperties.Providers providers = new QueueProperties.Providers();
        
        assertNotNull(providers.getRabbitMq());
        assertEquals("localhost", providers.getRabbitMq().getHost());
        assertEquals(5672, providers.getRabbitMq().getPort());
    }

    @Test
    void providers_SetRabbitMqProperties() {
        QueueProperties.Providers providers = new QueueProperties.Providers();
        QueueProperties.RabbitMqProperties customRabbitProps = new QueueProperties.RabbitMqProperties();
        customRabbitProps.setHost("custom-host");
        
        providers.setRabbitMq(customRabbitProps);
        
        assertEquals(customRabbitProps, providers.getRabbitMq());
        assertEquals("custom-host", providers.getRabbitMq().getHost());
    }

    @Test
    void queueProperties_ConfigurationPropertiesAnnotation() {
        // Verify that the class has the correct configuration properties prefix
        // This is important for Spring Boot auto-configuration
        assertTrue(QueueProperties.class.isAnnotationPresent(org.springframework.boot.context.properties.ConfigurationProperties.class));
        
        org.springframework.boot.context.properties.ConfigurationProperties annotation = 
            QueueProperties.class.getAnnotation(org.springframework.boot.context.properties.ConfigurationProperties.class);
        
        assertEquals("queue", annotation.prefix());
    }

    @Test
    void queueProperties_ValidationAnnotation() {
        // Verify that the class has validation annotation
        assertTrue(QueueProperties.class.isAnnotationPresent(org.springframework.validation.annotation.Validated.class));
    }

    @Test
    void rabbitMqProperties_RequiredQueues_CanBeEmpty() {
        QueueProperties.RabbitMqProperties rabbitProps = new QueueProperties.RabbitMqProperties();
        
        assertTrue(rabbitProps.getRequiredQueues().isEmpty());
        
        // Should be able to add queues
        rabbitProps.getRequiredQueues().add("new-queue");
        assertEquals(1, rabbitProps.getRequiredQueues().size());
        assertEquals("new-queue", rabbitProps.getRequiredQueues().get(0));
    }

    @Test
    void rabbitMqProperties_RequiredQueues_CanBeReplaced() {
        QueueProperties.RabbitMqProperties rabbitProps = new QueueProperties.RabbitMqProperties();
        List<String> newQueues = List.of("queue-a", "queue-b", "queue-c");
        
        rabbitProps.setRequiredQueues(newQueues);
        
        assertEquals(3, rabbitProps.getRequiredQueues().size());
        assertEquals(newQueues, rabbitProps.getRequiredQueues());
    }

    @Test
    void integrationTest_FullConfiguration() {
        QueueProperties properties = new QueueProperties();
        
        // Configure main properties
        properties.setEnabled(true);
        properties.setProvider(SupportedQueueProvider.RABBIT_MQ);
        
        // Configure RabbitMQ properties
        QueueProperties.RabbitMqProperties rabbitProps = properties.getProviders().getRabbitMq();
        rabbitProps.setHost("prod-rabbitmq.example.com");
        rabbitProps.setPort(5672);
        rabbitProps.setUsername("prod-user");
        rabbitProps.setPassword("prod-password");
        rabbitProps.setExchange("core-exchange");
        rabbitProps.setDefaultQueue("default-queue");
        rabbitProps.setRequiredQueues(List.of("user-events", "notification-events", "audit-events"));
        rabbitProps.setPollIntervalMs(500L);
        rabbitProps.setRetryCount(3);
        
        // Verify configuration
        assertTrue(properties.isEnabled());
        assertEquals(SupportedQueueProvider.RABBIT_MQ, properties.getProvider());
        assertEquals("prod-rabbitmq.example.com", properties.getProviders().getRabbitMq().getHost());
        assertEquals(5672, properties.getProviders().getRabbitMq().getPort());
        assertEquals("prod-user", properties.getProviders().getRabbitMq().getUsername());
        assertEquals("prod-password", properties.getProviders().getRabbitMq().getPassword());
        assertEquals("core-exchange", properties.getProviders().getRabbitMq().getExchange());
        assertEquals("default-queue", properties.getProviders().getRabbitMq().getDefaultQueue());
        assertEquals(3, properties.getProviders().getRabbitMq().getRequiredQueues().size());
        assertTrue(properties.getProviders().getRabbitMq().getRequiredQueues().contains("user-events"));
        assertEquals(500L, properties.getProviders().getRabbitMq().getPollIntervalMs());
        assertEquals(3, properties.getProviders().getRabbitMq().getRetryCount());
    }
}