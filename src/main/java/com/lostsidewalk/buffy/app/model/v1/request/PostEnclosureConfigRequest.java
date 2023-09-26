package com.lostsidewalk.buffy.app.model.v1.request;


import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A request model for configuring a new post enclosure.
 */
@Data
@NoArgsConstructor
public class PostEnclosureConfigRequest {

    /**
     * The URL of the enclosure.
     */
    String url;

    /**
     * The type of the enclosure.
     */
    String type;

    /**
     * The length of the enclosure in bytes.
     */
    Long length;
}
