package com.lostsidewalk.buffy.app.model.v1.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * A request model for updating the status of a post.
 */
@Data
public class PostStatusUpdateRequest {

    /**
     * The new status to be set for the post.
     */
    @Size(max = 64, message = "{post.status.update.error.new-status-too-long}")
    String newStatus; // may be null
}
