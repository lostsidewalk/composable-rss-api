package com.lostsidewalk.buffy.app.settings;

import com.lostsidewalk.buffy.*;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.SettingsResponse;
import com.lostsidewalk.buffy.auth.ApiKey;
import com.lostsidewalk.buffy.auth.ApiKeyDao;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.auth.UserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class SettingsService {

    @Autowired
    UserDao userDao;

    @Autowired
    private FrameworkConfigDao frameworkConfigDao;

    @Autowired
    private ApiKeyDao apiKeyDao;

    @SuppressWarnings("NestedMethodCall")
    public final SettingsResponse getFrameworkConfig(String username) throws DataAccessException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return SettingsResponse.from(
                user.getUsername(),
                user.getEmailAddress(),
                user.getAuthProvider(),
                user.getAuthProviderProfileImgUrl(),
                user.getAuthProviderUsername(),
                frameworkConfigDao.findByUserId(user.getId()),
                Optional.ofNullable(apiKeyDao.findByUserId(user.getId()))
                        .map(ApiKey::getApiKey)
                        .orElse(null)
        );
    }

    public final void updateFrameworkConfig(String username, SettingsUpdateRequest updateRequest) throws DataAccessException, DataUpdateException, DataConflictException {
        User user = userDao.findByName(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        Long id = user.getId();
        FrameworkConfig frameworkConfig = updateRequest.getFrameworkConfig();
        if (frameworkConfig != null) {
            frameworkConfig.setUserId(id);
            log.debug("Updating framework configuration for userId={}", id);
            frameworkConfigDao.save(frameworkConfig);
        }

        log.debug("Framework configuration updated for userId={}", id);
    }

    @Override
    public final String toString() {
        return "SettingsService{" +
                "userDao=" + userDao +
                ", frameworkConfigDao=" + frameworkConfigDao +
                ", apiKeyDao=" + apiKeyDao +
                '}';
    }
}
