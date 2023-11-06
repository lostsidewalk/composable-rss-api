package com.lostsidewalk.buffy.app.auth;

import com.lostsidewalk.buffy.app.utils.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";

    @Autowired
    HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Override
    public final void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        String targetUrl = CookieUtils.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(("/"));

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(targetUrl);

        if (exception instanceof OAuth2AuthenticationException) {
            OAuth2Error oAuth2Error = ((OAuth2AuthenticationException) exception).getError();
            String errorCode = oAuth2Error.getErrorCode();
            builder.queryParam("error", errorCode);
        }

        UriComponents build = builder.build();
        String s = build.toUriString();
        HttpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        RedirectStrategy redirectStrategy = getRedirectStrategy();
        redirectStrategy.sendRedirect(request, response, s);
    }

    @Override
    public final String toString() {
        return "OAuth2AuthenticationFailureHandler{" +
                "httpCookieOAuth2AuthorizationRequestRepository=" + httpCookieOAuth2AuthorizationRequestRepository +
                '}';
    }
}