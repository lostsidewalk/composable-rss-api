package com.lostsidewalk.buffy.app.audit;


import java.io.Serial;

public class ApiKeyException extends Exception {

    @Serial
    private static final long serialVersionUID = 3413875112341242298L;

    public ApiKeyException(String message) {
        super(message);
    }
}
