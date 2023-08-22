package com.lostsidewalk.buffy.app;


import com.lostsidewalk.buffy.*;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.credentials.QueueCredentialsService;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.app.order.*;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.app.settings.SettingsService;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import com.lostsidewalk.buffy.app.user.ApiUserService;
import com.lostsidewalk.buffy.app.user.LocalUserService;
import com.lostsidewalk.buffy.app.user.UserRoles;
import com.lostsidewalk.buffy.auth.*;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfoDao;
import com.lostsidewalk.buffy.post.StagingPostDao;
import com.lostsidewalk.buffy.queue.QueueCredentialDao;
import com.lostsidewalk.buffy.queue.QueueDefinitionDao;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinitionDao;
import com.lostsidewalk.buffy.subscription.SubscriptionMetricsDao;
import com.lostsidewalk.buffy.thumbnail.ThumbnailDao;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import redis.clients.jedis.JedisPool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import static java.time.temporal.ChronoUnit.DAYS;

class BaseWebControllerTest {

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
//    @MockBean
//    AuditService auditService;

    @MockBean
    StripeOrderService stripeOrderService;

    @MockBean
    SettingsService settingsService;

    @MockBean
    AuthService authService;

    @MockBean
    TokenService tokenService;

    @MockBean
    LocalUserService userService;

    @MockBean
    ApiUserService apiUserService;

    @MockBean
    MailService mailService;

    @MockBean
    StagingPostService stagingPostService;

    @MockBean
    QueueDefinitionService queueDefinitionService;

    @MockBean
    QueueCredentialsService queueCredentialsService;
    //
    // stripe callback queue processors
    //
    @MockBean
    StripeCallbackQueueProcessor stripeCallbackQueueProcessor;

    @MockBean
    CustomerEventQueueProcessor customerEventQueueProcessor;

    @MockBean
    StripeCustomerHandler stripeCustomerHandler;

    @MockBean
    StripeInvoiceHandler stripeInvoiceHandler;
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

    protected static final Collection<GrantedAuthority> TEST_AUTHORITIES = new ArrayList<>();
    static {
        TEST_AUTHORITIES.add(UserRoles.UNVERIFIED_AUTHORITY);
        TEST_AUTHORITIES.add(UserRoles.VERIFIED_AUTHORITY);
        TEST_AUTHORITIES.add(UserRoles.SUBSCRIBER_AUTHORITY);
        TEST_AUTHORITIES.add(UserRoles.DEV_AUTHORITY);
    }

    protected static final UserDetails TEST_USER_DETAILS = new UserDetails() {
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

    protected static final Collection<GrantedAuthority> TEST_API_AUTHORITIES = new ArrayList<>();
    static {
        TEST_API_AUTHORITIES.add(UserRoles.API_UNVERIFIED_AUTHORITY);
        TEST_API_AUTHORITIES.add(UserRoles.API_VERIFIED_AUTHORITY);
        TEST_API_AUTHORITIES.add(UserRoles.API_SUBSCRIBER_AUTHORITY);
        TEST_API_AUTHORITIES.add(UserRoles.API_DEV_AUTHORITY);
    }

    protected static final UserDetails TEST_API_USER_DETAILS = new UserDetails() {

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
}
