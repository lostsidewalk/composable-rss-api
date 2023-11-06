package com.lostsidewalk.buffy.app.audit;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.util.Date;
import java.util.StringJoiner;

import static java.lang.System.arraycopy;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@SuppressWarnings("UseOfObsoleteDateTimeApi")
@Slf4j(topic = "appErrorLog")
@Service
public class ErrorLogService {

    public static void logDataAccessException(String username, Date timestamp, DataAccessException e) {
        String message = e.getMessage();
        auditError("data-access-exception", username, timestamp, message);
    }

    public static void logDataUpdateException(String username, Date timestamp, DataUpdateException e) {
        String message = e.getMessage();
        auditError("data-not-found-exception", username, timestamp, message);
    }

    public static void logDataConflictException(String username, Date timestamp, DataConflictException e) {
        String message = e.getMessage();
        auditError("data-conflict-exception", username, timestamp, message);
    }

    public static void logIOException(String username, Date timestamp, IOException e) {
        String message = e.getMessage();
        auditError("io-exception", username, timestamp, message);
    }

    public static void logIllegalArgumentException(String username, Date timestamp, IllegalArgumentException e) {
        String message = e.getMessage();
        auditError("illegal-argument-exception", username, timestamp, message);
    }

    public static void logTokenValidationException(String username, Date timestamp, TokenValidationException e) {
        String message = e.getMessage();
        auditError("token-validation-exception", username, timestamp, message);
    }

    public static void logAuthenticationException(String username, Date timestamp, AuthenticationException e) {
        String message = e.getMessage();
        auditError("authentication-exception", username, timestamp, message);
    }

    private static final String FIELD_ERROR_TEMPLATE = "Field: %s, rejected value: %s, due to: %s";

    public static void logMethodArgumentNotValidException(String username, Date timestamp, MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringJoiner joiner = new StringJoiner(",");
        for (FieldError fe : bindingResult.getFieldErrors()) {
            String field = fe.getField();
            Object rejectedValue = fe.getRejectedValue();
            String defaultMessage = fe.getDefaultMessage();
            String format = String.format(FIELD_ERROR_TEMPLATE, field, rejectedValue, defaultMessage);
            joiner.add(format);
        }
        String fieldErrors = joiner.toString();
        auditError("method-argument-not-valid-exception", username, timestamp, fieldErrors);
    }

    public static void logValidationException(String username, Date timestamp, ValidationException e) {
        String message = e.getMessage();
        auditError("validation-exception", username, timestamp, message);
    }

    public static void logAuthClaimException(String username, Date timestamp, AuthClaimException e) {
        String message = e.getMessage();
        auditError("auth-claim-exception", username, timestamp, message);
    }

    public static void logAuthProviderException(String username, Date timestamp, AuthProviderException e) {
        String message = e.getMessage();
        auditError("auth-provider-exception", username, timestamp, message);
    }

    public static void logRegistrationException(String username, Date timestamp, RegistrationException e) {
        String message = e.getMessage();
        auditError("registration-exception", username, timestamp, message);
    }

    public static void logMailException(String username, Date timestamp, MailException e) {
        String message = e.getMessage();
        auditError("mail-exception", username, timestamp, message);
    }

    public static void logClientAbortException(String username, Date timestamp, ClientAbortException e) {
        String message = e.getMessage();
        auditError("client-abort-exception", username, timestamp, message);
    }

    //
    private static void auditError(String logTag, String username, Date timestamp, Object... args) {
        String fullFormatStr = "eventType={}, username={}, timestamp={}";
        if (isNotEmpty("message={}")) {
            fullFormatStr += (", " + "message={}");
        }
        Object[] allArgs = new Object[args.length + 5];
        allArgs[0] = logTag;
        allArgs[1] = username;
        allArgs[2] = timestamp;
        arraycopy(args, 0, allArgs, 3, args.length);
        log.error(fullFormatStr, allArgs);
    }
}
