package com.lostsidewalk.buffy.app.model.v1.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotBlank;

/**
 * A request model for updating the authentication requirement of a queue.
 */
@Slf4j
@Data
public class QueueAuthUpdateRequest {

    /**
     * The new authentication requirement to be set for the queue.
     */
    @NotBlank(message = "{queue.auth.error.requirement-is-blank}")
    Boolean isRequired;

    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
