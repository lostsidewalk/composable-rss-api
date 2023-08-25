package com.lostsidewalk.buffy.app.model.response;

import jakarta.validation.constraints.Size;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginResponse {

    @NotBlank(message = "{login.error.token-is-blank}")
    private String authToken;

    @NotBlank(message = "{login.error.username-is-blank}")
    @Size(max = 100, message = "{login.error.username-too-long}")
    private String username;

    @NotBlank(message = "{login.error.has-subscription-is-blank}")
    private boolean hasSubscription;

    private LoginResponse(String authToken, String username, boolean hasSubscription) {
        this.authToken = authToken;
        this.username = username;
        this.hasSubscription = hasSubscription;
    }

    public static LoginResponse from(String authToken, String username, boolean hasSubscription) {
        return new LoginResponse(authToken, username, hasSubscription);
    }
}
