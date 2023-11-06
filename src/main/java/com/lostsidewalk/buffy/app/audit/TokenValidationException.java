package com.lostsidewalk.buffy.app.audit;

import java.io.Serial;

public class TokenValidationException extends Exception {

    @Serial
    private static final long serialVersionUID = 114564123342234623L;

    public TokenValidationException(String message) {
        super(message);
    }
}
