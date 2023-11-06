package com.lostsidewalk.buffy.app.audit;

import java.io.Serial;

public class RegistrationException extends Exception {

    @Serial
    private static final long serialVersionUID = 2342342253673278623L;

    public RegistrationException(String message) {
        super(message);
    }
}
