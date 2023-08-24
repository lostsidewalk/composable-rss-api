package com.lostsidewalk.buffy.app.model.request;

import lombok.Data;

/**
 * A request model for updating the authentication requirement of a queue.
 */
@Data
public class QueueAuthUpdateRequest {

    /**
     * The new authentication requirement to be set for the queue.
     */
    Boolean isRequired;
}
