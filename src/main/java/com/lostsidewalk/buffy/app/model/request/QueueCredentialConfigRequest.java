package com.lostsidewalk.buffy.app.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * A request model for configuring a queue credential.
 */
@Data
@NoArgsConstructor
@JsonInclude(NON_ABSENT)
public class QueueCredentialConfigRequest {

    @NotBlank(message = "{queue.credential.error.username-is-blank}")
    @Size(max = 100, message = "{queue.credential.error.username-too-long}")
    String basicUsername;

    @NotBlank(message = "{queue.credential.error.password-is-blank}")
    @Size(max = 256, message = "{queue.credential.error.password-too-long}")
    String basicPassword;
}
