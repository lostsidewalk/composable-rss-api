package com.lostsidewalk.buffy.app.model.v1.request;

import lombok.extern.slf4j.Slf4j;

/**
 * A request model for updating the status of a queue.
 */
@Slf4j
public enum QueueStatusUpdateRequest {
    DEPLOY_PENDING,
    PUB_ALL,
    DEPUB_ALL,
}
