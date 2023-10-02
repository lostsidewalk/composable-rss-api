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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
public class AppErrorHandler {

    @Autowired
    ErrorLogService errorLogService;

    @Autowired
    ConcurrentHashMap<String, Integer> errorStatusMap;

    private void updateErrorCount(Exception e) {
        String n = e.getClass().getSimpleName();
        if (errorStatusMap.containsKey(n)) {
            errorStatusMap.put(n, errorStatusMap.get(n) + 1);
        } else {
            errorStatusMap.put(n, 1);
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
    @ExceptionHandler(DataAccessException.class) // 404
    public ResponseEntity<ErrorDetails> handleDataAccessException(DataAccessException e, Authentication authentication) {
        errorLogService.logDataAccessException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return notFoundResponse();
    }

    @ExceptionHandler(DataUpdateException.class) // 500
    public ResponseEntity<ErrorDetails> handleDataUpdateException(DataUpdateException e, Authentication authentication) {
        errorLogService.logDataUpdateException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(DataConflictException.class) // 409
    public ResponseEntity<ErrorDetails> handleDataConflictException(DataConflictException e, Authentication authentication) {
        errorLogService.logDataConflictException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return conflictResponse();
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbortException(ClientAbortException e, Authentication authentication) {
        String username = ofNullable(authentication).map(Authentication::getName).orElse(null);
        Date timestamp = new Date();
        errorLogService.logClientAbortException(username, timestamp, e);
        updateErrorCount(e);
    }

    @ExceptionHandler(JsonParseException.class)
    public ResponseEntity<ErrorDetails> handleJsonParseException(JsonParseException e, Authentication authentication) {
//        String username = ofNullable(authentication).map(Authentication::getName).orElse(null);
//        Date timestamp = new Date();
//        errorLogService.logJsonParseException(username, timestamp, e);
        updateErrorCount(e);
        return badRequestResponse("Invalid JSON", e.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorDetails> handleIOException(IOException e, Authentication authentication) {
        String username = ofNullable(authentication).map(Authentication::getName).orElse(null);
        Date timestamp = new Date();
        errorLogService.logIOException(username, timestamp, e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleIllegalArumentException(IllegalArgumentException e, Authentication authentication) {
        errorLogService.logIllegalArgumentException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return internalServerErrorResponse();
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorDetails> hanldeMailException(MailException e, Authentication authentication) {
        errorLogService.logMailException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
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
    @ExceptionHandler(TokenValidationException.class)
    public ResponseEntity<ErrorDetails> handleTokenValidationException(TokenValidationException e, Authentication authentication) {
        errorLogService.logTokenValidationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
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
    @ExceptionHandler(AuthenticationException.class) // bad credentials exception, username not found, oauth2, etc.
    public ResponseEntity<ErrorDetails> handleAuthenticationException(AuthenticationException e, Authentication authentication) {
        errorLogService.logAuthenticationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
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
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorDetails> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, Authentication authentication) {
        errorLogService.logMethodArgumentNotValidException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        String responseMessage = e.getBindingResult().getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(","));
        return badRequestResponse("Validation Failed", responseMessage);
    }

    @ExceptionHandler(ValidationException.class) // runtime exception
    protected ResponseEntity<ErrorDetails> handleValidationException(ValidationException e, Authentication authentication) {
        errorLogService.logValidationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);

        return badRequestResponse("Validation Failed", e.getMessage());
    }

    @ExceptionHandler(AuthClaimException.class)
    public ResponseEntity<ErrorDetails> handleAuthClaimException(AuthClaimException e, Authentication authentication) {
        errorLogService.logAuthClaimException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse( "Authentication failed", EMPTY);
    }

    @ExceptionHandler(AuthProviderException.class)
    public ResponseEntity<ErrorDetails> handleUserProviderException(AuthProviderException e, Authentication authentication) {
        errorLogService.logAuthProviderException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse("Authentication failed", EMPTY);
    }

    @ExceptionHandler(RegistrationException.class)
    ResponseEntity<ErrorDetails> handleRegistrationException(RegistrationException e, Authentication authentication) {
        errorLogService.logRegistrationException(ofNullable(authentication).map(Authentication::getName).orElse(null), new Date(), e);
        updateErrorCount(e);
        return badRequestResponse("Registration failed", e.getMessage());
    }
    //
    // utility methods
    //
    private static ResponseEntity<ErrorDetails> notFoundResponse() {
        return new ResponseEntity<>(getErrorDetails( "Entity not found.", EMPTY), NOT_FOUND);
    }

    private static ResponseEntity<ErrorDetails> internalServerErrorResponse() {
        return new ResponseEntity<>(getErrorDetails( "Something horrible happened, please try again later.", EMPTY), INTERNAL_SERVER_ERROR);
    }
    private static ResponseEntity<ErrorDetails> invalidCredentialsResponse() {
        return new ResponseEntity<>(getErrorDetails("Invalid credentials", EMPTY), FORBIDDEN);
    }

    private static ResponseEntity<ErrorDetails> badRequestResponse(String message, String messageDetails) {
        return new ResponseEntity<>(getErrorDetails(message, messageDetails), BAD_REQUEST);
    }

    private static ResponseEntity<ErrorDetails> conflictResponse() {
        return new ResponseEntity<>(getErrorDetails("Conflict", EMPTY), CONFLICT);
    }

    private static ErrorDetails getErrorDetails(String message, String detailMessage) {
        return new ErrorDetails(new Date(), message, detailMessage);
    }
}
