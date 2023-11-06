package com.lostsidewalk.buffy.app.audit;

import java.io.Serial;

public class AuthClaimException extends Exception {

    @Serial
    private static final long serialVersionUID = 3412423563872422298L;

    public AuthClaimException(String message) {
        super(message);
    }
}
