package com.lostsidewalk.buffy.app.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;


@Slf4j
public class HttpUtils {

    public static boolean isPatch(HttpMethod httpMethod) {
        return httpMethod != null && httpMethod.matches("PATCH");
    }
}
