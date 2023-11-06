package com.lostsidewalk.buffy.app.model.v1.request;


import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A request model for configuring a new person (author/contributor).
 */
@Slf4j
@Data
@NoArgsConstructor
public class PostPersonConfigRequest {

    /**
     * The name of the person.
     */
    String name;

    /**
     * The email address of the person.
     */
    String email;

    /**
     * The URI of the person.
     */
    String uri;


    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
