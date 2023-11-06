package com.lostsidewalk.buffy.app.model.v1.response;


import com.lostsidewalk.buffy.publisher.Publisher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * A response model for queue configuration operations.
 */
@Slf4j
@Data
public class QueueConfigResponse {

    /**
     * The queue being configured..
     */
    @NotNull(message = "{queue.config.error.queue-is-null}")
    QueueDTO queueDTO;

    /**
     * The results of deployment on all publishers.
     */
    @NotNull(message = "{queue.config.error.deploy-responses-is-null}")
    Map<String, DeployResponse> deployResponses;

    private QueueConfigResponse(QueueDTO queueDTO, Map<String, DeployResponse> deployResponses) {
        this.queueDTO = queueDTO;
        this.deployResponses = deployResponses;
    }

    /**
     * Static factory method to create a QueueConfigResponse data transfer object from the supplied parameters.
     *
     * @param queueDTO        the Queue itself
     * @param pubResults      optional mapping of publisher identifier to deployment responses for the given queue
     * @return a QueueConfigResponse entity encapsulating this information
     */
    public static QueueConfigResponse from(QueueDTO queueDTO, Map<String, Publisher.PubResult> pubResults) {
        return new QueueConfigResponse(queueDTO, DeployResponse.from(pubResults));
    }
}
