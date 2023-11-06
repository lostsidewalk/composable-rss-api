package com.lostsidewalk.buffy.app.audit;

import com.lostsidewalk.buffy.auth.AuthProvider;

import java.io.Serial;

public class AuthProviderException extends Exception {

    @Serial
    private static final long serialVersionUID = 3434234234872422298L;

    public AuthProviderException(String username, AuthProvider expected, AuthProvider actual) {
        super("User has incorrect auth provider, username=" + username + ", expected=" + expected + ", actual=" + actual);
    }
}
