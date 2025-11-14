package com.corems.common.exception.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

    @JsonProperty(required = true)
    private List<Error> errors = new ArrayList<>();

    public static ErrorResponse of(Error error) {
        List<Error> errors = new ArrayList<>();
        errors.add(error);
        return new ErrorResponse(errors);
    }

    public static ErrorResponse of(List<Error> errors) {
        return new ErrorResponse(errors);
    }
}
