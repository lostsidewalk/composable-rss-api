package com.lostsidewalk.buffy.app.model.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public final class RegistrationResponse {

    @NotBlank(message = "{registration.error.username-is-blank}")
    @Size(max = 100, message = "{registration.error.username-too-long}")
    String username;

    @NotBlank(message = "{registration.error.password-is-blank}")
    @Size(max = 256, message = "{registration.error.password-too-long}")
    String password;

    public RegistrationResponse(String username, String password) {
        setUsername(username);
        setPassword(password);
    }
}
