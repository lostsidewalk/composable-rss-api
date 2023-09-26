package com.lostsidewalk.buffy.app.v1.queue;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
import com.lostsidewalk.buffy.app.model.v1.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;

/**
 * Controller class for creating queues and posts in the application.
 */
@Slf4j
@RestController
@Validated
public class QueueController_Create extends QueueController {

    //
    // CREATE QUEUES
    //

    /**
     * Create a new feed queue with the provided configuration.
     * <p>
     * This endpoint is used to create a new feed queue with the provided configuration.
     * A feed queue is a structure used to manage, organize, and publish syndicated content in various formats
     * such as RSS 2.0, ATOM, and JSON.
     *
     * @param queueConfigRequest A queue configuration request.
     * @param authentication     The authenticated user's details.
     * @return A ResponseEntity containing a QueueConfigResponse with creation and deployment details about the created queue.
     * @throws DataAccessException   If there's an issue accessing data.
     * @throws DataUpdateException   If there's an issue updating data.
     * @throws DataConflictException If there is a duplicate key.
     */
    @Operation(summary = "Create a new feed queue")
    @ApiResponse(responseCode = "201", description = "Successfully created queue",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QueueConfigResponse.class)))
    @PostMapping(value = "/${api.version}/queues", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueConfigResponse> createQueue(
            @RequestBody
            @Parameter(description = "A queue configuration request", required = true,
                    schema = @Schema(implementation = QueueConfigRequest.class))
            @Valid QueueConfigRequest queueConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createQueue adding queue for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        // create the queue
        Long queueId = queueDefinitionService.createQueue(username, queueConfigRequest);
        // re-fetch this queue definition
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        String rss20Url = deployResponses.get("RSS_20").getUrl();
        URI createdLocation = URI.create(rss20Url);
        stopWatch.stop();
        appLogService.logQueueCreate(username, stopWatch, pubResults);
        return created(createdLocation).body(queueConfigResponse);
    }

    //
    // CREATE POST
    //

    /**
     * Create new posts in the specified queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to create new posts
     * in a queue given by its Id.
     *
     * @param queueIdent            The identifier of the queue into which the new posts should be added.
     * @param postConfigRequests    A list of PostConfigRequests, representing new posts to add.
     * @param authentication        The authentication details of the user making the request.
     * @return a ResponseEntity containing the created posts
     * @throws DataAccessException   If there's an issue accessing data.
     * @throws DataUpdateException   If there's an issue updating data.
     * @throws DataConflictException If there is a duplicate key.
     */
    @Operation(summary = "Create new posts in the specified queue")
    @ApiResponse(responseCode = "201", description = "Successfully created posts",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostDTO.class))))
    @PostMapping(value = "/${api.version}/queues/{queueIdent}/posts", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostDTO>> createPosts(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to create posts in", required = true)
            String queueIdent,
            //
            @RequestBody
            @Parameter(description = "List of post configuration requests", required = true,
                    schema = @Schema(implementation = PostConfigRequest.class))
            List<@Valid PostConfigRequest> postConfigRequests,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createPosts adding {} posts for for user={}, queueIdent={}", size(postConfigRequests), username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        List<PostDTO> createdPosts = new ArrayList<>();
        for (PostConfigRequest postConfigRequest : postConfigRequests) {
            Long postId = stagingPostService.createPost(username, queueId, postConfigRequest);
            StagingPost stagingPost = stagingPostService.findById(username, postId);
            createdPosts.add(preparePostDTO(stagingPost, queueIdent));
        }
        validator.validate(createdPosts);
        URI createdLocation = URI.create("/posts/" + queueId);
        stopWatch.stop();
        appLogService.logStagingPostCreate(username, stopWatch, postConfigRequests.size(), size(createdPosts));
        return created(createdLocation).body(createdPosts);
    }
}
