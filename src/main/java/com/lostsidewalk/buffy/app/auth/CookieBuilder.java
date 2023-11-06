package com.lostsidewalk.buffy.app.auth;

import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;

import static java.lang.Integer.MAX_VALUE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@SuppressWarnings({"ReturnOfThis", "unused"})
@Slf4j
public class CookieBuilder {

    private final String name;
    private final String value;

    public CookieBuilder(String name, String value) {
        this.name = name;
        this.value = value;
    }

    private boolean isHttpOnly;
    private boolean isSecure;
    private int maxAge = MAX_VALUE;
    private String path = "/";
    private String domain = EMPTY;

    final CookieBuilder setHttpOnly(@SuppressWarnings("SameParameterValue") boolean httpOnly) {
        isHttpOnly = httpOnly;
        return this;
    }

    final CookieBuilder setSecure(boolean secure) {
        isSecure = secure;
        return this;
    }

    final CookieBuilder setMaxAge(int maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    @SuppressWarnings("SameParameterValue")
    final CookieBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    final CookieBuilder setDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public final Cookie build() {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(isHttpOnly);
        cookie.setSecure(isSecure); // true for prod/https
        cookie.setMaxAge(maxAge);
        cookie.setPath(path);
        cookie.setDomain(domain);

        return cookie;
    }

    @Override
    public final String toString() {
        return "CookieBuilder{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", isHttpOnly=" + isHttpOnly +
                ", isSecure=" + isSecure +
                ", maxAge=" + maxAge +
                ", path='" + path + '\'' +
                ", domain='" + domain + '\'' +
                '}';
    }
}
