package com.corems.common.utils.db.spec;

import com.corems.common.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.persistence.criteria.Path;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Month;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("unchecked")
public class GenericSpecificationCastValueTest {

    private Object invokeCastValue(Class<?> javaType, String value) throws Exception {
        FilterRequest fr = new FilterRequest("field", FilterOperation.EQUALS, value);
        GenericSpecification<Object> spec = new GenericSpecification<>(fr);

        Path<?> path = Mockito.mock(Path.class);
        Mockito.when(path.getJavaType()).thenReturn((Class) javaType);

        Method m = GenericSpecification.class.getDeclaredMethod("castValue", Path.class, String.class);
        m.setAccessible(true);
        return m.invoke(spec, path, value);
    }

    @Test
    void castStringReturnsSame() throws Exception {
        Object r = invokeCastValue(String.class, "hello");
        assertThat(r).isInstanceOf(String.class).isEqualTo("hello");
    }

    @Test
    void castBooleanTrue() throws Exception {
        Object r = invokeCastValue(Boolean.class, "true");
        assertThat(r).isInstanceOf(Boolean.class).isEqualTo(Boolean.TRUE);
    }

    @Test
    void castUUIDValid() throws Exception {
        String s = "550e8400-e29b-41d4-a716-446655440000";
        Object r = invokeCastValue(UUID.class, s);
        assertThat(r).isInstanceOf(UUID.class).isEqualTo(UUID.fromString(s));
    }

    @Test
    void castUUIDInvalidThrowsServiceException() {
        assertThatThrownBy(() -> invokeCastValue(UUID.class, "not-a-uuid"))
                .hasRootCauseInstanceOf(ServiceException.class);
    }

    @Test
    void castOffsetDateTimeValid() throws Exception {
        String s = "2024-01-01T00:00:00Z";
        Object r = invokeCastValue(OffsetDateTime.class, s);
        assertThat(r).isInstanceOf(OffsetDateTime.class);
        OffsetDateTime odt = (OffsetDateTime) r;
        assertThat(odt.toInstant()).isEqualTo(Instant.parse(s));
    }

    @Test
    void castInstantValid() throws Exception {
        String s = "2024-01-01T00:00:00Z";
        Object r = invokeCastValue(Instant.class, s);
        assertThat(r).isInstanceOf(Instant.class).isEqualTo(Instant.parse(s));
    }

    @Test
    void castIntegerValid() throws Exception {
        Object r = invokeCastValue(Integer.class, "42");
        assertThat(r).isInstanceOf(Integer.class).isEqualTo(42);
    }

    @Test
    void castIntegerInvalidThrowsServiceException() {
        assertThatThrownBy(() -> invokeCastValue(Integer.class, "abc"))
                .hasRootCauseInstanceOf(ServiceException.class);
    }

    @Test
    void castEnumValid() throws Exception {
        Object r = invokeCastValue(Month.class, "JANUARY");
        assertThat(r).isInstanceOf(Month.class).isEqualTo(Month.JANUARY);
    }

    @Test
    void castEnumInvalidThrowsServiceException() {
        assertThatThrownBy(() -> invokeCastValue(Month.class, "NOTAMONTH"))
                .hasRootCauseInstanceOf(ServiceException.class);
    }
}
