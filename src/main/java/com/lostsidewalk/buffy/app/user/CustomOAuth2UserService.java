package com.lostsidewalk.buffy.app.user;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.audit.ErrorLogService;
import com.lostsidewalk.buffy.app.audit.MailException;
import com.lostsidewalk.buffy.app.audit.RegistrationException;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.auth.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.lostsidewalk.buffy.app.user.CustomOAuth2UserService.CustomOAuth2ErrorCodes.*;
import static com.lostsidewalk.buffy.app.user.OAuth2UserInfoFactory.getOAuth2UserInfo;
import static com.lostsidewalk.buffy.app.user.UserPrincipal.create;
import static com.lostsidewalk.buffy.app.utils.RandomUtils.generateRandomString;
import static com.lostsidewalk.buffy.auth.AuthProvider.byRegistrationId;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AppLogService appLogService;

    @Autowired
    ErrorLogService errorLogService;

    @Autowired
    MailService mailService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ApiKeyDao apiKeyDao;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error("AUTH", ex.getMessage(), null));
        } catch (RegistrationException ex) {
            errorLogService.logRegistrationException(oAuth2User.getName(), new Date(), ex);
            throw new OAuth2AuthenticationException(new OAuth2Error(ex.getMessage()));
        } catch (DataAccessException ex) {
            errorLogService.logDataAccessException(oAuth2User.getName(), new Date(), ex);
            throw new RuntimeException(ex);
        } catch (DataUpdateException ex) {
            errorLogService.logDataUpdateException(oAuth2User.getName(), new Date(), ex);
            throw new RuntimeException(ex);
        } catch (DataConflictException ex) {
            errorLogService.logDataConflictException(oAuth2User.getName(), new Date(), ex);
            throw new RuntimeException(ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) throws RegistrationException, DataAccessException, DataUpdateException, DataConflictException {
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(oAuth2UserRequest.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes());
        if (isEmpty(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        AuthProvider authProvider = byRegistrationId(oAuth2UserRequest.getClientRegistration().getRegistrationId());
        String authProviderId = oAuth2UserInfo.getId();
        User user = userDao.findByAuthProviderId(authProvider, authProviderId);
        if (user == null) {
            user = registerNewUser(authProvider, authProviderId, oAuth2UserInfo.getImageUrl(), oAuth2UserInfo.getName(), oAuth2UserInfo.getEmail());
        } else {
            updateUser(user, oAuth2UserInfo.getImageUrl(), oAuth2UserInfo.getName(), oAuth2UserInfo.getEmail());
        }

        return create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(AuthProvider authProvider, String authProviderId, String authProviderProfileImgUrl, String authProviderUsername, String email) throws RegistrationException, DataAccessException, DataUpdateException, DataConflictException {
        String username = authProvider + "_" + email;

        List<CustomOAuth2ErrorCodes> errorCodes = new ArrayList<>();
        errorCodes.addAll(validateEmailAddress(email));
        errorCodes.addAll(validateUsername(username));
        errorCodes.addAll(validateUser(username, email));

        List<String> results = errorCodes.stream()
                .filter(Objects::nonNull)
                .map(c -> c.errorCode)
                .collect(toList());

        boolean isValid = results.isEmpty();

        if (isValid) {
            StopWatch stopWatch = StopWatch.createStarted();
            //
            // (1) create the new user entity
            //
            User newUser = new User(
                    username, // internal username (can never change)
                    email, // email address (can change, as long as it remains unique globally)
                    authProvider, // auth provider type (should never change)
                    authProviderId, // user's Id at the auth provider (should never change)
                    authProviderProfileImgUrl, // user's profile img URL at the auth provider (can change)
                    authProviderUsername // user's name at the auth provider (can change)
            );
            //
            // (2) generate auth claims for the user
            //
            newUser.setAuthClaim(randomClaimValue());
            newUser.setVerified(false);
            //
            // (3) persist the user entity
            //
            User u = userDao.add(newUser);
            //
            // (4) generate API keys
            //
            ApiKey apiKey = generateApiKey(u.getUsername());
            //
            // (5) generate and send verification token
            //
            log.info("Sending API key recovery email to username={}", username);
            try {
                mailService.sendApiKeyRecoveryEmail(username, apiKey);
            } catch (UsernameNotFoundException | MailException ignored) {
                // ignored
            }
            stopWatch.stop();
            //
            // (6) user registration is complete
            //
            appLogService.logUserRegistration(u.getUsername(), stopWatch);
            return u;
        } else {
            throw new RegistrationException(join("; ", results));
        }
    }

    public ApiKey generateApiKey(String username) throws DataAccessException, DataUpdateException, DataConflictException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        String uuid = UUID.randomUUID().toString();
        String rawApiSecret = generateRandomString(32);
        String secret = passwordEncoder.encode(rawApiSecret);
        ApiKey apiKey = ApiKey.from(user.getId(), uuid, secret);
        return apiKeyDao.add(apiKey);
    }

    private void updateUser(User user, String authProviderProfileImgUrl, String authProviderUsername, String emailAddress) throws DataAccessException, DataConflictException {
        boolean doUpdate = false;
        if (!StringUtils.equals(user.getAuthProviderProfileImgUrl(), authProviderProfileImgUrl)) {
            user.setAuthProviderProfileImgUrl(authProviderProfileImgUrl);
            doUpdate = true;
        }
        if (!StringUtils.equals(user.getAuthProviderUsername(), authProviderUsername)) {
            user.setAuthProviderUsername(authProviderUsername);
            doUpdate = true;
        }
        if (!StringUtils.equals(user.getEmailAddress(), emailAddress)) {
            user.setEmailAddress(emailAddress);
            doUpdate = true;
        }
        if (doUpdate) {
            StopWatch stopWatch = StopWatch.createStarted();
            userDao.update(user);
            stopWatch.stop();
            appLogService.logUserUpdate(user, stopWatch);
        }
    }

    enum CustomOAuth2ErrorCodes {
        INVALID_USERNAME("invalid-username"),
        INVALID_EMAIL("invalid-email"),
        TRY_ANOTHER_METHOD("try-another-method");

        final String errorCode;

        CustomOAuth2ErrorCodes(String errorCode) {
            this.errorCode = errorCode;
        }
    }

    private List<CustomOAuth2ErrorCodes> validateUsername(String username) {
        if (isBlank(username)) {
            return singletonList(INVALID_USERNAME);
        }

        return emptyList();
    }

    private List<CustomOAuth2ErrorCodes> validateEmailAddress(String email) {
        if (isBlank(email)) {
            return singletonList(INVALID_EMAIL);
        }

        return emptyList();
    }

    private List<CustomOAuth2ErrorCodes> validateUser(String username, String email) throws DataAccessException {
        // alreadyExists iff: username or email exists system-wide
        boolean alreadyExists = userDao.checkExists(username, email);
        boolean isValid = !alreadyExists;

        return isValid ? emptyList() : singletonList(TRY_ANOTHER_METHOD);
    }

    private static String randomClaimValue() {
        return randomAlphanumeric(16);
    }
}
