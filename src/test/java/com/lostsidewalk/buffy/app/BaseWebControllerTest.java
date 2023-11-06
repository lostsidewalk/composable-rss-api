package com.lostsidewalk.buffy.app;


import com.google.common.collect.ImmutableList;
import com.lostsidewalk.buffy.FrameworkConfigDao;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.ThemeConfigDao;
import com.lostsidewalk.buffy.app.auth.ApiUserService;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.auth.LocalUserService;
import com.lostsidewalk.buffy.app.credentials.QueueCredentialsService;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.app.settings.SettingsService;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import com.lostsidewalk.buffy.auth.*;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfoDao;
import com.lostsidewalk.buffy.post.StagingPostDao;
import com.lostsidewalk.buffy.queue.QueueCredentialDao;
import com.lostsidewalk.buffy.queue.QueueDefinitionDao;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinitionDao;
import com.lostsidewalk.buffy.subscription.SubscriptionMetricsDao;
import com.lostsidewalk.buffy.thumbnail.ThumbnailDao;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import redis.clients.jedis.JedisPool;

import java.io.Serial;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

import static com.lostsidewalk.buffy.app.auth.UserRoles.*;
import static java.time.temporal.ChronoUnit.DAYS;


@Slf4j
@SuppressWarnings({"ProtectedField", "AbstractClassWithoutAbstractMethods"}) // not instantiable
public abstract class BaseWebControllerTest {

    @Autowired
    protected MockMvc mockMvc;
    //
    // transaction manager
    //
    @MockBean
    PlatformTransactionManager platformTransactionManager;
    //
    // service layer
    //
    @MockBean
    SettingsService settingsService;

    @MockBean
    protected
    AuthService authService;

    @MockBean
    protected
    TokenService tokenService;

    @MockBean
    protected
    LocalUserService userService;

    @MockBean
    protected
    ApiUserService apiUserService;

    @MockBean
    MailService mailService;

    @MockBean
    protected
    StagingPostService stagingPostService;

    @MockBean
    protected
    QueueDefinitionService queueDefinitionService;

    @MockBean
    protected
    QueueCredentialsService queueCredentialsService;

    @MockBean
    protected
    PostPublisher postPublisher;
    //
    // persistence layer
    //
    @MockBean
    FeatureDao featureDao;

    @MockBean
    QueueDefinitionDao queueDefinitionDao;

    @MockBean
    QueueCredentialDao queueCredentialDao;

    @MockBean
    FeedDiscoveryInfoDao feedDiscoveryInfoDao;

    @MockBean
    FrameworkConfigDao frameworkConfigDao;

    @MockBean
    ThemeConfigDao themeConfigDao;

    @MockBean
    StagingPostDao stagingPostDao;

    @MockBean
    SubscriptionDefinitionDao subscriptionDefinitionDao;

    @MockBean
    SubscriptionMetricsDao subscriptionMetricsDao;

    @MockBean
    ThumbnailDao thumbnailDao;

    @MockBean
    RoleDao roleDao;

    @MockBean
    UserDao userDao;

    @MockBean
    ApiKeyDao apiKeyDao;

    @MockBean
    JedisPool jedisPool;

    @MockBean
    JedisBasedProxyManager proxyManager;

    protected static final JwtUtil TEST_JWT_UTIL = new JwtUtil() {
        @Override
        public String extractUsername() {
            return "me";
        }

        @Override
        public Date extractExpiration() {
            Instant nowPlus30Days = Instant.now().plus(30L, DAYS);
            return Date.from(nowPlus30Days);
        }

        @Override
        public String extractValidationClaim() {
            return "b4223bd3427db93956acaadf9e425dd259bfb11dac44234604c819dbbf75e180";
        }

        @Override
        public Boolean isTokenValid() {
            return true;
        }

        @Override
        public Boolean isTokenExpired() {
            return false;
        }

        @Override
        public void requireNonExpired() {}

        @Override
        public void validateToken() {
            // pass
        }
    };

    private static final Collection<GrantedAuthority> TEST_AUTHORITIES = ImmutableList.of(
        UNVERIFIED_AUTHORITY,
        VERIFIED_AUTHORITY,
        DEV_AUTHORITY
    );

    static final UserDetails TEST_USER_DETAILS = new UserDetails() {
        @Serial
        private static final long serialVersionUID = 23462457282723L;

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return TEST_AUTHORITIES;
        }

        @Override
        public String getPassword() {
            return new BCryptPasswordEncoder().encode("testPassword");
        }

        @Override
        public String getUsername() {
            return "me";
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    };

    protected static final User TEST_API_USER = new User(1L, "me", "testPassword", "me@localhost");

    protected static final ApiKey TEST_API_KEY_OBJ = ApiKey.from(1L, "testApiKey", "testApiSecret");

    private static final Collection<GrantedAuthority> TEST_API_AUTHORITIES = ImmutableList.of(
            API_UNVERIFIED_AUTHORITY,
            API_VERIFIED_AUTHORITY,
            API_DEV_AUTHORITY
    );

    protected static final UserDetails TEST_API_USER_DETAILS = new UserDetails() {

        @Serial
        private static final long serialVersionUID = 234624234146723L;

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return TEST_API_AUTHORITIES;
        }

        @Override
        public String getPassword() {
            return new BCryptPasswordEncoder().encode("testPassword");
        }

        @Override
        public String getUsername() {
            return "me";
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    };

    @Override
    public String toString() {
        return "BaseWebControllerTest{" +
                "mockMvc=" + mockMvc +
                ", platformTransactionManager=" + platformTransactionManager +
                ", settingsService=" + settingsService +
                ", authService=" + authService +
                ", tokenService=" + tokenService +
                ", userService=" + userService +
                ", apiUserService=" + apiUserService +
                ", mailService=" + mailService +
                ", stagingPostService=" + stagingPostService +
                ", queueDefinitionService=" + queueDefinitionService +
                ", queueCredentialsService=" + queueCredentialsService +
                ", postPublisher=" + postPublisher +
                ", featureDao=" + featureDao +
                ", queueDefinitionDao=" + queueDefinitionDao +
                ", queueCredentialDao=" + queueCredentialDao +
                ", feedDiscoveryInfoDao=" + feedDiscoveryInfoDao +
                ", frameworkConfigDao=" + frameworkConfigDao +
                ", themeConfigDao=" + themeConfigDao +
                ", stagingPostDao=" + stagingPostDao +
                ", subscriptionDefinitionDao=" + subscriptionDefinitionDao +
                ", subscriptionMetricsDao=" + subscriptionMetricsDao +
                ", thumbnailDao=" + thumbnailDao +
                ", roleDao=" + roleDao +
                ", userDao=" + userDao +
                ", apiKeyDao=" + apiKeyDao +
                ", jedisPool=" + jedisPool +
                ", proxyManager=" + proxyManager +
                '}';
    }
}
