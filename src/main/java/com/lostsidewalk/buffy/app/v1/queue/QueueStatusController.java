package com.lostsidewalk.buffy.app.v1.queue;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.v1.request.QueueStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueStatusResponse;
import com.lostsidewalk.buffy.app.v1.BaseQueueController;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.publisher.Publisher;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.lostsidewalk.buffy.app.audit.AppLogService.logQueueStatusFetch;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.PUB_PENDING;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;


/**
 * Controller class for managing queue status-related operations.
 * <p>
 * This controller provides endpoints for managing status (i.e., deployments). Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
public class QueueStatusController extends BaseQueueController {

    /**
     * Get the status of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the status of a
     * queue given by its identifier.
     *
     * @param queueIdent       The identifier of the post to fetch the status from.
     * @param authentication   The authentication details of the user making the request.
     * @return A ResponseEntity containing the post status.
     * @throws DataAccessException If there's an issue accessing data.
     */
    // TODO: unit test
    @Operation(summary = "Get the status of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue status",
            content = @Content(schema = @Schema(implementation = QueueStatusResponse.class)))
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/status", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch status from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueStatus for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueStatusResponse queueStatus = getQueueDefinitionService().checkStatus(username, queueId);
        stopWatch.stop();
        logQueueStatusFetch(username, stopWatch, queueId, queueStatus);
        return ok(queueStatus);
    }

    /**
     * Update the deployment status of queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the deployment status
     * of an existing queue.
     *
     * @param queueIdent                    The identifier of the queue to fetch.
     * @param queueStatusUpdateRequest      The request containing the updated queue status.
     * @param httpMethod                    The HTTP method in use, either PATCH or PUT.
     * @param authentication                The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the image source update.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Change the deployment status of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue deployment status")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/status", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueConfigResponse> updateQueueStatus(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the deployment status for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue status", required = true)
            QueueStatusUpdateRequest queueStatusUpdateRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueStatus for user={}, queueIdent={}, queueStatusUpdateRequest={}, httpMethod={}",
                username, queueIdent, queueStatusUpdateRequest, httpMethod);
        StopWatch updateTimer = createStarted();
        List<StagingPost> stagingPosts = null;
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        if (queueStatusUpdateRequest == QueueStatusUpdateRequest.DEPLOY_PENDING) {
            // DEPLOY_PENDING: scoop up all staging posts that PUB_PENDING or DEPUB_PENDING, and publish those
            stagingPosts = getStagingPostService().getStagingPosts(username, singletonList(queueId), PUB_PENDING, DEPUB_PENDING);
        } else if (queueStatusUpdateRequest == QueueStatusUpdateRequest.PUB_ALL) {
            // PUB_ALL: update all posts to PUB_PENDING and deploy the entire queue
            List<Long> stagingPostIds = getStagingPostService().getStagingPosts(username, singletonList(queueId), PUB_PENDING)
                    .stream()
                    .map(StagingPost::getId)
                    .collect(toList());
            stagingPosts = getStagingPostService().updatePostPubStatus(username, stagingPostIds, PUB_PENDING);
        } else if (queueStatusUpdateRequest == QueueStatusUpdateRequest.DEPUB_ALL) {
            // DEPUB_ALL: update all posts to DEPUB_PENDING and deploy the entire queue
            List<Long> stagingPostIds = getStagingPostService().getStagingPosts(username, singletonList(queueId))
                    .stream()
                    .map(StagingPost::getId)
                    .collect(toList());
            stagingPosts = getStagingPostService().updatePostPubStatus(username, stagingPostIds, DEPUB_PENDING);
        }
        Map<String, Publisher.PubResult> pubResults = getPostPublisher().publishFeed(username, queueId, stagingPosts);
        QueueDefinition updatedQueue = getQueueDefinitionService().findByQueueId(username, queueId);
        QueueConfigResponse queueConfigResponse = prepareResponse(updatedQueue, pubResults);
        updateTimer.stop();
        AppLogService.logQueueStatusUpdate(username, updateTimer, queueId, queueStatusUpdateRequest);
        return ok(queueConfigResponse);
    }
}
