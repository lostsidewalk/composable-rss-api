package com.lostsidewalk.buffy.app.model.v1.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.publisher.Publisher;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * A response model for post create operations.
 */
@Slf4j
@Data
@JsonInclude(NON_EMPTY)
public class PostCreateResponse {

    /**
     *
     */
    @NotNull(message = "{post.create.response.error.posts-is-null}")
    List<Long> postIds;

    /**
     *
     */
    boolean deployed;

    /**
     *
     */
    @NotNull(message = "{post.create.response.error.deploy-responses-is-null")
    Map<String, ? extends DeployResponse> deployResponses;

    private PostCreateResponse(List<Long> postIds, Map<String, ? extends DeployResponse> deployResponses) {
        this.postIds = postIds;
        this.deployResponses = deployResponses;
    }

    public static PostCreateResponse from(List<Long> postIds, Map<String, Publisher.PubResult> pubResults) {
        Map<String, ? extends DeployResponse> deployResponses = null;
        if (pubResults != null) {
            deployResponses = DeployResponse.from(pubResults);
        }
        return new PostCreateResponse(new ArrayList<>(postIds), deployResponses == null ? null : new HashMap<>(deployResponses));
    }
}
