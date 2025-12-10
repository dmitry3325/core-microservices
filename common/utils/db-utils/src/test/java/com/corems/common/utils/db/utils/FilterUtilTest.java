package com.corems.common.utils.db.utils;

import com.corems.common.exception.ServiceException;
import com.corems.common.utils.db.spec.FilterOperation;
import com.corems.common.utils.db.spec.FilterRequest;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilterUtilTest {

    @Test
    void parse_WithNullInput_ReturnsEmptyList() {
        List<FilterRequest> result = FilterUtil.parse(null);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void parse_WithEmptyList_ReturnsEmptyList() {
        List<FilterRequest> result = FilterUtil.parse(List.of());
        
        assertTrue(result.isEmpty());
    }

    @Test
    void parse_WithBlankStrings_SkipsBlankEntries() {
        List<String> rawFilters = Arrays.asList("", "  ", null, "name:John");
        
        List<FilterRequest> result = FilterUtil.parse(rawFilters);
        
        assertEquals(1, result.size());
        assertEquals("name", result.get(0).field());
        assertEquals(FilterOperation.EQUALS, result.get(0).op());
        assertEquals("John", result.get(0).value());
    }

    @Test
    void parse_WithTwoPartFilter_UsesEqualsOperation() {
        List<String> rawFilters = List.of("name:John");
        
        List<FilterRequest> result = FilterUtil.parse(rawFilters);
        
        assertEquals(1, result.size());
        FilterRequest filter = result.get(0);
        assertEquals("name", filter.field());
        assertEquals(FilterOperation.EQUALS, filter.op());
        assertEquals("John", filter.value());
    }

    @Test
    void parse_WithThreePartFilter_ParsesOperation() {
        List<String> rawFilters = List.of("age:gt:25");
        
        List<FilterRequest> result = FilterUtil.parse(rawFilters);
        
        assertEquals(1, result.size());
        FilterRequest filter = result.get(0);
        assertEquals("age", filter.field());
        assertEquals(FilterOperation.GT, filter.op());
        assertEquals("25", filter.value());
    }

    @Test
    void parse_WithMultipleFilters_ParsesAll() {
        List<String> rawFilters = List.of(
            "name:John",
            "age:gte:18",
            "email:like:@example.com",
            "status:in:active,pending"
        );
        
        List<FilterRequest> result = FilterUtil.parse(rawFilters);
        
        assertEquals(4, result.size());
        
        assertEquals("name", result.get(0).field());
        assertEquals(FilterOperation.EQUALS, result.get(0).op());
        assertEquals("John", result.get(0).value());
        
        assertEquals("age", result.get(1).field());
        assertEquals(FilterOperation.GTE, result.get(1).op());
        assertEquals("18", result.get(1).value());
        
        assertEquals("email", result.get(2).field());
        assertEquals(FilterOperation.LIKE, result.get(2).op());
        assertEquals("@example.com", result.get(2).value());
        
        assertEquals("status", result.get(3).field());
        assertEquals(FilterOperation.IN, result.get(3).op());
        assertEquals("active,pending", result.get(3).value());
    }

    @Test
    void parse_WithAllOperations_ParsesCorrectly() {
        List<String> rawFilters = List.of(
            "field1:eq:value1",
            "field2:ne:value2",
            "field3:like:value3",
            "field4:in:value4",
            "field5:contains:value5",
            "field6:gt:value6",
            "field7:lt:value7",
            "field8:gte:value8",
            "field9:lte:value9"
        );
        
        List<FilterRequest> result = FilterUtil.parse(rawFilters);
        
        assertEquals(9, result.size());
        assertEquals(FilterOperation.EQUALS, result.get(0).op());
        assertEquals(FilterOperation.NOT_EQUALS, result.get(1).op());
        assertEquals(FilterOperation.LIKE, result.get(2).op());
        assertEquals(FilterOperation.IN, result.get(3).op());
        assertEquals(FilterOperation.CONTAINS, result.get(4).op());
        assertEquals(FilterOperation.GT, result.get(5).op());
        assertEquals(FilterOperation.LT, result.get(6).op());
        assertEquals(FilterOperation.GTE, result.get(7).op());
        assertEquals(FilterOperation.LTE, result.get(8).op());
    }

    @Test
    void parse_WithInvalidOperation_ThrowsServiceException() {
        List<String> rawFilters = List.of("field:invalid:value");
        
        assertThrows(ServiceException.class, () -> FilterUtil.parse(rawFilters));
    }

    @Test
    void parse_WithCaseInsensitiveOperation_ParsesCorrectly() {
        List<String> rawFilters = List.of("field:GT:value", "field2:Like:value2");
        
        List<FilterRequest> result = FilterUtil.parse(rawFilters);
        
        assertEquals(2, result.size());
        assertEquals(FilterOperation.GT, result.get(0).op());
        assertEquals(FilterOperation.LIKE, result.get(1).op());
    }

    @Test
    void parse_WithOnlyFieldName_SkipsEntry() {
        List<String> rawFilters = List.of("fieldonly", "name:John");
        
        List<FilterRequest> result = FilterUtil.parse(rawFilters);
        
        assertEquals(1, result.size());
        assertEquals("name", result.get(0).field());
    }

    @Test
    void parse_WithColonInValue_HandlesCorrectly() {
        List<String> rawFilters = List.of("url:eq:http://example.com:8080");
        
        List<FilterRequest> result = FilterUtil.parse(rawFilters);
        
        assertEquals(1, result.size());
        assertEquals("url", result.get(0).field());
        assertEquals(FilterOperation.EQUALS, result.get(0).op());
        assertEquals("http://example.com:8080", result.get(0).value());
    }

    @Test
    void parseAndResolve_WithNullInputs_ReturnsEmptyList() {
        List<FilterRequest> result = FilterUtil.parseAndResolve(null, null, null);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void parseAndResolve_WithValidFieldsAndNoAliases_ValidatesFields() {
        List<String> rawFilters = List.of("name:John", "age:gt:25");
        List<String> allowedFields = List.of("name", "age", "email");
        
        List<FilterRequest> result = FilterUtil.parseAndResolve(rawFilters, allowedFields, null);
        
        assertEquals(2, result.size());
        assertEquals("name", result.get(0).field());
        assertEquals("age", result.get(1).field());
    }

    @Test
    void parseAndResolve_WithInvalidField_ThrowsServiceException() {
        List<String> rawFilters = List.of("invalidField:value");
        List<String> allowedFields = List.of("name", "age");
        
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> FilterUtil.parseAndResolve(rawFilters, allowedFields, null));
        
        assertTrue(exception.getErrors().get(0).getDetails().contains("Invalid filter field"));
    }

    @Test
    void parseAndResolve_WithAliases_ResolvesFieldNames() {
        List<String> rawFilters = List.of("userName:John", "userAge:gt:25");
        List<String> allowedFields = List.of("user.name", "user.age");
        Map<String, String> aliases = Map.of(
            "userName", "user.name",
            "userAge", "user.age"
        );
        
        List<FilterRequest> result = FilterUtil.parseAndResolve(rawFilters, allowedFields, aliases);
        
        assertEquals(2, result.size());
        assertEquals("user.name", result.get(0).field());
        assertEquals("user.age", result.get(1).field());
    }

    @Test
    void parseAndResolve_WithAliasNotInAllowed_ThrowsServiceException() {
        List<String> rawFilters = List.of("userName:John");
        List<String> allowedFields = List.of("name");
        Map<String, String> aliases = Map.of("userName", "user.name");
        
        assertThrows(ServiceException.class, 
            () -> FilterUtil.parseAndResolve(rawFilters, allowedFields, aliases));
    }

    @Test
    void parseAndResolve_WithEmptyAllowedList_AllowsAllFields() {
        List<String> rawFilters = List.of("anyField:value");
        List<String> allowedFields = List.of();
        
        List<FilterRequest> result = FilterUtil.parseAndResolve(rawFilters, allowedFields, null);
        
        assertEquals(1, result.size());
        assertEquals("anyField", result.get(0).field());
    }

    @Test
    void validate_WithValidField_DoesNotThrow() {
        List<String> allowedFields = List.of("name", "age");
        
        assertDoesNotThrow(() -> FilterUtil.validate("name", "name", allowedFields));
    }

    @Test
    void validate_WithValidResolvedField_DoesNotThrow() {
        List<String> allowedFields = List.of("user.name", "user.age");
        
        assertDoesNotThrow(() -> FilterUtil.validate("userName", "user.name", allowedFields));
    }

    @Test
    void validate_WithInvalidField_ThrowsServiceException() {
        List<String> allowedFields = List.of("name", "age");
        
        ServiceException exception = assertThrows(ServiceException.class, 
            () -> FilterUtil.validate("invalidField", "invalidField", allowedFields));
        
        assertTrue(exception.getErrors().get(0).getDetails().contains("Invalid filter field: invalidField"));
    }

    @Test
    void validate_WithEmptyAllowedList_DoesNotThrow() {
        List<String> allowedFields = List.of();
        
        assertDoesNotThrow(() -> FilterUtil.validate("anyField", "anyField", allowedFields));
    }

    @Test
    void validate_WithNullAllowedList_DoesNotThrow() {
        assertDoesNotThrow(() -> FilterUtil.validate("anyField", "anyField", null));
    }
}