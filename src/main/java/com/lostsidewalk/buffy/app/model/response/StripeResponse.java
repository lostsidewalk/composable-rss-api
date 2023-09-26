package com.lostsidewalk.buffy.app.model.response;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class StripeResponse {

    @NotBlank(message = "{stripe.response.error.session-id-is-blank}")
    private String sessionId;

    @NotBlank(message = "{stripe.response.error.session-url-is-blank}")
    private String sessionUrl;

    StripeResponse(String sessionId, String sessionUrl) {
        this.sessionId = sessionId;
        this.sessionUrl = sessionUrl;
    }

    public static StripeResponse from(String sessionId, String sessionUrl) {
        return new StripeResponse(sessionId, sessionUrl);
    }
}
