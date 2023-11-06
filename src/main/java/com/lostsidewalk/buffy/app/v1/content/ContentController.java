package com.lostsidewalk.buffy.app.v1.content;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.ContentObjectConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.v1.BasePostEntityController;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.StagingPost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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

import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller class for managing content-related operations.
 * <p>
 * This controller provides endpoints for managing content objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
class ContentController extends BasePostEntityController<ContentObject> {

    @SuppressWarnings("MethodReturnAlwaysConstant")
    @Override
    protected final String getEntityContext() {
        return "content";
    }

    /**
     * Add new content to the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new content
     * to a specific post given by its Id.
     *
     * @param postId                   The Id of the post into which the new content should be added.
     * @param postContentConfigRequest A ContentObjectConfigRequest, representing new content to add.
     * @param authentication           The authentication details of the user making the request.
     * @return a ResponseEntity containing the created content
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add new content to the post given by its Id")
    @ApiResponse(responseCode = "201", description = "Successfully add content to post")
    @PostMapping(value = "/${api.version}/posts/{postId}/content", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostConfigResponse> addPostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add content to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A content object", required = true,
                    schema = @Schema(implementation = ContentObject.class))
            @Valid ContentObjectConfigRequest postContentConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostContent adding content for for user={}, postId={}", username, postId);
        StopWatch createTimer = createStarted();
        String entityIdent = getStagingPostService().addContent(username, postId, postContentConfigRequest);
        createTimer.stop();
        return finalizeAddEntity(username, createTimer, postId, entityIdent);
    }

    //
    // RETRIEVE POST CONTENT
    //

    /**
     * Get all content in the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * content in a specific post given by its Id.
     *
     * @param postId         The Id of the post to fetch content from.
     * @param offset         The number of items to skip before returning results.
     * @param limit          The maximum number of items to return.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post content.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all content in the post given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post content",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ContentObject.class))))
    @GetMapping(value = "/${api.version}/posts/{postId}/content", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<ContentObject>> getPostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch content from", required = true)
            Long postId,
            //
            @Parameter(name = "offset", description = "The number of items to skip before returning results")
            @Valid @RequestParam(name = "offset", required = false)
            @Positive
            Integer offset,
            //
            @Parameter(name = "limit", description = "The maximum number of items to return")
            @Valid @RequestParam(name = "limit", required = false)
            @Positive
            Integer limit,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostContent for user={}, postId={}", username, postId);
        StopWatch retrieveTimer = createStarted();
        List<ContentObject> postContent = getStagingPostService().findById(username, postId).getPostContents();
        retrieveTimer.stop();
        return finalizeRetrieveEntities(username, retrieveTimer, postId, postContent, offset, limit);
    }

    /**
     * Get a single content in the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve a single
     * content in a specific post given by its Id.
     *
     * @param postId         The Id of the post to fetch contents from.
     * @param contentIdent   The identifier of the content to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post contents.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a single content in the post given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post content",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContentObject.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/content/{contentIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ContentObject> getPostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch the content from", required = true)
            Long postId,
            //
            @PathVariable("contentIdent")
            @Parameter(description = "The identifier of the content to fetch", required = true)
            String contentIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostContents for user={}, postId={}", username, postId);
        StopWatch retrieveTimer = createStarted();
        ContentObject postContent = getStagingPostService().findContentByIdent(username, postId, contentIdent);
        retrieveTimer.stop();
        return finalizeRetrieveEntity(username, retrieveTimer, postId, contentIdent, postContent);
    }

    //
    // UPDATE POST CONTENT
    //

    /**
     * Update all post content on a post a given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post content on a post given by its Id.
     *
     * @param postId                    The Id of the post to update.
     * @param postContentConfigRequests A list of ContentObjectConfigRequest objects representing the content of the post.
     * @param httpMethod                The HTTP method in use, either PATCH or PUT.
     * @param authentication            The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post contents on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post contents")
    @RequestMapping(value = "/${api.version}/posts/{postId}/content", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostContents(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @RequestBody @Valid List<? extends ContentObjectConfigRequest> postContentConfigRequests,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        //
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContents for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updateContents(username, postId, postContentConfigRequests, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntities(username, updateTimer, updatedPost, size(postContentConfigRequests));
    }

    /**
     * Update a post content object given by its identifier on a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post content object given by its identifier, on a post given by its Id.
     *
     * @param postId                   The Id of the post to update.
     * @param contentIdent             The identifier of the content object to update.
     * @param postContentConfigRequest A ContentObjectConfigRequest, representing new content to update.
     * @param httpMethod               The HTTP method in use, either PATCH or PUT.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post content given by Ident, on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post content")
    @RequestMapping(value = "/${api.version}/posts/{postId}/content/{contentIdent}", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("contentIdent")
            @Parameter(description = "The identifier of the content to update", required = true)
            String contentIdent,
            //
            @Valid @RequestBody ContentObjectConfigRequest postContentConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        //
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContent for user={}, postId={}, contentIdent={}, httpMethod={}", username, postId, contentIdent, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updateContent(username, postId, contentIdent, postContentConfigRequest, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedPost, contentIdent);
    }

    //
    // DELETE POST CONTENT
    //

    /**
     * Delete all post contents on a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * post content on a post given by its Id.
     *
     * @param postId         The Id of the post from which the content will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all post contents on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post contents")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/content", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostContents(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete content from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContents for user={}, postId={}", username, postId);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deletePostContents(username, postId);
        deleteTimer.stop();
        return finalizeDeleteEntities(username, deleteTimer, updatedPost);
    }

    /**
     * Delete a post content object given by its identifier on a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * post content object given by its identifier, on a post given by its Id.
     *
     * @param postId         The Id of the post from which the content object will be removed.
     * @param contentIdent   The identifier of the content object to delete.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post content given by Ident, on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post content")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/content/{contentIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete content from", required = true)
            Long postId,
            //
            @PathVariable("contentIdent")
            @Parameter(description = "The identifier of the content to delete", required = true)
            String contentIdent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContent for user={}, postId={}, contentIdent={}", username, postId, contentIdent);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deletePostContent(username, postId, contentIdent);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, contentIdent, updatedPost);
    }
}
