package com.lostsidewalk.buffy.app.model.v1.request;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A request model for configuring a queue credential.
 */
@Slf4j
@Data
@NoArgsConstructor
public class QueueCredentialConfigRequest {

    /**
     * The username associated with this credential.
     */
    @NotBlank(message = "{queue.credential.error.username-is-blank}")
    @Size(max = 100, message = "{queue.credential.error.username-too-long}")
    String basicUsername;

    /**
     * The password associated with this credential.
     */
    @NotBlank(message = "{queue.credential.error.password-is-blank}")
    @Size(max = 256, message = "{queue.credential.error.password-too-long}")
    String basicPassword;

    @JsonAnySetter
    public static void handleUnrecognizedField(String key, Object value) {
        throw new IllegalArgumentException("Unrecognized field: " + key);
    }
}
