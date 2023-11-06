package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AuthClaimException;
import com.lostsidewalk.buffy.app.audit.AuthProviderException;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.model.request.LoginRequest;
import com.lostsidewalk.buffy.app.model.response.LoginResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH_REFRESH;
import static com.lostsidewalk.buffy.auth.AuthProvider.LOCAL;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * This controller handles authentication functionality (i.e., login, logout).
 * <p>
 * Password reset, user registration, and email validation are handled elsewhere.
 * <p>
 * The 'currentUser' call is used by the front-end to determine if the user
 * has a valid refresh token.
 * <p>
 * The 'authenticate' call is used to setup a logged-in session.  This call also adds a
 * refresh token cookie to the response, so that the user remains 'logged in' as long as
 * the cookie persists.
 * <p>
 * The 'deauthenticate' call is used to log out.  Calling this method finalized the auth claim
 * on the user object, so that the further attempts to validate already-generated tokens will fail.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
class AuthenticationController {

    @Autowired
    AuthService authService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    Validator validator;

    //
    // auth check
    //
    @PreAuthorize("hasAuthority('ROLE_UNVERIFIED')")
    @RequestMapping(value = "/currentuser", method = GET)
    public ResponseEntity<LoginResponse> getCurrentUser(Authentication authentication) throws AuthClaimException, DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        LoginResponse authenticationResponse = buildAuthenticationResponse(
                authService.generateAuthToken(username).authToken,
                username
        );
        validator.validate(authenticationResponse);
        return ok(authenticationResponse);
    }

    //
    // login (open access)
    //
    @RequestMapping(value = "/authenticate", method = POST)
    @Transactional
    public ResponseEntity<LoginResponse> createAuthenticationToken(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response)
            throws AuthProviderException, AuthClaimException, DataAccessException {
        // extract username
        String username = loginRequest.getUsername();
        // validate the auth provider
        authService.requireAuthProvider(username, LOCAL);
        // add refresh token cookie to response
        String authClaim = authService.requireAuthClaim(username);
        // authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
        // generate the auth token/build auth response
        authService.addTokenCookieToResponse(APP_AUTH_REFRESH, username, authClaim, response);
        LoginResponse authenticationResponse = buildAuthenticationResponse(
                authService.generateAuthToken(username).authToken,
                username
        );
        validator.validate(authenticationResponse);
        log.info("Login succeeded for username={}", username);
        return ok(authenticationResponse);
    }

    private static LoginResponse buildAuthenticationResponse(String authToken, String username) {
        LoginResponse loginResponse;
        loginResponse = LoginResponse.from(authToken, username);

        return loginResponse;
    }

    //
    // logout (open access)
    //
    @RequestMapping(value = "/deauthenticate", method = GET)
    @Transactional
    public ResponseEntity<String> deauthenticate(Authentication authentication) throws DataAccessException, DataUpdateException {
        if (authentication != null) {
            UserDetails userDetails = (UserDetails) authentication.getDetails();
            String username = userDetails.getUsername();
            authService.finalizeAuthClaim(username);
            log.info("Finalized auth claim for username={}", username);
        }
        return ok().build();
    }

    @Override
    public String toString() {
        return "AuthenticationController{" +
                "authService=" + authService +
                ", authenticationManager=" + authenticationManager +
                ", validator=" + validator +
                '}';
    }
}
