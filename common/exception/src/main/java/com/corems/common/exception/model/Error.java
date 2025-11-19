package com.corems.common.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Error {

    @JsonProperty
    private String reasonCode;

    @JsonProperty
    private String description;

    @JsonProperty
    private String details;

    public static Error of(String reasonCode, String description, String details) {
        return new Error(reasonCode, description, details);
    }

    public static Error of(String reasonCode, String description) {
        return new Error(reasonCode, description, null);
    }
}
