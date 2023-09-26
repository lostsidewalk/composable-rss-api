package com.lostsidewalk.buffy.app.model.v1.request;


import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A request model for configuring a new post URL.
 */
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
}
