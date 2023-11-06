package com.lostsidewalk.buffy.app.model;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppToken {

    public final String authToken;

    public final int maxAgeInSeconds;

    public AppToken(String authToken, int maxAgeInSeconds) {
        this.authToken = authToken;
        this.maxAgeInSeconds = maxAgeInSeconds;
    }

    @Override
    public final String toString() {
        return "AppToken{" +
                "authToken='" + authToken + '\'' +
                ", maxAgeInSeconds=" + maxAgeInSeconds +
                '}';
    }
}
