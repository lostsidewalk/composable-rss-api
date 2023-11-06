package com.lostsidewalk.buffy.app.model.v1.request;


import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A request model for configuring a new post enclosure.
 */
@Slf4j
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


    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
