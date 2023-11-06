package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.ApiKeyException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.audit.MailException;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.auth.ApiKey;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * This controller handles API key recovery functionality.
 */
@Slf4j
@RestController
@Validated
public class ApiKeyController {

    private static final String DEFAULT_RESPONSE_TEXT = "Ok";

    @Autowired
    AuthService authService;

    @Autowired
    MailService mailService;

    //
    // API key recovery init
    //
    @SuppressWarnings("DesignForExtension")
    @RequestMapping(value = "/send_key", method = POST)
    @Transactional
    public ResponseEntity<String> initApiKeyRecovery(Authentication authentication) throws ApiKeyException, DataAccessException, MailException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        StopWatch stopWatch = StopWatch.createStarted();
        ApiKey apiKey = authService.requireApiKey(username);
        mailService.sendApiKeyRecoveryEmail(username, apiKey);
        stopWatch.stop();
        AppLogService.logApiKeyRecoveryInit(username, stopWatch);

        return ok(DEFAULT_RESPONSE_TEXT);
    }

    @Override
    public final String toString() {
        return "ApiKeyController{" +
                "authService=" + authService +
                ", mailService=" + mailService +
                '}';
    }
}
