package com.lostsidewalk.buffy.app.model.v1.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * A response model for post configuration operations.
 */
@Slf4j
@Data
@JsonInclude(NON_EMPTY)
public class PostConfigResponse {

    /**
     * The post being configured
     */
    @NotNull(message = "{post.config.response.error.current-value-is-null}")
    PostDTO postDTO;

    /**
     * True if the updated post was deployed.
     */
    boolean deployed;

    /**
     * The results of deployment on all publishers.
     */
    @NotNull(message = "{post.config.response.error.deploy-responses-is-null")
    Map<String, DeployResponse> deployResponses;

    private PostConfigResponse(PostDTO postDTO, Map<String, DeployResponse> deployResponses) {
        this.postDTO = postDTO;
        deployed = (deployResponses != null);
        this.deployResponses = deployResponses;
    }

    /**
     * Static factory method to create a PostConfigResponse data transfer object from the supplied parameters.
     *
     * @param postDTO         the Post itself
     * @param pubResults      optional mapping of publisher identifier to deployment responses for the given queue
     * @return a PostConfigResponse entity encapsulating this information
     */
    public static PostConfigResponse from(PostDTO postDTO, Map<String, PubResult> pubResults) {
        if (pubResults != null) {
            return new PostConfigResponse(postDTO, DeployResponse.from(pubResults));
        } else {
            return new PostConfigResponse(postDTO, null);
        }
    }
}
