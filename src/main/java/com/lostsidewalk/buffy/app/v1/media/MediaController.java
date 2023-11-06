package com.lostsidewalk.buffy.app.v1.media;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.v1.BasePostController;
import com.lostsidewalk.buffy.post.PostMedia;
import com.lostsidewalk.buffy.post.StagingPost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller class for managing media-related operations.
 * <p>
 * This controller provides endpoints for managing media objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
class MediaController extends BasePostController {

    //
    // RETRIEVE POST MEDIA
    //

    /**
     * Get the media in the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * media in a post given by its Id.
     *
     * @param postId          The Id of the post to fetch media from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post media.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the media in the post given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post media",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostMedia.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/media", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostMedia> getPostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch media from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostMedia for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        PostMedia postMedia = stagingPost.getPostMedia();
        if (postMedia != null) {
            getValidator().validate(postMedia);
        }
        stopWatch.stop();
        AppLogService.logStagingPostAttributeFetch(username, stopWatch, postId, "postMedia");
        return ok(postMedia);
    }

    //
    // UPDATE POST MEDIA
    //

    /**
     * Update a post media object on a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post media object on a post given by its Id.
     *
     * @param postId              The Id of the post to update.
     * @param postMedia           A PostMedia object, representing new media to update.
     * @param httpMethod          The HTTP method in use, either PATCH or PUT.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post media object on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post media")
    @RequestMapping(value = "/${api.version}/posts/{postId}/media", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody PostMedia postMedia,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostMedia for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostMedia(username, postId, postMedia, isPatch(httpMethod));
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        AppLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postMedia");
        return ok().body(postConfigResponse);
    }

    //
    // DELETE POST MEDIA
    //

    /**
     * Delete a post media object from a post given by Id,
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a post media object on a post given by its Id.
     *
     * @param postId               The Id of the post from which the media will be removed.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a post media object from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post media")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/media", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete media from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostMedia for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        getStagingPostService().clearPostMedia(username, postId);
        StagingPost updatedPost = getStagingPostService().findById(username, postId);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        AppLogService.logStagingPostAttributeDelete(username, stopWatch, postId, "postMedia");
        return ok().body(postConfigResponse);
    }
}
