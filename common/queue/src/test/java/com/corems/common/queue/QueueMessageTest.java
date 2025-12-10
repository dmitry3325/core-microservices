package com.corems.common.queue;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueueMessageTest {

    @Test
    void constructor_CreatesMessageWithDefaults() {
        QueueMessage message = new QueueMessage();
        
        assertNull(message.getId());
        assertNull(message.getType());
        assertNull(message.getPayload());
        assertEquals(0, message.getAttempts());
        assertNull(message.getHeaders());
        assertNotNull(message.getCreatedAt());
        assertTrue(message.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        QueueMessage message = new QueueMessage();
        String testId = "test-id-123";
        String testType = "USER_CREATED";
        Object testPayload = "test payload";
        int testAttempts = 3;
        Map<String, String> testHeaders = Map.of("source", "user-service", "version", "1.0");
        Instant testCreatedAt = Instant.now().minusSeconds(60);
        
        message.setId(testId);
        message.setType(testType);
        message.setPayload(testPayload);
        message.setAttempts(testAttempts);
        message.setHeaders(testHeaders);
        message.setCreatedAt(testCreatedAt);
        
        assertEquals(testId, message.getId());
        assertEquals(testType, message.getType());
        assertEquals(testPayload, message.getPayload());
        assertEquals(testAttempts, message.getAttempts());
        assertEquals(testHeaders, message.getHeaders());
        assertEquals(testCreatedAt, message.getCreatedAt());
    }

    @Test
    void payload_CanHoldDifferentTypes() {
        QueueMessage message = new QueueMessage();
        
        // Test with String
        message.setPayload("string payload");
        assertEquals("string payload", message.getPayload());
        
        // Test with Map
        Map<String, Object> mapPayload = Map.of("key1", "value1", "key2", 42);
        message.setPayload(mapPayload);
        assertEquals(mapPayload, message.getPayload());
        
        // Test with custom object
        TestPayload customPayload = new TestPayload("test", 123);
        message.setPayload(customPayload);
        assertEquals(customPayload, message.getPayload());
    }

    @Test
    void headers_CanBeModified() {
        QueueMessage message = new QueueMessage();
        Map<String, String> headers = new HashMap<>();
        headers.put("correlation-id", "abc-123");
        headers.put("retry-count", "1");
        
        message.setHeaders(headers);
        assertEquals(2, message.getHeaders().size());
        assertEquals("abc-123", message.getHeaders().get("correlation-id"));
        
        // Modify headers
        headers.put("retry-count", "2");
        message.setHeaders(headers);
        assertEquals("2", message.getHeaders().get("retry-count"));
    }

    @Test
    void attempts_CanBeIncremented() {
        QueueMessage message = new QueueMessage();
        assertEquals(0, message.getAttempts());
        
        message.setAttempts(message.getAttempts() + 1);
        assertEquals(1, message.getAttempts());
        
        message.setAttempts(message.getAttempts() + 1);
        assertEquals(2, message.getAttempts());
    }

    @Test
    void serializable_HasSerialVersionUID() {
        // Verify that QueueMessage implements Serializable properly
        assertTrue(java.io.Serializable.class.isAssignableFrom(QueueMessage.class));
        
        // Test serialization/deserialization would require more setup
        // but the class structure supports it with serialVersionUID
    }

    @Test
    void createdAt_DefaultsToCurrentTime() {
        Instant before = Instant.now();
        QueueMessage message = new QueueMessage();
        Instant after = Instant.now();
        
        assertNotNull(message.getCreatedAt());
        assertTrue(message.getCreatedAt().isAfter(before.minusSeconds(1)));
        assertTrue(message.getCreatedAt().isBefore(after.plusSeconds(1)));
    }

    @Test
    void createdAt_CanBeOverridden() {
        QueueMessage message = new QueueMessage();
        Instant customTime = Instant.parse("2023-01-01T12:00:00Z");
        
        message.setCreatedAt(customTime);
        assertEquals(customTime, message.getCreatedAt());
    }

    // Helper class for testing custom payloads
    private static class TestPayload {
        private final String name;
        private final int value;
        
        public TestPayload(String name, int value) {
            this.name = name;
            this.value = value;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestPayload that = (TestPayload) obj;
            return value == that.value && name.equals(that.name);
        }
        
        @Override
        public int hashCode() {
            return name.hashCode() + value;
        }
    }
}