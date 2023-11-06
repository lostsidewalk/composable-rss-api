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
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collector;

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
    MailService mailService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private ApiKeyDao apiKeyDao;

    @Override
    public final OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        String name = oAuth2User.getName();
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            String message = ex.getMessage();
            throw new OAuth2AuthenticationException(new OAuth2Error("AUTH", message, null));
        } catch (RegistrationException ex) {
            ErrorLogService.logRegistrationException(name, new Date(), ex);
            String message = ex.getMessage();
            throw new OAuth2AuthenticationException(new OAuth2Error(message));
        } catch (DataAccessException ex) {
            ErrorLogService.logDataAccessException(name, new Date(), ex);
            throw new RuntimeException(ex);
        } catch (DataUpdateException ex) {
            ErrorLogService.logDataUpdateException(name, new Date(), ex);
            throw new RuntimeException(ex);
        } catch (DataConflictException ex) {
            ErrorLogService.logDataConflictException(name, new Date(), ex);
            throw new RuntimeException(ex);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2AuthenticatedPrincipal oAuth2User) throws RegistrationException, DataAccessException, DataUpdateException, DataConflictException {
        ClientRegistration clientRegistration = oAuth2UserRequest.getClientRegistration();
        String registrationId = clientRegistration.getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, attributes);
        String email = oAuth2UserInfo.getEmail();
        if (isEmpty(email)) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        AuthProvider authProvider = byRegistrationId(registrationId);
        String authProviderId = oAuth2UserInfo.getId();
        User user = userDao.findByAuthProviderId(authProvider, authProviderId);
        String imageUrl = oAuth2UserInfo.getImageUrl();
        String name = oAuth2UserInfo.getName();
        if (user == null) {
            user = registerNewUser(authProvider, authProviderId, imageUrl, name, email);
        } else {
            updateUser(user, imageUrl, name, email);
        }

        return create(user, attributes);
    }

    @SuppressWarnings("NestedMethodCall")
    private User registerNewUser(AuthProvider authProvider, String authProviderId, String authProviderProfileImgUrl, String authProviderUsername, String email) throws RegistrationException, DataAccessException, DataUpdateException, DataConflictException {
        String username = authProvider + "_" + email;

        Collection<CustomOAuth2ErrorCodes> errorCodes = new ArrayList<>(3);
        errorCodes.addAll(validateEmailAddress(email));
        errorCodes.addAll(validateUsername(username));
        errorCodes.addAll(validateUser(username, email));

        Collector<String, ?, List<String>> stringListCollector = toList();
        List<String> results = errorCodes.stream()
                .filter(Objects::nonNull)
                .map(errorCode -> errorCode.errorCode)
                .collect(stringListCollector);

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
            String authClaim = randomClaimValue();
            newUser.setAuthClaim(authClaim);
            newUser.setVerified(false);
            //
            // (3) persist the user entity
            //
            User u = userDao.add(newUser);
            //
            // (4) generate API keys
            //
            ApiKey apiKey = generateApiKey(username);
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
            AppLogService.logUserRegistration(username, stopWatch);
            return u;
        } else {
            String message = join("; ", results);
            throw new RegistrationException(message);
        }
    }

    private ApiKey generateApiKey(String username) throws DataAccessException, DataUpdateException, DataConflictException {
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

    @SuppressWarnings("NestedMethodCall")
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
            AppLogService.logUserUpdate(user, stopWatch);
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

        @Override
        public String toString() {
            return "CustomOAuth2ErrorCodes{" +
                    "errorCode='" + errorCode + '\'' +
                    '}';
        }
    }

    private static List<CustomOAuth2ErrorCodes> validateUsername(String username) {
        if (isBlank(username)) {
            return singletonList(INVALID_USERNAME);
        }

        return emptyList();
    }

    private static List<CustomOAuth2ErrorCodes> validateEmailAddress(String email) {
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

    @Override
    public final String toString() {
        return "CustomOAuth2UserService{" +
                "passwordEncoder=" + passwordEncoder +
                ", mailService=" + mailService +
                ", userDao=" + userDao +
                ", apiKeyDao=" + apiKeyDao +
                '}';
    }
}
