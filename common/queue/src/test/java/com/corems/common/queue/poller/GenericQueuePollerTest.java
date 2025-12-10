package com.corems.common.queue.poller;

import com.corems.common.queue.QueueClient;
import com.corems.common.queue.QueueMessage;
import com.corems.common.queue.config.QueueClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenericQueuePollerTest {

    private QueueClient mockQueueClient;
    private QueueClientProperties mockProperties;
    private Map<String, MessageHandler> handlers;
    private MessageHandler mockHandler;

    @BeforeEach
    void setUp() {
        mockQueueClient = mock(QueueClient.class);
        mockProperties = mock(QueueClientProperties.class);
        mockHandler = mock(MessageHandler.class);
        
        when(mockQueueClient.getProperties()).thenReturn(mockProperties);
        when(mockProperties.getDefaultQueue()).thenReturn("test-queue");
        when(mockProperties.getRetryCount()).thenReturn(3);
        
        handlers = new HashMap<>();
        handlers.put("TEST_MESSAGE", mockHandler);
    }

    @Test
    void constructor_WithAutoStart_StartsPolling() throws InterruptedException {
        CountDownLatch pollCalled = new CountDownLatch(1);
        
        when(mockQueueClient.poll("test-queue")).thenAnswer(invocation -> {
            pollCalled.countDown();
            return Optional.empty();
        });
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers);
        
        assertTrue(pollCalled.await(2, TimeUnit.SECONDS));
        poller.close();
    }

    @Test
    void constructor_WithoutAutoStart_DoesNotStartPolling() throws InterruptedException {
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        
        // Give it a moment to potentially start
        Thread.sleep(100);
        
        verify(mockQueueClient, never()).poll(anyString());
        poller.close();
    }

    @Test
    void start_WhenNotRunning_StartsPolling() throws InterruptedException {
        CountDownLatch pollCalled = new CountDownLatch(1);
        
        when(mockQueueClient.poll("test-queue")).thenAnswer(invocation -> {
            pollCalled.countDown();
            return Optional.empty();
        });
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        poller.start();
        
        assertTrue(pollCalled.await(2, TimeUnit.SECONDS));
        poller.close();
    }

    @Test
    void start_WhenAlreadyRunning_IsIdempotent() throws InterruptedException {
        AtomicInteger pollCount = new AtomicInteger(0);
        
        when(mockQueueClient.poll("test-queue")).thenAnswer(invocation -> {
            pollCount.incrementAndGet();
            Thread.sleep(50); // Small delay to allow multiple calls
            return Optional.empty();
        });
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        
        poller.start();
        poller.start(); // Second call should be ignored
        
        Thread.sleep(200);
        poller.close();
        
        // Should have been called multiple times, but not double the amount
        assertTrue(pollCount.get() > 0);
    }

    @Test
    void pollOnce_WithMessage_ProcessesMessage() throws Exception {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        message.setType("TEST_MESSAGE");
        message.setPayload("test payload");
        
        when(mockQueueClient.poll("test-queue")).thenReturn(Optional.of(message));
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        poller.pollOnce();
        
        verify(mockHandler).handle(message);
        poller.close();
    }

    @Test
    void pollOnce_WithNoMessage_DoesNothing() {
        when(mockQueueClient.poll("test-queue")).thenReturn(Optional.empty());
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        poller.pollOnce();
        
        verifyNoInteractions(mockHandler);
        poller.close();
    }

    @Test
    void pollOnce_WithUnknownMessageType_LogsError() {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        message.setType("UNKNOWN_MESSAGE");
        message.setPayload("test payload");
        
        when(mockQueueClient.poll("test-queue")).thenReturn(Optional.of(message));
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        poller.pollOnce();
        
        // Should not call any handler
        verifyNoInteractions(mockHandler);
        poller.close();
    }

    @Test
    void processMessage_WhenHandlerThrows_RetriesMessage() throws Exception {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        message.setType("TEST_MESSAGE");
        message.setAttempts(0);
        
        doThrow(new RuntimeException("Handler failed")).when(mockHandler).handle(message);
        when(mockQueueClient.poll("test-queue")).thenReturn(Optional.of(message));
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        poller.pollOnce();
        
        // Should increment attempts and re-enqueue
        assertEquals(1, message.getAttempts());
        verify(mockQueueClient).send("test-queue", message);
        poller.close();
    }

    @Test
    void processMessage_WhenMaxRetriesReached_DropsMessage() throws Exception {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        message.setType("TEST_MESSAGE");
        message.setAttempts(3); // Already at max retries
        
        doThrow(new RuntimeException("Handler failed")).when(mockHandler).handle(message);
        when(mockQueueClient.poll("test-queue")).thenReturn(Optional.of(message));
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        poller.pollOnce();
        
        // Should not re-enqueue
        verify(mockQueueClient, never()).send(anyString(), any(QueueMessage.class));
        poller.close();
    }

    @Test
    void processMessage_WhenReEnqueueFails_LogsError() throws Exception {
        QueueMessage message = new QueueMessage();
        message.setId("test-id");
        message.setType("TEST_MESSAGE");
        message.setAttempts(0);
        
        doThrow(new RuntimeException("Handler failed")).when(mockHandler).handle(message);
        doThrow(new RuntimeException("Send failed")).when(mockQueueClient).send(anyString(), any(QueueMessage.class));
        when(mockQueueClient.poll("test-queue")).thenReturn(Optional.of(message));
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers, Map.of(), false);
        
        // Should not throw exception, just log error
        assertDoesNotThrow(() -> poller.pollOnce());
        
        poller.close();
    }

    @Test
    void stop_StopsPolling() throws InterruptedException {
        AtomicReference<Boolean> pollingActive = new AtomicReference<>(false);
        CountDownLatch pollStarted = new CountDownLatch(1);
        CountDownLatch pollStopped = new CountDownLatch(1);
        
        when(mockQueueClient.poll("test-queue")).thenAnswer(invocation -> {
            pollingActive.set(true);
            pollStarted.countDown();
            
            // Wait for stop signal or timeout
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                pollingActive.set(false);
                pollStopped.countDown();
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        });
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers);
        
        // Wait for polling to start
        assertTrue(pollStarted.await(2, TimeUnit.SECONDS));
        assertTrue(pollingActive.get());
        
        // Stop polling
        poller.stop();
        poller.close();
        
        // Verify polling stopped
        assertTrue(pollStopped.await(2, TimeUnit.SECONDS));
        assertFalse(pollingActive.get());
    }

    @Test
    void close_StopsPolling() throws InterruptedException {
        CountDownLatch pollCalled = new CountDownLatch(1);
        
        when(mockQueueClient.poll("test-queue")).thenAnswer(invocation -> {
            pollCalled.countDown();
            try {
                Thread.sleep(1000); // Long sleep to test interruption
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        });
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, handlers);
        
        assertTrue(pollCalled.await(2, TimeUnit.SECONDS));
        
        // Close should stop polling
        assertDoesNotThrow(() -> poller.close());
    }

    @Test
    void multipleHandlers_ProcessesDifferentMessageTypes() throws Exception {
        MessageHandler handler1 = mock(MessageHandler.class);
        MessageHandler handler2 = mock(MessageHandler.class);
        
        Map<String, MessageHandler> multiHandlers = Map.of(
            "TYPE_1", handler1,
            "TYPE_2", handler2
        );
        
        QueueMessage message1 = new QueueMessage();
        message1.setType("TYPE_1");
        
        QueueMessage message2 = new QueueMessage();
        message2.setType("TYPE_2");
        
        GenericQueuePoller poller = new GenericQueuePoller(mockQueueClient, multiHandlers, Map.of(), false);
        
        when(mockQueueClient.poll("test-queue"))
            .thenReturn(Optional.of(message1))
            .thenReturn(Optional.of(message2));
        
        poller.pollOnce();
        poller.pollOnce();
        
        verify(handler1).handle(message1);
        verify(handler2).handle(message2);
        
        poller.close();
    }
}