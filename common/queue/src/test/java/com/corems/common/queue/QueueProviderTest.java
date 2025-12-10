package com.corems.common.queue;

import com.corems.common.exception.ServiceException;
import com.corems.common.queue.config.QueueProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueueProviderTest {

    private QueueProperties queueProperties;
    private QueueClient mockClient;
    private QueueProvider queueProvider;

    @BeforeEach
    void setUp() {
        queueProperties = mock(QueueProperties.class);
        mockClient = mock(QueueClient.class);
        queueProvider = new QueueProvider(queueProperties);
    }

    @Test
    void constructor_WithNullProperties_ThrowsException() {
        assertThrows(NullPointerException.class, () -> new QueueProvider(null));
    }

    @Test
    void constructor_WithValidProperties_CreatesProvider() {
        QueueProvider provider = new QueueProvider(queueProperties);
        assertNotNull(provider);
        assertEquals(queueProperties, provider.getQueueProperties());
    }

    @Test
    void isEnabled_WhenDisabledByConfig_ReturnsFalse() {
        when(queueProperties.isEnabled()).thenReturn(false);
        
        assertFalse(queueProvider.isEnabled());
    }

    @Test
    void isEnabled_WhenEnabledButNoProviders_ReturnsFalse() {
        when(queueProperties.isEnabled()).thenReturn(true);
        
        assertFalse(queueProvider.isEnabled());
    }

    @Test
    void isEnabled_WhenEnabledWithProviders_ReturnsTrue() {
        when(queueProperties.isEnabled()).thenReturn(true);
        queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, mockClient);
        
        assertTrue(queueProvider.isEnabled());
    }

    @Test
    void registerProvider_WithValidInputs_RegistersSuccessfully() {
        queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, mockClient);
        
        QueueClient retrievedClient = queueProvider.getProvider(SupportedQueueProvider.RABBIT_MQ);
        assertEquals(mockClient, retrievedClient);
    }

    @Test
    void registerProvider_WithNullProviderType_ThrowsException() {
        assertThrows(NullPointerException.class, 
            () -> queueProvider.registerProvider(null, mockClient));
    }

    @Test
    void registerProvider_WithNullClient_ThrowsException() {
        assertThrows(NullPointerException.class, 
            () -> queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, null));
    }

    @Test
    void getProvider_WithRegisteredProvider_ReturnsClient() {
        queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, mockClient);
        
        QueueClient result = queueProvider.getProvider(SupportedQueueProvider.RABBIT_MQ);
        assertEquals(mockClient, result);
    }

    @Test
    void getProvider_WithUnregisteredProvider_ThrowsServiceException() {
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> queueProvider.getProvider(SupportedQueueProvider.RABBIT_MQ));
        
        assertTrue(exception.getMessage().contains("No QueueClient registered for provider"));
        assertTrue(exception.getMessage().contains("RABBIT_MQ"));
    }

    @Test
    void getDefaultClient_WhenDisabled_ThrowsServiceException() {
        when(queueProperties.isEnabled()).thenReturn(false);
        
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> queueProvider.getDefaultClient());
        
        assertTrue(exception.getMessage().contains("Queueing is disabled by configuration"));
    }

    @Test
    void getDefaultClient_WhenEnabledButNoProviders_ThrowsServiceException() {
        when(queueProperties.isEnabled()).thenReturn(true);
        
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> queueProvider.getDefaultClient());
        
        assertTrue(exception.getMessage().contains("Queueing is disabled by configuration"));
    }

    @Test
    void getDefaultClient_WithNullProvider_ThrowsServiceException() {
        when(queueProperties.isEnabled()).thenReturn(true);
        when(queueProperties.getProvider()).thenReturn(null);
        queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, mockClient);
        
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> queueProvider.getDefaultClient());
        
        assertTrue(exception.getMessage().contains("Queue provider is not configured"));
    }

    @Test
    void getDefaultClient_WithValidConfiguration_ReturnsClient() {
        when(queueProperties.isEnabled()).thenReturn(true);
        when(queueProperties.getProvider()).thenReturn(SupportedQueueProvider.RABBIT_MQ);
        queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, mockClient);
        
        QueueClient result = queueProvider.getDefaultClient();
        assertEquals(mockClient, result);
    }

    @Test
    void getDefaultClient_WithUnregisteredDefaultProvider_ThrowsServiceException() {
        when(queueProperties.isEnabled()).thenReturn(true);
        when(queueProperties.getProvider()).thenReturn(SupportedQueueProvider.RABBIT_MQ);
        // Don't register any provider - this will cause isEnabled() to return false
        // and throw "Queueing is disabled by configuration" instead
        
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> queueProvider.getDefaultClient());
        
        // Since no providers are registered, isEnabled() returns false and throws different message
        assertTrue(exception.getMessage().contains("Queueing is disabled by configuration"));
    }

    @Test
    void registerProvider_OverwritesPreviousRegistration() {
        QueueClient firstClient = mock(QueueClient.class);
        QueueClient secondClient = mock(QueueClient.class);
        
        queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, firstClient);
        assertEquals(firstClient, queueProvider.getProvider(SupportedQueueProvider.RABBIT_MQ));
        
        queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, secondClient);
        assertEquals(secondClient, queueProvider.getProvider(SupportedQueueProvider.RABBIT_MQ));
    }

    @Test
    void multipleProviders_CanBeRegistered() {
        // Even though only RABBIT_MQ exists now, test the pattern for future providers
        when(queueProperties.isEnabled()).thenReturn(true);
        queueProvider.registerProvider(SupportedQueueProvider.RABBIT_MQ, mockClient);
        
        assertEquals(mockClient, queueProvider.getProvider(SupportedQueueProvider.RABBIT_MQ));
        assertTrue(queueProvider.isEnabled());
    }

    @Test
    void getQueueProperties_ReturnsOriginalProperties() {
        assertEquals(queueProperties, queueProvider.getQueueProperties());
    }
}