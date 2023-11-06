package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.app.audit.TokenValidationException;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.Collection;

import static com.lostsidewalk.buffy.app.auth.HashingUtils.sha256;
import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Component
class JwtProcessor {

    @Autowired
    LocalUserService userService;

    final void processJwt(JwtUtil jwtUtil, String username, String userValidationClaim, String jwt) throws TokenValidationException {
        String validationClaimHash = jwtUtil.extractValidationClaim();
        if (isNotBlank(validationClaimHash)) {
            Charset charset = defaultCharset();
            String userValidationClaimHash = sha256(userValidationClaim, charset);
            if (equalsIgnoreCase(userValidationClaimHash, validationClaimHash)) {
                UserDetails userDetails = userService.loadUserByUsername(username);
                Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
                WebAuthenticationToken authToken = new WebAuthenticationToken(userDetails, jwt, authorities);
                authToken.setDetails(userDetails);
                //
                // !! ACHTUNG !! POINT OF NO RETURN !!
                //
                SecurityContext context = getContext();
                context.setAuthentication(authToken);
                //
                // !! YOU'VE DONE IT NOW !!
                //
            } else {
                throw new TokenValidationException("Token validation claim is outdated");
            }
            // Note: if those values don't match, this means that the value was changed on the user record *after* the token was issued;
            // (this is a way of invalidating unexpired keys in the wild)
        } else {
            throw new TokenValidationException("Token validation claim is missing");
        }
    }

    @Override
    public final String toString() {
        return "JwtProcessor{" +
                "userService=" + userService +
                '}';
    }
}
