package com.lostsidewalk.buffy.app.model.v1.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * A request model for updating the status of a post.
 */
@Slf4j
@Data
public class PostStatusUpdateRequest {

    /**
     * The new status to be set for the post.
     */
    @NotBlank
    @Size(max = 64, message = "{post.status.update.error.new-status-too-long}")
    String newStatus;

    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
