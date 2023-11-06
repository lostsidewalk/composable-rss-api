package com.lostsidewalk.buffy.app.user;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public final String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public final String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public final String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public final String getImageUrl() {
        return (String) attributes.get("picture");
    }
}