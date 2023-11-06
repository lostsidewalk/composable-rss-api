package com.lostsidewalk.buffy.app.user;

import com.lostsidewalk.buffy.auth.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.lostsidewalk.buffy.app.auth.UserRoles.VERIFIED_AUTHORITY;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;


@Slf4j
class UserPrincipal implements OAuth2User, UserDetails {

    @Serial
    private static final long serialVersionUID = 114236788456437323L;

    private final Long id;
    private final String username;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    private UserPrincipal(Long id, String username, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    @SuppressWarnings({"NestedMethodCall", "WeakerAccess"})
    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmailAddress(),
                user.getPassword(),
                Collections.singletonList(VERIFIED_AUTHORITY)
        );
    }

    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = create(user);
        //noinspection AccessingNonPublicFieldOfAnotherObject
        userPrincipal.attributes = unmodifiableMap(attributes);
        return userPrincipal;
    }

    public final Long getId() {
        return id;
    }

    public final String getEmail() {
        return email;
    }

    @Override
    public final String getPassword() {
        return password;
    }

    @Override
    public final String getUsername() {
        return username;
    }

    @Override
    public final boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public final boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public final boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public final boolean isEnabled() {
        return true;
    }

    @Override
    public final Collection<? extends GrantedAuthority> getAuthorities() {
        return unmodifiableCollection(authorities);
    }

    @Override
    public final Map<String, Object> getAttributes() {
        return unmodifiableMap(attributes);
    }

    @Override
    public final String getName() {
        return String.valueOf(id);
    }

    @Override
    public final String toString() {
        return "UserPrincipal{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", authorities=" + authorities +
                ", attributes=" + attributes +
                '}';
    }
}