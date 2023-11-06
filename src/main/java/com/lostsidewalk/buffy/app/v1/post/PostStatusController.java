package com.lostsidewalk.buffy.app.v1.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.v1.BasePostController;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
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

import java.util.Map;

import static com.lostsidewalk.buffy.app.audit.AppLogService.logStagingPostAttributeFetch;
import static com.lostsidewalk.buffy.app.audit.AppLogService.logStagingPostPubStatusUpdate;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.PUB_PENDING;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;


/**
 * Controller class for managing post status-related operations.
 * <p>
 * This controller provides endpoints for managing post status. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
public class PostStatusController extends BasePostController {

    /**
     * Get the status of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the status of a
     * posts given by its Id.  The status of a post is given by the PostPubStatus enumeration.
     *
     * @param postId         The Id of the post to fetch the status from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the post status.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the status of a post given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post status",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/status", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPostStatus(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch status from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostStatus for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        String response;
        if (stagingPost.isPublished()) {
            response = "PUBLISHED"; // TODO: unit test
        } else if (stagingPost.getPostPubStatus() != null) {
            response = stagingPost.getPostPubStatus().name(); // TODO: unit test
        } else {
            response = "UNPUBLISHED";
        }
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "status");
        return ok(response);
    }

    /**
     * Update the publication status of the post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * publication status of a post given by its Id. The status can be 'PUB_PENDING',
     * 'DEPUB_PENDING', or null.
     *
     * @param postId                  The Id of the post to update.
     * @param postStatusUpdateRequest The request containing the updated post status.
     * @param httpMethod              The HTTP method in use, either PATCH or PUT.
     * @param authentication          The authentication details of the user making the request.
     * @return A ResponseEntity containing a list of DeployResponse objects indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the publication status of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post publication status",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostConfigResponse.class))))
    @RequestMapping(value = "/${api.version}/posts/{postId}/status", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostStatus(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated post status", required = true)
            PostStatusUpdateRequest postStatusUpdateRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}, httpMethod={}", username, postId, postStatusUpdateRequest, httpMethod);
        StopWatch stopWatch = createStarted();
        PostPubStatus newStatus = PostPubStatus.valueOf(postStatusUpdateRequest.getNewStatus());
        StagingPost updatedPost = getStagingPostService().updatePostPubStatus(username, postId, newStatus);
        Map<String, PubResult> pubResults = updateStatusAndPublish(username, updatedPost, newStatus);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, pubResults);
        stopWatch.stop();
        logStagingPostPubStatusUpdate(username, stopWatch, postId, postStatusUpdateRequest, pubResults);
        return ok(postConfigResponse);
    }

    @SuppressWarnings("OverlyComplexMethod") // yes, yes it is
    private Map<String, PubResult> updateStatusAndPublish(String username, StagingPost updatedPost, PostPubStatus newStatus) throws DataAccessException, DataUpdateException {
        Map<String, PubResult> pubResults = null;
        boolean isAutoDeploy = getQueueDefinitionService().isAutoDeploy(username, updatedPost.getQueueId());
        boolean isPublished = updatedPost.isPublished();
        if (!isAutoDeploy && isPublished) {
            if (newStatus != DEPUB_PENDING) {
                throw new IllegalArgumentException("Invalid transition");
            }
        } else if (isAutoDeploy && isPublished) {
            if (newStatus == DEPUB_PENDING) {
                // immediately redeploy feed to remove depublished post
                pubResults = reDeploy(updatedPost);
            } else {
                throw new IllegalArgumentException("Invalid transition");
            }
        } else if (!isAutoDeploy && !isPublished) {
            if (newStatus == DEPUB_PENDING) {
                throw new IllegalArgumentException("Invalid transition");
            }
        } else if (isAutoDeploy && !isPublished) {
            if (newStatus == DEPUB_PENDING) {
                throw new IllegalArgumentException("Invalid transition");
            } else if (newStatus == PUB_PENDING) {
                // immediately redeploy feed w/updated post
                pubResults = reDeploy(updatedPost);
            }
        }
        return pubResults;
    }
}
