package com.lostsidewalk.buffy.app.user;

import org.springframework.security.core.AuthenticationException;

import java.io.Serial;

class OAuth2AuthenticationProcessingException extends AuthenticationException {

    @Serial
    private static final long serialVersionUID = 23423426747523L;

    OAuth2AuthenticationProcessingException(String msg) {
        super(msg);
    }
}
