package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.audit.ErrorLogService;
import com.lostsidewalk.buffy.auth.User;
import com.lostsidewalk.buffy.auth.UserDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.lostsidewalk.buffy.app.auth.UserRoles.*;
import static java.util.Set.copyOf;


@Service
@Slf4j
public class ApiUserService implements UserDetailsService {

    @Autowired
    UserDao userDao;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${comprss.development:false}")
    boolean isDevelopment;

    //
    // user loading
    //
    @Override
    public final UserDetails loadUserByUsername(String username) {
        if ("NONE_PROVIDED".equals(username)) {
            throw new UsernameNotFoundException(username);
        }

        User user;
        try {
            user = userDao.findByName(username);
            if (user == null) {
                throw new UsernameNotFoundException(username);
            }
            Set<SimpleGrantedAuthority> implicitAuthorities = gatherImplicitAuthorities(user);
            Set<SimpleGrantedAuthority> grantedAuthorities = copyOf(implicitAuthorities);
            return toUserDetails(user, grantedAuthorities, passwordEncoder);
        } catch (DataAccessException e) {
            log.error("Unable to load user due to data access exception");
            ErrorLogService.logDataAccessException(username, new Date(), e);
            return null;
        }
    }

    private Set<SimpleGrantedAuthority> gatherImplicitAuthorities(User user) {
        Set<SimpleGrantedAuthority> implicitFeatures = new HashSet<>(3);

        // all API users get the 'api_unverified' role
        implicitFeatures.add(API_UNVERIFIED_AUTHORITY);

        // verified API users get the 'api_verified' role
        if (user.isVerified()) {
            implicitFeatures.add(API_VERIFIED_AUTHORITY);
        }

        // all API users get the 'api_development' role when that property is enabled
        if (isDevelopment) {
            implicitFeatures.add(API_DEV_AUTHORITY);
        }

        return implicitFeatures;
    }

    //
    // utility methods
    //
    private static UserDetails toUserDetails(User user, Collection<SimpleGrantedAuthority> grantedAuthorities, PasswordEncoder passwordEncoder) {
        return new UserDetails() {
            @Serial
            private static final long serialVersionUID = 3413823442343242298L;

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return grantedAuthorities;
            }

            @Override
            public String getPassword() {
                String password = user.getPassword();
                return passwordEncoder.encode(password);
            }

            @Override
            public String getUsername() {
                return user.getUsername();
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

    @Override
    public final String toString() {
        return "ApiUserService{" +
                "userDao=" + userDao +
                ", passwordEncoder=" + passwordEncoder +
                ", isDevelopment=" + isDevelopment +
                '}';
    }
}
