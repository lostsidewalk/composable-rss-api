package com.lostsidewalk.buffy.app.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.util.Enumeration;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class OptionsAuthHandler {

    static class MissingOptionsHeaderException extends Exception {

        @Serial
        private static final long serialVersionUID = 2342342672342356623L;

        final Enumeration<String> headerNames;

        MissingOptionsHeaderException(Enumeration<String> headerNames) {
            this.headerNames = headerNames;
        }
    }

    static void processRequest(HttpServletRequest request) throws MissingOptionsHeaderException {
        String accessControlRequestMethod = request.getHeader("Access-Control-Request-Method");
        String accessControlRequestHeaders = request.getHeader("Access-Control-Request-Headers");

        if (isBlank(accessControlRequestMethod) || isBlank(accessControlRequestHeaders)) {
            Enumeration<String> headerNames = request.getHeaderNames();
            throw new MissingOptionsHeaderException(headerNames);
        }
    }
}
