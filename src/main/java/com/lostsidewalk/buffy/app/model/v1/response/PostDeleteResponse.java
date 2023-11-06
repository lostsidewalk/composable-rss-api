package com.lostsidewalk.buffy.app.model.v1.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.publisher.Publisher;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * A response model for post delete operations.
 */
@Slf4j
@Data
@JsonInclude(NON_EMPTY)
public class PostDeleteResponse {

    /**
     *
     */
    @NotBlank(message = "{post.delete.response.error.message-is-blank}")
    String message;

    /**
     *
     */
    @NotNull(message = "{post.delete.response.error.deploy-responses-is-null")
    Map<String, DeployResponse> deployResponses;

    private PostDeleteResponse(String message, Map<String, DeployResponse> deployResponses) {
        this.message = message;
        this.deployResponses = deployResponses;
    }

    public static PostDeleteResponse from(String message, Map<String, Publisher.PubResult> pubResults) {
        if (pubResults != null) {
            return new PostDeleteResponse(message, DeployResponse.from(pubResults));
        } else {
            return new PostDeleteResponse(message, null);
        }
    }
}
