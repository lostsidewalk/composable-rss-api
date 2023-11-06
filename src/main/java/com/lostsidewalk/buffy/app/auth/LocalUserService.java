package com.lostsidewalk.buffy.app.auth;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.ErrorLogService;
import com.lostsidewalk.buffy.app.audit.RegistrationException;
import com.lostsidewalk.buffy.auth.*;
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
import java.util.*;
import java.util.stream.Collector;

import static com.google.common.collect.Sets.union;
import static com.lostsidewalk.buffy.app.auth.UserRoles.*;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Slf4j
public class LocalUserService implements UserDetailsService {

    @Autowired
    UserDao userDao;

    @Autowired
    RoleDao roleDao;

    @Autowired
    FeatureDao featureDao;

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
            Set<SimpleGrantedAuthority> grantedAuthorities = gatherGrantedAuthorities(user);
            Set<SimpleGrantedAuthority> implicitAuthorities = gatherImplicitAuthorities(user);
            Sets.SetView<SimpleGrantedAuthority> union = union(grantedAuthorities, implicitAuthorities);
            ImmutableSet<SimpleGrantedAuthority> copy = union.immutableCopy();
            return toUserDetails(user, copy, passwordEncoder);
        } catch (DataAccessException e) {
            log.error("Unable to load user due to data access exception");
            ErrorLogService.logDataAccessException(username, new Date(), e);
            return null;
        }
    }

    //
    // role/feature assignation
    //
    private Set<SimpleGrantedAuthority> gatherGrantedAuthorities(User user) throws DataAccessException {
        Collection<String> grantedFeatures = new HashSet<>(5);
        String username = user.getUsername();
        List<Role> roles = roleDao.findByUsername(username);
        if (isNotEmpty(roles)) {
            for (Role r : roles) {
                String name = r.getName();
                List<String> byRolename = featureDao.findByRolename(name);
                grantedFeatures.addAll(byRolename);
            }
        }

        Collector<SimpleGrantedAuthority, ?, Set<SimpleGrantedAuthority>> simpleGrantedAuthoritySetCollector = toSet();
        return grantedFeatures.stream().map(SimpleGrantedAuthority::new).collect(simpleGrantedAuthoritySetCollector);
    }

    private Set<SimpleGrantedAuthority> gatherImplicitAuthorities(User user) {
        Set<SimpleGrantedAuthority> implicitFeatures = new HashSet<>(5);

        // all users get the 'unverified' role
        implicitFeatures.add(UNVERIFIED_AUTHORITY);

        // verified users get the 'verified' role
        if (user.isVerified()) {
            implicitFeatures.add(VERIFIED_AUTHORITY);
        }

        // all users get the 'development' role when that property is enabled
        if (isDevelopment) {
            implicitFeatures.add(DEV_AUTHORITY);
        }

        return implicitFeatures;
    }

    //
    // registration
    //
    @SuppressWarnings("NestedMethodCall")
    public final void registerUser(String username, String email, String password) throws RegistrationException, DataAccessException, DataUpdateException, DataConflictException {

        Collection<String> errors = new ArrayList<>(4);
        errors.addAll(validateUsername(username));
        errors.addAll(validateEmailAddress(email));
        errors.addAll(validatePassword(password));
        errors.addAll(validateUser(username, email));

        List<String> results = errors.stream()
                .filter(Objects::nonNull)
                .collect(toList());

        boolean isValid = results.isEmpty();

        if (isValid) {
            User newUser = new User(username, password, email);
            userDao.add(newUser);
            log.info("Registered user, username={}, email={}", username, email);
        } else {
            throw new RegistrationException(join("; ", results));
        }
    }

    private static List<String> validateUsername(String username) {
        if (isBlank(username)) {
            return singletonList("Username must not be blank");
        }

        return emptyList();
    }

    private static List<String> validateEmailAddress(String email) {
        if (isBlank(email)) {
            return singletonList("Email address must not be blank");
        }

        return emptyList();
    }

    private static List<String> validatePassword(String password) {
        if (isBlank(password)) {
            return singletonList("Password must not be blank");
        }
        if (password.length() < 6) {
            return singletonList("Password must be greater than 6 characters");
        }

        return emptyList();
    }

    private List<String> validateUser(String username, String email) throws DataAccessException {
        boolean alreadyExists;
        alreadyExists = userDao.checkExists(username, email);
        if (alreadyExists) {
            return singletonList("Username and email address must both be unique.");
        }

        return emptyList();
    }

    public final void deregisterUser(String username) throws DataAccessException, DataUpdateException {
        userDao.deleteByName(username);
    }

    //
    // verification
    //
    public final void markAsVerified(String username) throws DataAccessException, DataUpdateException {
        userDao.setVerified(username, true);
    }

    //
    // utility methods
    //
    private static UserDetails toUserDetails(User user, Collection<SimpleGrantedAuthority> grantedAuthorities, PasswordEncoder passwordEncoder) {
        return new UserDetails() {
            @Serial
            private static final long serialVersionUID = 23462423412357723L;

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
        return "LocalUserService{" +
                "userDao=" + userDao +
                ", roleDao=" + roleDao +
                ", featureDao=" + featureDao +
                ", passwordEncoder=" + passwordEncoder +
                ", isDevelopment=" + isDevelopment +
                '}';
    }
}
