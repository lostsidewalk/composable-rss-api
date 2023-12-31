package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Data
@AllArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "{password.reset.error.username-is-blank}")
    @Size(max = 100, message = "{password.reset.error.username-too-long}")
    private String username;

    @NotBlank(message = "{password.reset.error.email-is-blank}")
    @Email(message = "{password.reset.error.email-is-invalid}")
    @Size(max = 512, message = "{password.reset.error.email-too-long}")
    private String email;
}
