package com.lostsidewalk.buffy.app.model.v1.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A request model for configuring a queue credential.
 */
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
}
