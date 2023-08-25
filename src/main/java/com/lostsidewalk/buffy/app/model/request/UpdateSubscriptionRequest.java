package com.lostsidewalk.buffy.app.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSubscriptionRequest {

    @NotNull(message = "{update.subscription.error.status-is-null}")
    SubscriptionStatus subscriptionStatus;
}
