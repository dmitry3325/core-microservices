package com.corems.userms.app.controller;

import com.corems.userms.app.model.exception.AuthExceptionReasonCodes;
import com.corems.userms.app.model.exception.AuthServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ErrorController {

    @GetMapping("/unauthorized")
    public ResponseEntity<?> unauthorized() {
        throw new AuthServiceException(AuthExceptionReasonCodes.UNAUTHORIZED);
    }
}
