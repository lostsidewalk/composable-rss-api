package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.*;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.app.model.AppToken;
import com.lostsidewalk.buffy.app.model.request.RegistrationRequest;
import com.lostsidewalk.buffy.app.model.response.RegistrationResponse;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import com.lostsidewalk.buffy.app.auth.LocalUserService;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import com.lostsidewalk.buffy.auth.ApiKey;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static com.lostsidewalk.buffy.app.model.TokenType.VERIFICATION;
import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
@RestController
@Validated
public class RegistrationController {

    @Autowired
    LocalUserService userService;

    @Autowired
    AuthService authService;

    @Autowired
    MailService mailService;

    @Autowired
    TokenService tokenService;

    @Autowired
    Validator validator;

    @Value("${verification.error.redirect.url}")
    String verificationErrorRedirectUrl;

    @Value("${verification.continue.redirect.url}")
    String verificationContinueRedirectUrl;

    //
    // registration and verification span the following two calls:
    //
    // the initial registration process starts here
    //
    @SuppressWarnings("DesignForExtension")
    @RequestMapping(path = "/register", method = POST, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest registrationRequest) throws RegistrationException, AuthClaimException, DataAccessException, DataUpdateException, DataConflictException {
        //
        // (1) validate the incoming params and create the new user entity
        //
        String username = registrationRequest.getUsername();
        String email = registrationRequest.getEmail();
        String password = registrationRequest.getPassword();
        log.info("Registering username={}, email={}", username, email);
        StopWatch stopWatch = StopWatch.createStarted();
        userService.registerUser(username, email, password);
        //
        // (2) generate claims for the user
        //
        authService.finalizeVerificationClaim(username);
        authService.finalizeAuthClaim(username);
        authService.finalizePwResetClaim(username);
        //
        // (3) generate API key
        //
        ApiKey apiKey = authService.generateApiKey(username);
        //
        // (4) generate and send the verification token
        //
        AppToken verificationToken = authService.generateVerificationToken(username);
        log.info("Sending verification email to username={}", username);
        try {
            mailService.sendVerificationEmail(username, verificationToken, apiKey);
        } catch (UsernameNotFoundException | MailException ignored) {
            // ignored
        }
        //
        // (5) user registration is complete, respond w/username and password, and http status 200 to trigger authentication
        //
        RegistrationResponse registrationResponse = new RegistrationResponse(username, password);
        validator.validate(registrationResponse);
        stopWatch.stop();
        AppLogService.logUserRegistration(username, stopWatch);
        return ok(registrationResponse);
    }

    //
    // the verification step is completed when the user clicks the get-back link from their email
    //
    @SuppressWarnings("DesignForExtension")
    @RequestMapping(path = "/verify/{token}", method = GET)
    @Transactional
    public void verify(@PathVariable String token, HttpServletResponse response) throws TokenValidationException, IOException, DataAccessException, DataUpdateException {
        //
        // (5) validate the supplied token
        //
        JwtUtil jwtUtil = tokenService.instanceFor(VERIFICATION, token); // token w/claims
        if (jwtUtil.isTokenExpired()) {
            response.sendRedirect(verificationErrorRedirectUrl);
        }
        //
        // (6) extract username from token and mark user as verified
        //
        String username = jwtUtil.extractUsername();
        log.info("Verification continuation received for username={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        userService.markAsVerified(username);
        //
        // (7) finalize verification claim
        //
        authService.finalizeVerificationClaim(username);
        //
        //
        //
        stopWatch.stop();
        AppLogService.logUserVerification(username, stopWatch);
        //
        // (7) user verification is complete, hand-off to front-end
        //
        response.sendRedirect(verificationContinueRedirectUrl);
    }

    @SuppressWarnings("DesignForExtension")
    @RequestMapping(path = "/deregister", method = DELETE, produces = APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<ResponseMessage> deregister(Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.info("De-registering username={}", username);
        //
        StopWatch stopWatch = StopWatch.createStarted();
        // delete user account
        // * the delete operation should cascade to all subsidiary entities, i.e., API keys
        userService.deregisterUser(username);
        //
        stopWatch.stop();
        AppLogService.logUserDeregistration(username, stopWatch);

        ResponseEntity.BodyBuilder responseBuilder = ok();
        ResponseMessage body = buildResponseMessage(EMPTY);
        return responseBuilder.body(body);
    }

    @Override
    public final String toString() {
        return "RegistrationController{" +
                ", userService=" + userService +
                ", authService=" + authService +
                ", mailService=" + mailService +
                ", tokenService=" + tokenService +
                ", validator=" + validator +
                ", verificationErrorRedirectUrl='" + verificationErrorRedirectUrl + '\'' +
                ", verificationContinueRedirectUrl='" + verificationContinueRedirectUrl + '\'' +
                '}';
    }
}
