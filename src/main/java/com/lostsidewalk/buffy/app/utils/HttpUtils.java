package com.lostsidewalk.buffy.app.utils;

import org.springframework.http.HttpMethod;

public class HttpUtils {

    public static boolean isPatch(HttpMethod httpMethod) {
        return httpMethod != null && httpMethod.matches("PATCH");
    }
}
