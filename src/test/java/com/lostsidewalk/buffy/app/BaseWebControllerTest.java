package com.lostsidewalk.buffy.app;


import com.lostsidewalk.buffy.*;
import com.lostsidewalk.buffy.app.auth.AuthService;
import com.lostsidewalk.buffy.app.mail.MailService;
import com.lostsidewalk.buffy.app.order.*;
import com.lostsidewalk.buffy.app.settings.SettingsService;
import com.lostsidewalk.buffy.app.token.TokenService;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import com.lostsidewalk.buffy.app.user.LocalUserService;
import com.lostsidewalk.buffy.app.user.UserRoles;
import com.lostsidewalk.buffy.auth.ApiKeyDao;
import com.lostsidewalk.buffy.auth.FeatureDao;
import com.lostsidewalk.buffy.auth.RoleDao;
import com.lostsidewalk.buffy.auth.UserDao;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfoDao;
import com.lostsidewalk.buffy.post.StagingPostDao;
import com.lostsidewalk.buffy.queue.FeedCredentialsDao;
import com.lostsidewalk.buffy.queue.QueueDefinitionDao;
import com.lostsidewalk.buffy.subscription.SubscriptionDefinitionDao;
import com.lostsidewalk.buffy.subscription.SubscriptionMetricsDao;
import com.lostsidewalk.buffy.thumbnail.ThumbnailDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

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
    MailService mailService;
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
    FeedCredentialsDao feedCredentialsDao;

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
}