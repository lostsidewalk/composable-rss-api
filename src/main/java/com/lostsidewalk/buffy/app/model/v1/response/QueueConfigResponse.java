package com.lostsidewalk.buffy.app.model.v1.response;


import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * A response model for queue configuration operations.
 */
@Data
public class QueueConfigResponse {

    /**
     * The queue under configuration.
     */
    @NotNull(message = "{queue.config.error.queue-is-null}")
    QueueDTO queueDTO;

    /**
     * The results of deployment on all publishers.
     */
    @NotNull(message = "{queue.config.error.dpeloy-responses-is-null}")
    Map<String, DeployResponse> deployResponses;

    private QueueConfigResponse(QueueDTO queueDTO, Map<String, DeployResponse> deployResponses) {
        this.queueDTO = queueDTO;
        this.deployResponses = deployResponses;
    }

    /**
     * Static factory method to create a QueueConfig data transfer object from the supplied parameters.
     *
     * @param queueDTO        the Queue itself
     * @param deployResponses a mapping of publisher identifier to deployment responses for the given queue
     * @return a QueueConfigResponse entity encapsulating this information
     */
    public static QueueConfigResponse from(QueueDTO queueDTO, Map<String, DeployResponse> deployResponses) {
        return new QueueConfigResponse(queueDTO, deployResponses);
    }
}
