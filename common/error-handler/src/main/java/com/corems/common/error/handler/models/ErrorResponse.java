package com.corems.common.error.handler.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

    @JsonProperty(required = true)
    private Errors errors = new Errors();

    public static ErrorResponse of(Error error) {
        return new ErrorResponse(Errors.of(error));
    }

    public static ErrorResponse of(List<Error> errors) {
        return new ErrorResponse(Errors.of(errors));
    }
}
