package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import jakarta.validation.constraints.Size;

/**
 * A request model for updating the status of a queue.
 */
@Data
public class QueueStatusUpdateRequest {

    /**
     * The new status to be set for the queue.
     */
    @NotNull(message = "{queue.status.update.error.new-status-is-blank}")
    @Size(max = 64, message = "{queue.status.update.error.new-status-too-long}")
    String newStatus;
}
