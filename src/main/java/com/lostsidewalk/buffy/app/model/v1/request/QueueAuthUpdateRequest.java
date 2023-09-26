package com.lostsidewalk.buffy.app.model.v1.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * A request model for updating the authentication requirement of a queue.
 */
@Data
public class QueueAuthUpdateRequest {

    /**
     * The new authentication requirement to be set for the queue.
     */
    @NotBlank(message = "{queue.auth.error.requirement-is-blank}")
    Boolean isRequired;
}
