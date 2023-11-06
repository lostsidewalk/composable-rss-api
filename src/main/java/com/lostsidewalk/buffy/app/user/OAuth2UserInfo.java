package com.lostsidewalk.buffy.app.user;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

@Slf4j
abstract class OAuth2UserInfo {
    final Map<String, Object> attributes;

    OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    abstract String getId();

    abstract String getName();

    abstract String getEmail();

    abstract String getImageUrl();

    @Override
    public final String toString() {
        return "OAuth2UserInfo{" +
                "attributes=" + attributes +
                '}';
    }
}
