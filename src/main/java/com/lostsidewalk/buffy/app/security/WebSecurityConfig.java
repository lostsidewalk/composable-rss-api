package com.lostsidewalk.buffy.app.security;

import com.lostsidewalk.buffy.app.auth.*;
import com.lostsidewalk.buffy.app.user.CustomOAuth2UserService;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static java.util.Collections.singletonList;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class WebSecurityConfig {

    @Autowired
    LocalUserService userDetailsService;

    @Autowired
    AuthTokenFilter authTokenFilter;

    @Autowired
    RateLimitingFilter rateLimitingFilter;

    @Autowired
    PasswordEncoder passwordEncoder;

    @PostConstruct
    void postConstruct() {
        log.info("Web security configuration initializating.. compRssOriginUrl={}", compRssOriginUrl);
    }

    @SuppressWarnings("DesignForExtension")
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    @SuppressWarnings({"MethodMayBeStatic", "DesignForExtension"})
    @SneakyThrows
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) {
        return authConfiguration.getAuthenticationManager();
    }

    @SuppressWarnings({"MethodMayBeStatic", "DesignForExtension"})
    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired
    private OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private static AuthenticationEntryPoint currentUserEntryPoint() {
        return (request, response, authException) -> response.setStatus(SC_UNAUTHORIZED);
    }

    /**
     * private static final String CONTENT_SECURITY_POLICY_DIRECTIVES =
     *         "default-src 'self';" +
     *         "connect-src 'self' http://localhost:8080 ws://192.168.86.180:3000/ws;img-src 'self' data: https://* http://*;" +
     *         "style-src 'unsafe-inline' https://fonts.googleapis.com;base-uri 'self';" +
     *         "form-action 'self';" +
     *         "font-src https://fonts.gstatic.com http://localhost:3000";
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    @Value("${comprss.originUrl}")
    String compRssOriginUrl;

    @SneakyThrows
    @SuppressWarnings({"DesignForExtension", "NestedMethodCall", "ChainedMethodCall"})
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) {

        http
                .logout(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
//				.headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.contentSecurityPolicy(CONTENT_SECURITY_POLICY_DIRECTIVES))
                .cors(corsCustomizer -> corsCustomizer.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    configuration.setAllowedOriginPatterns(singletonList(compRssOriginUrl));
                    configuration.setAllowedMethods(singletonList("*"));
                    configuration.setAllowedHeaders(singletonList("*"));
                    configuration.setAllowCredentials(true);
                    return configuration;
                }))
                .authorizeHttpRequests(a ->
                        a.requestMatchers("/").permitAll() // index
                                .requestMatchers("/authenticate").permitAll() // user authentication (login)
                                .requestMatchers("/oauth2/**").authenticated() // oauth2
                                .requestMatchers("/pw_reset").permitAll() // password reset (init)
                                .requestMatchers("/pw_reset/**").permitAll() // password reset w/token (get-back)
                                .requestMatchers("/deauthenticate").permitAll() // user de-authentication (logout)
                                .requestMatchers("/register").permitAll() // user registration (init)
                                .requestMatchers("/verify/**").permitAll() // verification (i.e., user registration get-back)
                                .requestMatchers("/stripe").permitAll() // stripe callback (payment)
                                // permit actuators
                                .requestMatchers("/actuator").permitAll()
                                .requestMatchers("/actuator/**").permitAll()
                                // permit options calls
                                .requestMatchers(OPTIONS, "/**").permitAll() // OPTIONS calls are validated downstream by checking for the presence of required headers
                                // permit image proxy calls
                                .requestMatchers("/proxy/unsecured/**").permitAll()
                                // permit API docs V3
                                .requestMatchers("/v3/api-docs").permitAll()
                                .requestMatchers("/v3/api-docs/**").permitAll()
                                // (all others require authentication)
                                .anyRequest().authenticated()
                ).sessionManagement(sessionManagementCustomizer -> sessionManagementCustomizer.sessionCreationPolicy(STATELESS))
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(currentUserEntryPoint(), new AntPathRequestMatcher("/**")))
                .oauth2Login(o ->
                        o.authorizationEndpoint(e -> e.baseUri("/oauth2/authorize")
                                        .authorizationRequestRepository(cookieAuthorizationRequestRepository()))
                                .redirectionEndpoint(e -> e.baseUri("/oauth2/callback/*"))
                                .userInfoEndpoint(e -> e.userService(customOAuth2UserService))
                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                .failureHandler(oAuth2AuthenticationFailureHandler));
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(rateLimitingFilter, AuthTokenFilter.class);

        return http.build();
    }

    @Override
    public final String toString() {
        return "WebSecurityConfig{" +
                "userDetailsService=" + userDetailsService +
                ", authTokenFilter=" + authTokenFilter +
                ", rateLimitingFilter=" + rateLimitingFilter +
                ", passwordEncoder=" + passwordEncoder +
                ", customOAuth2UserService=" + customOAuth2UserService +
                ", oAuth2AuthenticationSuccessHandler=" + oAuth2AuthenticationSuccessHandler +
                ", oAuth2AuthenticationFailureHandler=" + oAuth2AuthenticationFailureHandler +
                ", compRssOriginUrl='" + compRssOriginUrl + '\'' +
                '}';
    }
}
