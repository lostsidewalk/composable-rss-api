package com.lostsidewalk.buffy.app.user;

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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.lostsidewalk.buffy.app.user.UserRoles.*;
import static java.util.Set.copyOf;

@Service
@Slf4j
public class ApiUserService implements UserDetailsService {

    @Autowired
    ErrorLogService errorLogService;

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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
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
            return toUserDetails(user, copyOf(implicitAuthorities), this.passwordEncoder);
        } catch (DataAccessException e) {
            log.error("Unable to load user due to data access exception");
            errorLogService.logDataAccessException(username, new Date(), e);
            return null;
        }
    }

    private Set<SimpleGrantedAuthority> gatherImplicitAuthorities(User user) {
        Set<SimpleGrantedAuthority> implicitFeatures = new HashSet<>();

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
    private static UserDetails toUserDetails(User user, Set<SimpleGrantedAuthority> grantedAuthorities, PasswordEncoder passwordEncoder) {
        return new UserDetails() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return grantedAuthorities;
            }

            @Override
            public String getPassword() {
                return passwordEncoder.encode(user.getPassword());
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
}
