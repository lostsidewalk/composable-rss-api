package com.lostsidewalk.buffy.app.model.response;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotBlank;


@Slf4j
@Data
public class LoginResponse {

    @NotBlank(message = "{login.error.token-is-blank}")
    private String authToken;

    @NotBlank(message = "{login.error.username-is-blank}")
    @Size(max = 100, message = "{login.error.username-too-long}")
    private String username;

    private LoginResponse(String authToken, String username) {
        this.authToken = authToken;
        this.username = username;
    }

    public static LoginResponse from(String authToken, String username) {
        return new LoginResponse(authToken, username);
    }
}
