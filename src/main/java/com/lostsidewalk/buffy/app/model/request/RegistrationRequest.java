package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

    @NotBlank(message = "{registration.error.username-is-blank}")
    @Size(max = 100, message = "{registration.error.username-too-long}")
    String username;

    @NotBlank(message = "{registration.error.email-is-blank}")
    @Email(message = "{registration.error.email-is-invalid}")
    @Size(max = 512, message = "{registration.error.email-too-long}")
    String email;

    @NotBlank(message = "{registration.error.password-is-blank}")
    @Size(min = 6, max = 256, message = "{registration.error.password-length}")
    String password;
}
