package com.corems.common.queue.clients;

import com.corems.common.exception.ServiceException;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.config.QueueProperties;

import lombok.Getter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;

class RabbitMqClientTest {

    private RabbitTemplate mockRabbitTemplate;
    private QueueProperties.RabbitMqProperties properties;
    private RabbitMqClient rabbitMqClient;

    @BeforeEach
    void setUp() {
        mockRabbitTemplate = mock(RabbitTemplate.class);
        properties = new QueueProperties.RabbitMqProperties();
        properties.setHost("localhost");
        properties.setPort(5672);
        properties.setDefaultQueue("test-queue");
        properties.setExchange("test-exchange");
        properties.setPollIntervalMs(1000L);
        
        rabbitMqClient = new RabbitMqClient(mockRabbitTemplate, properties);
    }

    @Test
    void validate_WithValidProperties_DoesNotThrow() {
        QueueProperties.RabbitMqProperties validProps = new QueueProperties.RabbitMqProperties();
        validProps.setHost("localhost");
        validProps.setPort(5672);
        validProps.setDefaultQueue("test-queue");
        
        assertDoesNotThrow(() -> RabbitMqClient.validate(validProps));
    }

    @Test
    void validate_WithNullProperties_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> RabbitMqClient.validate(null));
        
        assertTrue(exception.getMessage().contains("RabbitMQ provider config missing"));
    }

    @Test
    void validate_WithNullHost_ThrowsException() {
        QueueProperties.RabbitMqProperties props = new QueueProperties.RabbitMqProperties();
        props.setHost(null);
        props.setDefaultQueue("test-queue");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> RabbitMqClient.validate(props));
        
        assertTrue(exception.getMessage().contains("RabbitMQ host missing"));
    }

    @Test
    void validate_WithBlankHost_ThrowsException() {
        QueueProperties.RabbitMqProperties props = new QueueProperties.RabbitMqProperties();
        props.setHost("   ");
        props.setDefaultQueue("test-queue");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> RabbitMqClient.validate(props));
        
        assertTrue(exception.getMessage().contains("RabbitMQ host missing"));
    }

    @Test
    void validate_WithInvalidPort_ThrowsException() {
        QueueProperties.RabbitMqProperties props = new QueueProperties.RabbitMqProperties();
        props.setHost("localhost");
        props.setPort(-1);
        props.setDefaultQueue("test-queue");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> RabbitMqClient.validate(props));
        
        assertTrue(exception.getMessage().contains("RabbitMQ port is invalid"));
    }

    @Test
    void validate_WithPortTooHigh_ThrowsException() {
        QueueProperties.RabbitMqProperties props = new QueueProperties.RabbitMqProperties();
        props.setHost("localhost");
        props.setPort(65536);
        props.setDefaultQueue("test-queue");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> RabbitMqClient.validate(props));
        
        assertTrue(exception.getMessage().contains("RabbitMQ port is invalid"));
    }

    @Test
    void validate_WithNoDefaultQueueOrRequiredQueues_ThrowsException() {
        QueueProperties.RabbitMqProperties props = new QueueProperties.RabbitMqProperties();
        props.setHost("localhost");
        props.setPort(5672);
        // No default queue or required queues
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> RabbitMqClient.validate(props));
        
        assertTrue(exception.getMessage().contains("must define either defaultQueue or requiredQueues"));
    }

    @Test
    void validate_WithRequiredQueuesOnly_DoesNotThrow() {
        QueueProperties.RabbitMqProperties props = new QueueProperties.RabbitMqProperties();
        props.setHost("localhost");
        props.setPort(5672);
        props.setRequiredQueues(List.of("queue1", "queue2"));
        
        assertDoesNotThrow(() -> RabbitMqClient.validate(props));
    }

    @Test
    void getProperties_ReturnsCorrectProperties() {
        assertEquals(properties, rabbitMqClient.getProperties());
    }

    @Test
    void send_WithDefaultQueue_CallsRabbitTemplate() {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        message.setType("TEST_MESSAGE");
        
        rabbitMqClient.send(message);
        
        verify(mockRabbitTemplate).convertAndSend(eq("test-exchange"), eq("test-queue"), eq(message));
    }

    @Test
    void send_WithSpecificDestination_CallsRabbitTemplate() {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        String destination = "specific-queue";
        
        rabbitMqClient.send(destination, message);
        
        verify(mockRabbitTemplate).convertAndSend(eq("test-exchange"), eq(destination), eq(message));
    }

    @Test
    void send_WithNullDestination_UsesDefaultQueue() {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        
        rabbitMqClient.send(null, message);
        
        verify(mockRabbitTemplate).convertAndSend(eq("test-exchange"), eq("test-queue"), eq(message));
    }

    @Test
    void send_WithEmptyDestination_UsesDefaultQueue() {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        
        rabbitMqClient.send("", message);
        
        verify(mockRabbitTemplate).convertAndSend(eq("test-exchange"), eq("test-queue"), eq(message));
    }

    @Test
    void send_WithNoExchange_UsesEmptyExchange() {
        properties.setExchange(null);
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        
        rabbitMqClient.send(message);
        
        verify(mockRabbitTemplate).convertAndSend(eq(""), eq("test-queue"), eq(message));
    }

    @Test
    void send_WhenRabbitTemplateThrows_ThrowsServiceException() {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        
        doThrow(new RuntimeException("Connection failed")).when(mockRabbitTemplate)
            .convertAndSend(anyString(), anyString(), any(QueueMessage.class));
        
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> rabbitMqClient.send(message));
        
        assertTrue(exception.getErrors().get(0).getDetails().contains("Failed to send message"));
    }

    @Test
    void poll_WithDefaultQueue_CallsRabbitTemplate() {
        QueueMessage expectedMessage = new QueueMessage();
        expectedMessage.setId("received-id");
        
        when(mockRabbitTemplate.receiveAndConvert("test-queue", 1000L))
            .thenReturn(expectedMessage);
        
        Optional<QueueMessage> result = rabbitMqClient.poll();
        
        assertTrue(result.isPresent());
        assertEquals(expectedMessage, result.get());
        verify(mockRabbitTemplate).receiveAndConvert("test-queue", 1000L);
    }

    @Test
    void poll_WithSpecificDestination_CallsRabbitTemplate() {
        QueueMessage expectedMessage = new QueueMessage();
        String destination = "specific-queue";
        
        when(mockRabbitTemplate.receiveAndConvert(destination, 1000L))
            .thenReturn(expectedMessage);
        
        Optional<QueueMessage> result = rabbitMqClient.poll(destination);
        
        assertTrue(result.isPresent());
        assertEquals(expectedMessage, result.get());
        verify(mockRabbitTemplate).receiveAndConvert(destination, 1000L);
    }

    @Test
    void poll_WhenNoMessage_ReturnsEmpty() {
        when(mockRabbitTemplate.receiveAndConvert("test-queue", 1000L))
            .thenReturn(null);
        
        Optional<QueueMessage> result = rabbitMqClient.poll();
        
        assertTrue(result.isEmpty());
    }

    @Test
    void poll_WhenRabbitTemplateThrows_ReturnsEmpty() {
        when(mockRabbitTemplate.receiveAndConvert("test-queue", 1000L))
            .thenThrow(new RuntimeException("Connection failed"));
        
        Optional<QueueMessage> result = rabbitMqClient.poll();
        
        assertTrue(result.isEmpty());
    }

    @Test
    void poll_WithDestination_WhenRabbitTemplateThrows_ReturnsEmpty() {
        String destination = "specific-queue";
        when(mockRabbitTemplate.receiveAndConvert(destination, 1000L))
            .thenThrow(new RuntimeException("Connection failed"));
        
        Optional<QueueMessage> result = rabbitMqClient.poll(destination);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void constructor_WithValidInputs_CreatesClient() {
        RabbitMqClient client = new RabbitMqClient(mockRabbitTemplate, properties);
        
        assertNotNull(client);
        assertEquals(properties, client.getProperties());
    }

    @Test
    void send_WithComplexMessage_HandlesCorrectly() {
        QueueMessage message = new QueueMessage();
        message.setId("complex-id");
        message.setType("USER_CREATED");
        message.setPayload(new TestPayload("John Doe", 30));
        message.setAttempts(1);
        
        rabbitMqClient.send("user-events", message);
        
        verify(mockRabbitTemplate).convertAndSend(eq("test-exchange"), eq("user-events"), eq(message));
    }

    @Getter
    private static class TestPayload {
        private final String name;
        private final int age;
        
        public TestPayload(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
}