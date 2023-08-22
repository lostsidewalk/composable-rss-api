package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.post.PostMedia;
import com.lostsidewalk.buffy.post.StagingPost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for managing media-related operations.
 *
 * This controller provides endpoints for managing media objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class PostMediaController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    //
    // RETRIEVE POST MEDIA
    //

    /**
     * Get all media in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * medias in a specific post identified by its Id.
     *
     * @param postId          The Id of the post to fetch medias from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post medias.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all medias in the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post medias",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostMedia.class)))
    @GetMapping("/posts/{postId}/media")
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostMedia> getPostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch medias from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostMedias for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        PostMedia postMedia = stagingPost.getPostMedia();
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, postId, "postMedia");
        return ok(postMedia);
    }

    //
    // UPDATE POST MEDIA
    //

    /**
     * Update a post media given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post media object, on a post identified by its Id.
     *
     * @param postId               The Id of the post to update.
     * @param postMedia            A PostMedia object, representing new media to update.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post media object on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post media")
    @PutMapping("/posts/{postId}/media")
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody PostMedia postMedia,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostMedia for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostMedia(username, postId, postMedia);
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postMedia");
        return ok().body(buildResponseMessage("Updated media on post Id " + postId));
    }

    //
    // DELETE POST MEDIA
    //

    /**
     * Delete a post media object from a post given by Id,
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a post media object on a post identified by its Id.
     *
     * @param postId               The Id of the post whence the media will be removed.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post media object on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post media")
    @DeleteMapping("/posts/{postId}/media")
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostMedia for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostMedia(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, postId, "postMedia");
        return ok().body(buildResponseMessage("Deleted media from post Id " + postId));
    }
}
