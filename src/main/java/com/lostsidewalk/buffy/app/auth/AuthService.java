package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.ApiKeyException;
import com.lostsidewalk.buffy.app.audit.AuthClaimException;
import com.lostsidewalk.buffy.app.audit.AuthProviderException;
import com.lostsidewalk.buffy.app.model.AppToken;
import com.lostsidewalk.buffy.app.model.TokenType;
import com.lostsidewalk.buffy.app.model.request.PasswordResetRequest;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.auth.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static com.lostsidewalk.buffy.app.model.TokenType.*;
import static com.lostsidewalk.buffy.app.utils.RandomUtils.generateRandomString;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Optional.of;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
public class AuthService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserDao userDao;

    @Autowired
    ApiKeyDao apiKeyDao;

    @Autowired
    TokenService tokenService;

    @Value("${comprss.development:false}")
    boolean isDevelopment;

    public final AppToken initPasswordReset(PasswordResetRequest passwordResetRequest) throws AuthClaimException, DataAccessException, DataUpdateException {
        // (1) locate the user by name or email
        String username = passwordResetRequest.getUsername();
        User userByName = userDao.findByName(username);
        if (userByName == null) {
            // invalid request (username supplied by user cannot be found)
            throw new UsernameNotFoundException(username);
        }
        User userByEmail = null;
        String email = passwordResetRequest.getEmail();
        if (isNotBlank(email)) {
            userByEmail = userDao.findByEmailAddress(email);
            if (userByEmail == null) {
                // invalid request (email supplied by user cannot be found)
                throw new UsernameNotFoundException(username);
            }
        }
        if (userByEmail != null && !userByName.equals(userByEmail)) {
            // invalid request (user and email supplied but corresponding users don't match)
            throw new UsernameNotFoundException(username);
        }
        // (2) finalize the current pw reset claim (invalidates all outstanding reset tokens)
        log.info("Finalizing current PW reset claim for username={}, email={}", username, email);
        finalizePwResetClaim(username);
        // (3) generate and email a new pw reset token
        return generatePasswordResetToken(username);
    }

    public final void continuePasswordReset(String username, HttpServletResponse response) throws DataAccessException, DataUpdateException {
        // (5) finalize the current pw reset claim (it's being used right now)
        finalizePwResetClaim(username);
        // (6) finalize/regenerate the pw reset auth claim
        finalizePwResetAuthClaim(username);
        // (7) setup a short-lived logged-in session (add the pw_auth claim cookie to the response)
        User user = userDao.findByName(username);
        if (user != null) {
            String pwResetAuthClaim = user.getPwResetAuthClaim();
            addTokenCookieToResponse(PW_AUTH, username, pwResetAuthClaim, response);
        } else {
            throw new UsernameNotFoundException(username);
        }
    }

    public final void requireAuthProvider(String username, AuthProvider expected) throws AuthProviderException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        Stream<AuthProvider> stream = of(user).map(User::getAuthProvider)
                .filter(a -> a == expected)
                .stream();
        Optional<AuthProvider> any = stream.findAny();
        AuthProvider actual = user.getAuthProvider();
        any.orElseThrow(() -> new AuthProviderException(username, expected, actual));
    }

    public final String requireAuthClaim(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return of(user).map(User::getAuthClaim)
                .orElseThrow(() -> new AuthClaimException("User has no auth claim"));
    }

    public final ApiKey requireApiKey(String username) throws ApiKeyException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return of(user).map(User::getId).map(apiKeyDao::findByUserId)
                .orElseThrow(() -> new ApiKeyException("User has no API key"));
    }

    public final void finalizeAuthClaim(String username) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.info("Finalizing auth claim for username={}", username);
        String newAuthClaim = randomClaimValue();
        user.setAuthClaim(newAuthClaim);
        userDao.updateAuthClaim(user);
    }

    public final void finalizePwResetClaim(String username) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.info("Finalizing PW reset claim for username={}", username);
        String newPwResetClaim = randomClaimValue();
        user.setPwResetClaim(newPwResetClaim);
        userDao.updatePwResetClaim(user);
    }

    public final void finalizeVerificationClaim(String username) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.info("Finalizing verification claim for username={}", username);
        String newVerificationClaim = randomClaimValue();
        user.setVerificationClaim(newVerificationClaim);
        userDao.updateVerificationClaim(user);
    }

    public final String requirePwResetAuthClaim(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return of(user)
                .map(User::getPwResetAuthClaim)
                .orElseThrow(() -> new AuthClaimException("User has no PW reset auth claim"));
    }

    public final void finalizePwResetAuthClaim(String username) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        log.info("Finalizing PW reset auth claim for username={}", username);
        String newPwResetAuthClaim = randomClaimValue();
        user.setPwResetAuthClaim(newPwResetAuthClaim);
        userDao.updatePwResetAuthClaim(user);
    }

    public final void addTokenCookieToResponse(TokenType tokenType, String username, String validationClaim, HttpServletResponse response) {
        AppToken appToken = generateAppToken(tokenType, username, validationClaim);
        Cookie tokenCookie = new CookieBuilder(tokenType.tokenName, appToken.authToken)
                .setPath("/")
                .setHttpOnly(true)
                .setMaxAge(appToken.maxAgeInSeconds)
                .setSecure(!isDevelopment)
                .build();
        // add app token cookie to response
        response.addCookie(tokenCookie);
    }

    @SuppressWarnings("MethodMayBeStatic")
    public final String getTokenCookieFromRequest(TokenType tokenType, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (isNotEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if (StringUtils.equals(name, tokenType.tokenName)) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    public final AppToken generateAuthToken(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String u = user.getUsername();
        String authClaimSecret = user.getAuthClaim();
        if (isBlank(authClaimSecret)) {
            throw new AuthClaimException("User has no auth claim");
        }

        return generateAppToken(APP_AUTH, u, authClaimSecret);
    }

    @SuppressWarnings("WeakerAccess")
    public final AppToken generatePasswordResetToken(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String u = user.getUsername();
        String pwResetClaimSecret = user.getPwResetClaim();
        if (isBlank(pwResetClaimSecret)) {
            throw new AuthClaimException("User has no PW reset claim");
        }

        return generateAppToken(PW_RESET, u, pwResetClaimSecret);
    }

    public final AppToken generateVerificationToken(String username) throws AuthClaimException, DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String u = user.getUsername();
        String vfnClaim = user.getVerificationClaim();
        if (isBlank(vfnClaim)) {
            throw new AuthClaimException("User has no verification claim");
        }

        return generateAppToken(VERIFICATION, u, vfnClaim);
    }

    public final ApiKey generateApiKey(String username) throws DataAccessException, DataUpdateException, DataConflictException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String uuid = UUID.randomUUID().toString();
        String rawApiSecret = generateRandomString(32);
        String secret = passwordEncoder.encode(rawApiSecret);
        Long id = user.getId();
        ApiKey apiKey = ApiKey.from(id, uuid, secret);
        return apiKeyDao.add(apiKey);
    }

    public final void updatePassword(String username, String newPassword) throws DataAccessException, DataUpdateException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        user.setPassword(newPassword);
        userDao.updatePassword(user);
    }

    public final User findUserByApiKey(String apiKey) throws ApiKeyException, DataAccessException {
        User user = userDao.findByApiKey(apiKey);
        if (user == null) {
            throw new ApiKeyException("Unable to locate user by API Key");
        }

        return user;
    }

    //
    //
    //
    final AppToken generateAppToken(TokenType tokenType, String username, String validationClaim) {
        Map<String, Object> claimsMap = new HashMap<>(1);
        Charset charset = defaultCharset();
        String validationClaimHash = sha256(validationClaim, charset);
        claimsMap.put(tokenType.tokenName, validationClaimHash);
        String authToken = tokenService.generateToken(claimsMap, username, tokenType);
        int maxAgeInSeconds = tokenType.maxAgeInSeconds;

        return new AppToken(authToken, maxAgeInSeconds);
    }

    private static String randomClaimValue() {
        return randomAlphanumeric(16);
    }

    @Override
    public final String toString() {
        return "AuthService{" +
                "passwordEncoder=" + passwordEncoder +
                ", userDao=" + userDao +
                ", apiKeyDao=" + apiKeyDao +
                ", tokenService=" + tokenService +
                ", isDevelopment=" + isDevelopment +
                '}';
    }
}
