package com.lostsidewalk.buffy.app.model.v1.request;


import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A request model for configuring a new post URL.
 */
@Slf4j
@Data
@NoArgsConstructor
public class PostUrlConfigRequest {

    /**
     * The title of the URL.
     */
    String title;

    /**
     * The type of the URL.
     */
    String type;

    /**
     * The actual URL (href) value.
     */
    String href;

    /**
     * The language of the content at the URL.
     */
    String hreflang;

    /**
     * The relationship of the URL to the current document.
     */
    String rel;

    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
