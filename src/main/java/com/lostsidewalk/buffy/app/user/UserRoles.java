package com.lostsidewalk.buffy.app.user;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class UserRoles {
    //
    // roles for application service access
    //
    public static final String UNVERIFIED_ROLE = "ROLE_UNVERIFIED";

    public static final String VERIFIED_ROLE = "ROLE_VERIFIED";

    public static final String DEV_ROLE = "ROLE_DEV";

    public static final SimpleGrantedAuthority UNVERIFIED_AUTHORITY = new SimpleGrantedAuthority(UNVERIFIED_ROLE);

    public static final SimpleGrantedAuthority VERIFIED_AUTHORITY = new SimpleGrantedAuthority(VERIFIED_ROLE);

    public static final SimpleGrantedAuthority DEV_AUTHORITY = new SimpleGrantedAuthority(DEV_ROLE);
    //
    // roles for API access
    //
    public static final String API_UNVERIFIED_ROLE = "API_ROLE_UNVERIFIED";

    public static final String API_VERIFIED_ROLE = "API_ROLE_VERIFIED";

    public static final String API_DEV_ROLE = "API_ROLE_DEV";

    public static final SimpleGrantedAuthority API_UNVERIFIED_AUTHORITY = new SimpleGrantedAuthority(API_UNVERIFIED_ROLE);

    public static final SimpleGrantedAuthority API_VERIFIED_AUTHORITY = new SimpleGrantedAuthority(API_VERIFIED_ROLE);

    public static final SimpleGrantedAuthority API_DEV_AUTHORITY = new SimpleGrantedAuthority(API_DEV_ROLE);
}
