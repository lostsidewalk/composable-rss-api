package com.lostsidewalk.buffy.app.model.v1.request;


import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A request model for configuring a new person (author/contributor).
 */
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
}
