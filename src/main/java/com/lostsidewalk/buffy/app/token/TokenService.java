package com.lostsidewalk.buffy.app.token;

import com.lostsidewalk.buffy.app.audit.TokenValidationException;
import com.lostsidewalk.buffy.app.model.TokenType;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import static io.jsonwebtoken.SignatureAlgorithm.HS256;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

@Service
@Slf4j
public class TokenService {

    @Value("${token.service.secret}")
    private String secretKey;

    @SuppressWarnings("ChainedMethodCall")
    public final JwtUtil instanceFor(TokenType tokenType, String token) throws TokenValidationException {

        Claims claims;
        try {
            String name = tokenType.name();
            Jws<Claims> claimsJws = Jwts.parser().requireAudience(name).setSigningKey(secretKey).parseClaimsJws(token);
            claims = claimsJws.getBody();
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | UnsupportedJwtException |
                 IllegalArgumentException e) {
            String rootCauseMessage = getRootCauseMessage(e);
            throw new TokenValidationException("Unable to parse token due to: " + rootCauseMessage);
        }

        JwtUtil jwtUtil = new JwtUtil() {

            @Override
            public String extractUsername() {
                return extractClaim(Claims::getSubject);
            }

            @Override
            public Date extractExpiration() {
                return extractClaim(Claims::getExpiration);
            }

            @Override
            public String extractValidationClaim() {
                //noinspection NestedMethodCall
                return extractClaim(claims -> claims.get(tokenType.tokenName, String.class));
            }

            @SuppressWarnings("MethodMayBeStatic")
            private <T> T extractClaim(Function<? super Claims, T> claimsResolver) {
                return claimsResolver.apply(claims);
            }

            @Override
            public Boolean isTokenValid() {
                return !isNull(claims);
            }

            @Override
            public Boolean isTokenExpired() {
                return extractExpiration().before(new Date());
            }

            @Override
            public void requireNonExpired() throws TokenValidationException {
                if (isTokenExpired()) {
                    throw new TokenValidationException("Token is expired");
                }
            }

            @Override
            public void validateToken() throws TokenValidationException {
                if (!isTokenValid()) {
                    throw new TokenValidationException("Not a valid JWT token");
                }
            }
        };

        jwtUtil.validateToken();

        return jwtUtil;
    }

    //
    // methods to query the token
    //
    public final String generateToken(Map<String, Object> claims, String subject, TokenType tokenType) {
        long date = currentTimeMillis();
        Date expiration = tokenType.expirationBuilder.apply(date);
        String name = tokenType.name();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(date))
                .setExpiration(expiration)
                .setAudience(name)
                .signWith(HS256, secretKey).compact();
    }

    @Override
    public final String toString() {
        return "TokenService{" +
                "secretKey='" + secretKey + '\'' +
                '}';
    }

    @SuppressWarnings("unused")
    public interface JwtUtil {
        String extractUsername();

        Date extractExpiration();

        String extractValidationClaim();

        Boolean isTokenValid();

        Boolean isTokenExpired();

        void requireNonExpired() throws TokenValidationException;

        void validateToken() throws TokenValidationException;
    }
}