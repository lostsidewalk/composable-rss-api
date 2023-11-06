package com.lostsidewalk.buffy.app.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

import static org.springframework.security.core.SpringSecurityCoreVersion.SERIAL_VERSION_UID;

@Slf4j
class WebAuthenticationToken extends AbstractAuthenticationToken {

    WebAuthenticationToken(Serializable principal, Serializable credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true); // must use super, as we override
    }

    @Serial
    private static final long serialVersionUID = SERIAL_VERSION_UID;

    private final Serializable principal;

    private Serializable credentials;

    @Override
    public final Object getCredentials() {
        return credentials;
    }

    @Override
    public final Object getPrincipal() {
        return principal;
    }

    @Override
    public final void setAuthenticated(boolean isAuthenticated) {
        Assert.isTrue(!isAuthenticated,
                "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        super.setAuthenticated(false);
    }

    @Override
    public final void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }

    @Override
    public final String toString() {
        return "WebAuthenticationToken{" +
                "principal=" + principal +
                ", credentials=" + credentials +
                '}';
    }
}
