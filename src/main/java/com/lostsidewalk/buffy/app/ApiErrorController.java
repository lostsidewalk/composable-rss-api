package com.lostsidewalk.buffy.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;


@Slf4j
@RestController
public class ApiErrorController implements ErrorController {

    private static final String STATUS_CODE_ATTR_NAME = "jakarta.servlet.error.status_code";

    private static final String REQUEST_URI_ATTR_NAME = "jakarta.servlet.error.request_uri";

    private static final String ERROR_MESSAGE_ATTR_NAME = "jakarta.servlet.error.message"; // may be blank

    private static final String ERROR_PATH = "/error";

    @SuppressWarnings("MethodMayBeStatic")
    @RequestMapping(ERROR_PATH)
    final ResponseEntity<Object> handleErrors(WebRequest request) {
        Integer statusCode = (Integer) request.getAttribute(STATUS_CODE_ATTR_NAME, 0);
        String requestUri = (String) request.getAttribute(REQUEST_URI_ATTR_NAME, 0);
        String errorMessage = (String) request.getAttribute(ERROR_MESSAGE_ATTR_NAME, 0);
        FeedErrorResponse errorResponse = new FeedErrorResponse(statusCode, requestUri, errorMessage);
        HttpStatus httpStatus = (statusCode != null ? HttpStatus.valueOf(statusCode) : INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(errorResponse, httpStatus);
    }

    @Data
    @AllArgsConstructor
    @JsonInclude(NON_EMPTY)
    static class FeedErrorResponse {
        Integer statusCode;
        String requestUri;
        String errorMessage;
    }
}
