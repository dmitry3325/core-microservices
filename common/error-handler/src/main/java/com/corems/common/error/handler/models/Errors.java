package com.corems.common.error.handler.models;

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
public class Errors {

    @JsonProperty(required = true)
    private List<Error> error = new ArrayList<>();

    public static Errors of(Error error) {
        List<Error> errors = new ArrayList<>();
        errors.add(error);
        return new Errors(errors);
    }

    public static Errors of(List<Error> errors) {
        return new Errors(errors);
    }
}
