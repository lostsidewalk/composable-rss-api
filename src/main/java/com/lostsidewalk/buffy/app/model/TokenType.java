package com.lostsidewalk.buffy.app.model;

import java.util.Date;
import java.util.function.LongFunction;

import static java.lang.String.format;
import static java.util.Locale.ROOT;

public enum TokenType {
    APP_AUTH(5 * 60, "ComposableRSS Auth Token"), // 5 min max
    APP_AUTH_REFRESH(24 * 60 * 60, "ComposableRSS Auth Refresh Token"), // 1 day max
    PW_RESET(24 * 60 * 60, "ComposableRSS Password Reset Token"), // 1 day max
    PW_AUTH(5 * 60, "ComposableRSS Password Reset Auth Token"), // 5 min max
    VERIFICATION(60 * 60 * 24 * 365, "ComposableRSS Email Verification Token"); // 1 year max

    public final LongFunction<Date> expirationBuilder;

    public final int maxAgeInSeconds;

    @SuppressWarnings("WeakerAccess")
    public final String description;

    public final String tokenName;

    TokenType(int maxAgeInSeconds, String description) {
        this.maxAgeInSeconds = maxAgeInSeconds;
        expirationBuilder = defaultExpirationBuilder(maxAgeInSeconds);
        this.description = description;
        String name = name();
        tokenName = defaultName(name);
    }

    private static LongFunction<Date> defaultExpirationBuilder(int maxAgeInSeconds) {
        return l -> new Date(l + (1000L * maxAgeInSeconds));
    }

    private static String defaultName(String name) {
        String s = name.toLowerCase(ROOT);
        return format("newsgears-%s-token", s);
    }

    @Override
    public String toString() {
        return "TokenType{" +
                "expirationBuilder=" + expirationBuilder +
                ", maxAgeInSeconds=" + maxAgeInSeconds +
                ", description='" + description + '\'' +
                ", tokenName='" + tokenName + '\'' +
                '}';
    }
}
