package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.StagingPost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for managing content-related operations.
 *
 * This controller provides endpoints for managing content objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class PostContentController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    //
    // CREATE POST CONTENT
    //

    /**
     * Add new content to the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new content
     * to a specific post identified by its Id.
     *
     * @param postId                 The Id of the post into which the new content should be added.
     * @param postContent            A ContentObject, representing new content to add.
     * @param authentication         The authentication details of the user making the request.
     * @return a ResponseEntity containing the created content
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add new content to the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully add content to post")
    @PostMapping(value = "/posts/{postId}/content", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add content to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A content object", required = true,
                    schema = @Schema(implementation = ContentObject.class))
            @Valid ContentObject postContent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostContent adding content for for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.addContent(username, postId, postContent);
        stopWatch.stop();
        appLogService.logStagingPostAddContent(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Added content to post Id " + postId));
    }

    //
    // RETRIEVE POST CONTENT
    //

    /**
     * Get all content in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * content in a specific post identified by its Id.
     *
     * @param postId          The Id of the post to fetch content from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post content.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all content in the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post content",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ContentObject.class))))
    @GetMapping(value = "/posts/{postId}/content", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<ContentObject>> getPostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch content from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostContent for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<ContentObject> postContent = stagingPost.getPostContents();
        stopWatch.stop();
        appLogService.logStagingPostContentsFetch(username, stopWatch, postId, size(postContent));
        return ok(postContent);
    }

    //
    // UPDATE POST CONTENT
    //

    /**
     * Update a post content object given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post content object identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post to update.
     * @param contentIdx           The index of the content object to update.
     * @param postContent          A ContentObject, representing new content to update.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post content given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post content")
    @PutMapping(value = "/posts/{postId}/content/{contentIdx}", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("contentIdx")
            @Parameter(description = "The index of the content to update", required = true)
            Integer contentIdx,
            //
            @Valid @RequestBody ContentObject postContent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContent for user={}, postId={}, contentIdx={}", username, postId, contentIdx);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostContent(username, postId, contentIdx, postContent);
        stopWatch.stop();
        appLogService.logStagingPostContentUpdate(username, stopWatch, postId, contentIdx);
        return ok().body(buildResponseMessage("Updated content on post Id " + postId));
    }

    //
    // DELETE POST CONTENT
    //

    /**
     * Delete a post content object given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a post content object identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post whence the content object will be removed.
     * @param contentIdx           The index of the content object to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post content given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post content")
    @DeleteMapping(value = "/posts/{postId}/content/{contentIdx}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            //
            @PathVariable("contentIdx")
            @Parameter(description = "The index of the content to delete", required = true)
            Integer contentIdx,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContent for user={}, postId={}, contentIdx={}", username, postId, contentIdx);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostContent(username, postId, contentIdx);
        stopWatch.stop();
        appLogService.logStagingPostContentDelete(username, stopWatch, postId, contentIdx);
        return ok().body(buildResponseMessage("Deleted content from post Id " + postId));
    }
}
