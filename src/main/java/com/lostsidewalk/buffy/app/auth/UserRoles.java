package com.lostsidewalk.buffy.app.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


@Slf4j
public class UserRoles {
    //
    // roles for application service access
    //
    private static final String UNVERIFIED_ROLE = "ROLE_UNVERIFIED";

    private static final String VERIFIED_ROLE = "ROLE_VERIFIED";

    private static final String DEV_ROLE = "ROLE_DEV";

    public static final SimpleGrantedAuthority UNVERIFIED_AUTHORITY = new SimpleGrantedAuthority(UNVERIFIED_ROLE);

    public static final SimpleGrantedAuthority VERIFIED_AUTHORITY = new SimpleGrantedAuthority(VERIFIED_ROLE);

    public static final SimpleGrantedAuthority DEV_AUTHORITY = new SimpleGrantedAuthority(DEV_ROLE);
    //
    // roles for API access
    //
    private static final String API_UNVERIFIED_ROLE = "API_ROLE_UNVERIFIED";

    private static final String API_VERIFIED_ROLE = "API_ROLE_VERIFIED";

    private static final String API_DEV_ROLE = "API_ROLE_DEV";

    public static final SimpleGrantedAuthority API_UNVERIFIED_AUTHORITY = new SimpleGrantedAuthority(API_UNVERIFIED_ROLE);

    public static final SimpleGrantedAuthority API_VERIFIED_AUTHORITY = new SimpleGrantedAuthority(API_VERIFIED_ROLE);

    public static final SimpleGrantedAuthority API_DEV_AUTHORITY = new SimpleGrantedAuthority(API_DEV_ROLE);
}
