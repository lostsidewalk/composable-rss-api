package com.lostsidewalk.buffy.app.v1.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class PostController_Delete extends PostController {

    //
    // DELETE POST
    //

    /**
     * Delete a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * a post given by its Id.
     *
     * @param postId              The Id of the post to delete.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post")
    @DeleteMapping(value = "/${api.version}/posts/{postId}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePost(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePost for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostPubStatus(username, postId, DEPUB_PENDING);
        stagingPostService.deleteById(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostDelete(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Deleted post Id " + postId));
    }

    //
    // DELETE POST INDIVIDUAL FIELDS
    //

    /**
     * Delete the iTunes descriptor for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the iTunes descriptor from a post given by its Id.
     *
     * @param postId              The Id of the post to delete the iTunes descriptor from.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the iTunes descriptor from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post iTunes descriptor")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/itunes", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostITunes(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the iTunes descriptor to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostITunes for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostITunes(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, postId, "postITunes");
        return ok().body(buildResponseMessage("Deleted iTunes descriptor from post Id " + postId));
    }

    /**
     * Delete the comment string for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the comment string from a post given by its Id.
     *
     * @param postId              The Id of the post to delete the comment string from.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the comment string from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post comment string")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/comment", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostComment(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the comment string to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostComment for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostComment(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, postId, "postComment");
        return ok().body(buildResponseMessage("Deleted comment string from post Id " + postId));
    }

    /**
     * Delete the rights string for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the rights string from a post given by its Id.
     *
     * @param postId              The Id of the post to delete the rights string from.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the rights string from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post rights string")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/rights", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostRights(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the rights string to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostRights for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostRights(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, postId, "postRights");
        return ok().body(buildResponseMessage("Deleted rights string from post Id " + postId));
    }

    /**
     * Delete the categories for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the categories from a post given by its Id.
     *
     * @param postId              The Id of the post to delete the categories from.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the categories from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post categories")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/categories", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostCategories(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the rights string to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostCategories for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostCategories(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, postId, "categories");
        return ok().body(buildResponseMessage("Deleted categories from post Id " + postId));
    }

    /**
     * Delete the expiration timestamp for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the expiration timestamp from a post given by its Id.
     *
     * @param postId              The Id of the post to delete the expiration timestamp from.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the expiration timestamp from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post expiration timestamp")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/expiration", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteExpirationTimestamp(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the expiration timestamp to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteExpirationTimestamp for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearExpirationTimestamp(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, postId, "expirationTimestamp");
        return ok().body(buildResponseMessage("Deleted expiration timestamp string from post Id " + postId));
    }
}
