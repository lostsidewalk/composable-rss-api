package com.lostsidewalk.buffy.app.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SerializationUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Optional;


@Slf4j
public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                String cookieName = cookie.getName();
                if (cookieName.equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }

        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                String cookieName = cookie.getName();
                if (cookieName.equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    public static String serialize(Object object) {
        Encoder urlEncoder = Base64.getUrlEncoder();
        byte[] serialize = SerializationUtils.serialize(object);
        return urlEncoder
                .encodeToString(serialize);
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        Base64.Decoder urlDecoder = Base64.getUrlDecoder();
        String value = cookie.getValue();
        byte[] decodedBytes = urlDecoder.decode(value);

        try (ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes);
             ObjectInput ois = new ObjectInputStream(bis)) {
            Object deserializedObject = ois.readObject();
            if (cls.isInstance(deserializedObject)) {
                return cls.cast(deserializedObject);
            } else {
                //noinspection ThrowCaughtLocally
                throw new IllegalArgumentException("Deserialized object is not of the specified class.");
            }
        } catch (IOException | ClassNotFoundException | IllegalArgumentException e) {
            log.debug("Unable to deserialize object due to: {}", e.getMessage());
        }

        return null;
    }
}
