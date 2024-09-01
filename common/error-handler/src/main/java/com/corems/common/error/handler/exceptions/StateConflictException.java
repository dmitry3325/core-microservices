package com.corems.common.error.handler.exceptions;

import com.corems.common.error.handler.models.Error;
import org.springframework.http.HttpStatus;

import java.util.List;

public class StateConflictException extends ServiceException {

    public StateConflictException(List<Error> errors) {
        super(errors, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
