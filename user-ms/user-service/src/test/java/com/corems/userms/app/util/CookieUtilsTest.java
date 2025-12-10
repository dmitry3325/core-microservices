package com.corems.userms.app.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookieUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private Cookie[] cookies;

    @BeforeEach
    void setUp() {
        cookies = new Cookie[]{
                new Cookie("sessionId", "abc123"),
                new Cookie("userId", "user456"),
                new Cookie("theme", "dark")
        };
    }

    @Test
    void getCookie_WhenCookieExists_ShouldReturnCookie() {
        // Given
        when(request.getCookies()).thenReturn(cookies);

        // When
        Optional<Cookie> result = CookieUtils.getCookie(request, "sessionId");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("sessionId");
        assertThat(result.get().getValue()).isEqualTo("abc123");
    }

    @Test
    void getCookie_WhenCookieNotExists_ShouldReturnEmpty() {
        // Given
        when(request.getCookies()).thenReturn(cookies);

        // When
        Optional<Cookie> result = CookieUtils.getCookie(request, "nonExistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCookie_WhenNoCookies_ShouldReturnEmpty() {
        // Given
        when(request.getCookies()).thenReturn(null);

        // When
        Optional<Cookie> result = CookieUtils.getCookie(request, "sessionId");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getCookie_WhenEmptyCookieArray_ShouldReturnEmpty() {
        // Given
        when(request.getCookies()).thenReturn(new Cookie[0]);

        // When
        Optional<Cookie> result = CookieUtils.getCookie(request, "sessionId");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void addCookie_WhenCalled_ShouldAddCookieWithCorrectProperties() {
        // When
        CookieUtils.addCookie(response, "testCookie", "testValue", 3600);

        // Then
        verify(response).addCookie(argThat(cookie -> 
            cookie.getName().equals("testCookie") &&
            cookie.getValue().equals("testValue") &&
            cookie.getPath().equals("/") &&
            cookie.isHttpOnly() &&
            cookie.getMaxAge() == 3600
        ));
    }

    @Test
    void deleteCookie_WhenCookieExists_ShouldDeleteCookie() {
        // Given
        when(request.getCookies()).thenReturn(cookies);

        // When
        CookieUtils.deleteCookie(request, response, "sessionId");

        // Then
        verify(response).addCookie(argThat(cookie -> 
            cookie.getName().equals("sessionId") &&
            cookie.getValue().equals("") &&
            cookie.getPath().equals("/") &&
            cookie.getMaxAge() == 0
        ));
    }

    @Test
    void deleteCookie_WhenCookieNotExists_ShouldNotAddCookie() {
        // Given
        when(request.getCookies()).thenReturn(cookies);

        // When
        CookieUtils.deleteCookie(request, response, "nonExistent");

        // Then
        verify(response, never()).addCookie(any());
    }

    @Test
    void deleteCookie_WhenNoCookies_ShouldNotAddCookie() {
        // Given
        when(request.getCookies()).thenReturn(null);

        // When
        CookieUtils.deleteCookie(request, response, "sessionId");

        // Then
        verify(response, never()).addCookie(any());
    }

    @Test
    void serialize_WhenValidObject_ShouldReturnBase64String() {
        // Given
        TestSerializableObject obj = new TestSerializableObject("test", 123);

        // When
        String result = CookieUtils.serialize(obj);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        // Base64 encoding may contain padding characters, so we just check it's valid Base64
        assertThat(result).matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    @Test
    void deserialize_WhenValidSerializedObject_ShouldReturnObject() {
        // Given
        TestSerializableObject original = new TestSerializableObject("test", 123);
        String serialized = CookieUtils.serialize(original);
        Cookie cookie = new Cookie("test", serialized);

        // When
        TestSerializableObject result = CookieUtils.deserialize(cookie, TestSerializableObject.class);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test");
        assertThat(result.getValue()).isEqualTo(123);
    }

    @Test
    void serialize_WhenNullObject_ShouldReturnValidBase64() {
        // When - Serializing null should work and return a valid Base64 string
        String result = CookieUtils.serialize(null);
        
        // Then - Should return a valid Base64 string representing null
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
        assertThat(result).matches("^[A-Za-z0-9+/]*={0,2}$");
    }

    @Test
    void deserialize_WhenInvalidBase64_ShouldThrowException() {
        // Given
        Cookie invalidCookie = new Cookie("test", "invalid-base64!");

        // When & Then
        assertThatThrownBy(() -> CookieUtils.deserialize(invalidCookie, TestSerializableObject.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Illegal base64 character");
    }

    @Test
    void serializeDeserialize_RoundTrip_ShouldPreserveData() {
        // Given
        TestSerializableObject original = new TestSerializableObject("roundTrip", 999);

        // When
        String serialized = CookieUtils.serialize(original);
        Cookie cookie = new Cookie("test", serialized);
        TestSerializableObject deserialized = CookieUtils.deserialize(cookie, TestSerializableObject.class);

        // Then
        assertThat(deserialized.getName()).isEqualTo(original.getName());
        assertThat(deserialized.getValue()).isEqualTo(original.getValue());
    }

    // Test helper class
    private static class TestSerializableObject implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String name;
        private final int value;

        public TestSerializableObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}