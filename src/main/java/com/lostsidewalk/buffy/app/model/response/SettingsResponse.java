package com.lostsidewalk.buffy.app.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.FrameworkConfig;
import com.lostsidewalk.buffy.auth.AuthProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import javax.validation.constraints.NotBlank;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

@Data
@JsonInclude(NON_ABSENT)
public class SettingsResponse {

    @NotBlank(message = "{settings.error.username-is-blank}")
    @Size(max = 100, message = "{settings.error.username-too-long}")
    String username;

    @NotBlank(message = "{settings.error.email-is-blank}")
    @Email(message = "{settings.error.email-is-invalid}")
    @Size(max = 512, message = "{settings.error.email-too-long}")
    String emailAddress;

    @NotBlank(message = "{settings.error.auth-provider-is-blank}")
    AuthProvider authProvider;

    @Size(max = 1024, message = "{settings.error.auth-provider-profile-img-url-too-long}")
    String authProviderProfileImgUrl;

    @Size(max = 256, message = "{settings.error.auth-provider-profile-username-too-long}")
    String authProviderUsername;

    @Valid
    FrameworkConfig frameworkConfig;

    @Size(max = 36, message = "{settings.error.api-key-too-long}")
    String apiKey;

    @Valid
    SubscriptionResponse subscription;

    private SettingsResponse(String username, String emailAddress, AuthProvider authProvider, String authProviderProfileImgUrl, String authProviderUsername, FrameworkConfig frameworkConfig, String apiKey) {
        this.username = username;
        this.emailAddress = emailAddress;
        this.frameworkConfig = frameworkConfig;
        this.authProvider = authProvider;
        this.authProviderUsername = authProviderUsername;
        this.authProviderProfileImgUrl = authProviderProfileImgUrl;
        this.apiKey = apiKey;
    }

    public static SettingsResponse from(String username, String emailAddress, AuthProvider authProvider, String authProviderProfileImgUrl, String authProviderUsername, FrameworkConfig frameworkConfig, String apiKey) {
        return new SettingsResponse(username, emailAddress, authProvider, authProviderProfileImgUrl, authProviderUsername, frameworkConfig, apiKey);
    }
}
