package com.lostsidewalk.buffy.app.audit;

import java.io.Serial;

public class MailException extends Exception {

    @Serial
    private static final long serialVersionUID = 2346242323462357523L;

    public MailException(String message) {
        super(message);
    }
}
