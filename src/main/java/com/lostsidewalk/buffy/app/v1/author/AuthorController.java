package com.lostsidewalk.buffy.app.v1.author;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostPersonConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.v1.BasePostEntityController;
import com.lostsidewalk.buffy.post.PostPerson;
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
 * Base controller class for managing author-related operations.
 * <p>
 * This controller provides endpoints for managing author objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
class AuthorController extends BasePostEntityController<PostPerson> {

    @SuppressWarnings("MethodReturnAlwaysConstant")
    @Override
    protected final String getEntityContext() {
        return "authors";
    }

    /**
     * Add a new author to a specific post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add a new author
     * to a specific post given by its Id.
     *
     * @param postId                  The Id of the post to which the new author should be added.
     * @param postAuthorConfigRequest A PostPersonConfigRequest object representing the new author to add.
     * @param authentication          The authentication details of the user making the request.
     * @return a ResponseEntity containing the created author
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add a new author to a post by its Id")
    @ApiResponse(responseCode = "201", description = "Successfully added the author to the post")
    @PostMapping(value = "/${api.version}/posts/{postId}/authors", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostConfigResponse> addPostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add the author to", required = true)
            Long postId,
            @RequestBody
            @Parameter(description = "An author object", required = true,
                    schema = @Schema(implementation = PostPersonConfigRequest.class))
            @Valid PostPersonConfigRequest postAuthorConfigRequest,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostAuthor adding author for user={}, postId={}", username, postId);
        StopWatch createTimer = createStarted();
        String authorIdent = getStagingPostService().addAuthor(username, postId, postAuthorConfigRequest);
        createTimer.stop();
        return finalizeAddEntity(username, createTimer, postId, authorIdent);
    }

    /**
     * Get all authors in a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * authors in a specific post given by its Id.
     *
     * @param postId         The Id of the post to fetch authors from.
     * @param offset         The number of items to skip before returning results.
     * @param limit          The maximum number of items to return.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post authors.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all authors in a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post authors",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostPerson.class))))
    @GetMapping(value = "/${api.version}/posts/{postId}/authors", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostPerson>> getPostAuthors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch authors from", required = true)
            Long postId,
            @Parameter(name = "The number of items to skip before returning results")
            @Valid @RequestParam(name = "offset", required = false)
            @Positive
            Integer offset,
            @Parameter(name = "The maximum number of items to return")
            @Valid @RequestParam(name = "limit", required = false)
            @Positive
            Integer limit,
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostAuthors for user={}, postId={}", username, postId);
        StopWatch retrieveTimer = createStarted();
        List<PostPerson> authors = getStagingPostService().findById(username, postId).getAuthors();
        retrieveTimer.stop();
        return finalizeRetrieveEntities(username, retrieveTimer, postId, authors, offset, limit);
    }

    /**
     * Get a single author given by its identifier on a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve a single
     * author given by its identifier on a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the author from.
     * @param authorIdent    The identifier of the author to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post author.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a single author in a post by its Id and author ident")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post author",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostPerson.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/authors/{authorIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostPerson> getPostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch the author from", required = true)
            Long postId,
            @PathVariable("authorIdent")
            @Parameter(description = "The identifier of the author to fetch", required = true)
            String authorIdent,
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostAuthors for user={}, postId={}", username, postId);
        StopWatch retrieveTimer = createStarted();
        PostPerson postAuthor = getStagingPostService().findAuthorByIdent(username, postId, authorIdent);
        retrieveTimer.stop();
        return finalizeRetrieveEntity(username, retrieveTimer, postId, authorIdent, postAuthor);
    }

    /**
     * Update all post authors on a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post authors on a post given by its Id.
     *
     * @param postId                   The Id of the post to update.
     * @param postAuthorConfigRequests A list of PostPersonConfigRequest objects representing the authors of the post.
     * @param httpMethod               The HTTP method in use, either PATCH or PUT.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update all post authors on a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post authors")
    @RequestMapping(value = "/${api.version}/posts/{postId}/authors", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostAuthors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            @RequestBody @Valid List<? extends PostPersonConfigRequest> postAuthorConfigRequests,
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostAuthors for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updateAuthors(username, postId, postAuthorConfigRequests, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntities(username, updateTimer, updatedPost, size(postAuthorConfigRequests));
    }

    /**
     * Update a post author given by its identifier on a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post author given by its identifier, on a post given by its Id.
     *
     * @param postId                  The Id of the post to update.
     * @param authorIdent             The identifier of the author to update.
     * @param postAuthorConfigRequest A PostPersonConfigRequest object representing the author to update.
     * @param httpMethod              The HTTP method in use, either PATCH or PUT.
     * @param authentication          The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update a post author by identifier on a post by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post author")
    @RequestMapping(value = "/${api.version}/posts/{postId}/authors/{authorIdent}", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            @PathVariable("authorIdent")
            @Parameter(description = "The identifier of the author to update", required = true)
            String authorIdent,
            @Valid @RequestBody PostPersonConfigRequest postAuthorConfigRequest,
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostAuthor for user={}, postId={}, authorIdent={}, httpMethod={}", username, postId, authorIdent, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updateAuthor(username, postId, authorIdent, postAuthorConfigRequest, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedPost, authorIdent);
    }

    /**
     * Delete all authors from a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * authors from a post given by its Id.
     *
     * @param postId         The Id of the post from which authors will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all authors from a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post authors")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/authors", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostAuthors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete authors from", required = true)
            Long postId,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostAuthors for user={}, postId={}", username, postId);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deleteAuthors(username, postId);
        deleteTimer.stop();
        return finalizeDeleteEntities(username, deleteTimer, updatedPost);
    }

    /**
     * Delete an author from a post given its Id and author identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete an
     * author from a post given by its Id and the author's identifier.
     *
     * @param postId         The Id of the post from which the author will be removed.
     * @param authorIdent    The identifier of the author to delete.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete an author from a post by Id and author ident")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post author")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/authors/{authorIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete the author from", required = true)
            Long postId,
            //
            @PathVariable("authorIdent")
            @Parameter(description = "The identifier of the author to delete", required = true)
            String authorIdent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostAuthor for user={}, postId={}, authorIdent={}", username, postId, authorIdent);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deleteAuthor(username, postId, authorIdent);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, authorIdent, updatedPost);
    }
}
