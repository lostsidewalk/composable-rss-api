package com.lostsidewalk.buffy.app;

import com.fasterxml.jackson.core.JsonParseException;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.*;
import com.lostsidewalk.buffy.app.model.error.ErrorDetails;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
public class AppErrorHandler {

    @Autowired
    ConcurrentHashMap<String, Integer> errorStatusMap;

    private void updateErrorCount(Exception e) {
        Class<? extends Exception> aClass = e.getClass();
        String simpleName = aClass.getSimpleName();
        if (errorStatusMap.containsKey(simpleName)) {
            Integer count = errorStatusMap.get(simpleName);
            errorStatusMap.put(simpleName, count + 1);
        } else {
            errorStatusMap.put(simpleName, 1);
        }
    }

    //
    // internal server error conditions:
    //
    // database access exception
    // data not found exception
    // OPML (export) exception
    // IO exception/client abort exception/JSON parse exception
    // illegal argument exception (runtime)
    // stripe exception
    // mail exception
    //
    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(DataAccessException.class) // 404
    public final ResponseEntity<ErrorDetails> handleDataAccessException(DataAccessException e, Authentication authentication) {
        ErrorLogService.logDataAccessException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return notFoundResponse();
    }

    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(DataUpdateException.class) // 500
    public final ResponseEntity<ErrorDetails> handleDataUpdateException(DataUpdateException e, Authentication authentication) {
        ErrorLogService.logDataUpdateException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(DataConflictException.class) // 409
    public final ResponseEntity<ErrorDetails> handleDataConflictException(DataConflictException e, Authentication authentication) {
        ErrorLogService.logDataConflictException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return conflictResponse();
    }

    @ExceptionHandler(ClientAbortException.class)
    public final void handleClientAbortException(ClientAbortException e, Authentication authentication) {
        String username = ofNullable(authentication).map(Authentication::getName).orElse(null);
        Date timestamp = new Date();
        ErrorLogService.logClientAbortException(username, timestamp, e);
        updateErrorCount(e);
    }

    @ExceptionHandler(JsonParseException.class)
    public final ResponseEntity<ErrorDetails> handleJsonParseException(JsonParseException e, Authentication authentication) {
//        String username = ofNullable(authentication).map(Authentication::getName).orElse(null);
//        Date timestamp = new Date();
//        ErrorLogService.logJsonParseException(username, timestamp, e);
        updateErrorCount(e);
        String message = e.getMessage();
        return badRequestResponse("Invalid JSON", message);
    }

    @ExceptionHandler(IOException.class)
    public final ResponseEntity<ErrorDetails> handleIOException(IOException e, Authentication authentication) {
        String username = ofNullable(authentication).map(Authentication::getName).orElse(null);
        Date timestamp = new Date();
        ErrorLogService.logIOException(username, timestamp, e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(IllegalArgumentException.class)
    public final ResponseEntity<ErrorDetails> handleIllegalArumentException(IllegalArgumentException e, Authentication authentication) {
        ErrorLogService.logIllegalArgumentException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(MailException.class)
    public final ResponseEntity<ErrorDetails> hanldeMailException(MailException e, Authentication authentication) {
        ErrorLogService.logMailException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    //
    // invalid credentials conditions (token-related):
    //
    // token is expired
    // username is missing from token
    // unable to locate authentication token (in request header or cookie)
    // token validation claim is outdated
    // token validation claim is missing
    // unable to parse token
    // not a valid token (claims are missing)
    //
    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(TokenValidationException.class)
    public final ResponseEntity<ErrorDetails> handleTokenValidationException(TokenValidationException e, Authentication authentication) {
        ErrorLogService.logTokenValidationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return invalidCredentialsResponse();
    }

    //
    // invalid credentials conditions (other):
    //
    // supplied credentials are invalid
    // userDao cannot locate user by name
    // userDao cannot locate user by email address
    // user by name NEQ user by email address
    //
    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(AuthenticationException.class) // bad credentials exception, username not found, oauth2, etc.
    public final ResponseEntity<ErrorDetails> handleAuthenticationException(AuthenticationException e, Authentication authentication) {
        ErrorLogService.logAuthenticationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return invalidCredentialsResponse();
    }

    //
    // bad request conditions:
    //
    // invalid method arguments
    // invalid callback from Stripe (signature verification failed)
    // missing/empty authentication claim during login/pw reset/verification
    // incorrect auth provider for user
    // invalid registration request
    // proxy URL hash validation failure
    //
    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected final ResponseEntity<ErrorDetails> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, Authentication authentication) {
        ErrorLogService.logMethodArgumentNotValidException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        String responseMessage = fieldErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(","));
        return badRequestResponse("Validation Failed", responseMessage);
    }

    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(ValidationException.class) // runtime exception
    protected final ResponseEntity<ErrorDetails> handleValidationException(ValidationException e, Authentication authentication) {
        ErrorLogService.logValidationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);

        return badRequestResponse("Validation Failed", e.getMessage());
    }

    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(AuthClaimException.class)
    public final ResponseEntity<ErrorDetails> handleAuthClaimException(AuthClaimException e, Authentication authentication) {
        ErrorLogService.logAuthClaimException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse("Authentication failed", EMPTY);
    }

    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(AuthProviderException.class)
    public final ResponseEntity<ErrorDetails> handleUserProviderException(AuthProviderException e, Authentication authentication) {
        ErrorLogService.logAuthProviderException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse("Authentication failed", EMPTY);
    }

    @SuppressWarnings("NestedMethodCall")
    @ExceptionHandler(RegistrationException.class)
    final ResponseEntity<ErrorDetails> handleRegistrationException(RegistrationException e, Authentication authentication) {
        ErrorLogService.logRegistrationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse("Registration failed", e.getMessage());
    }

    //
    // utility methods
    //
    private static ResponseEntity<ErrorDetails> notFoundResponse() {
        ErrorDetails errorDetails = getErrorDetails("Entity not found.", EMPTY);
        return new ResponseEntity<>(errorDetails, NOT_FOUND);
    }

    private static ResponseEntity<ErrorDetails> internalServerErrorResponse() {
        ErrorDetails errorDetails = getErrorDetails("Something horrible happened, please try again later.", EMPTY);
        return new ResponseEntity<>(errorDetails, INTERNAL_SERVER_ERROR);
    }

    private static ResponseEntity<ErrorDetails> invalidCredentialsResponse() {
        ErrorDetails errorDetails = getErrorDetails("Invalid credentials", EMPTY);
        return new ResponseEntity<>(errorDetails, FORBIDDEN);
    }

    private static ResponseEntity<ErrorDetails> badRequestResponse(String message, String messageDetails) {
        ErrorDetails errorDetails = getErrorDetails(message, messageDetails);
        return new ResponseEntity<>(errorDetails, BAD_REQUEST);
    }

    private static ResponseEntity<ErrorDetails> conflictResponse() {
        ErrorDetails errorDetails = getErrorDetails("Conflict", EMPTY);
        return new ResponseEntity<>(errorDetails, CONFLICT);
    }

    private static ErrorDetails getErrorDetails(String message, String detailMessage) {
        return new ErrorDetails(new Date(), message, detailMessage);
    }

    @Override
    public final String toString() {
        return "AppErrorHandler{" +
                "errorStatusMap=" + errorStatusMap +
                '}';
    }
}
