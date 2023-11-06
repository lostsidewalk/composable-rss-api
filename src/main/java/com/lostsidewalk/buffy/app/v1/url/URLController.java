package com.lostsidewalk.buffy.app.v1.url;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostUrlConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.v1.BasePostEntityController;
import com.lostsidewalk.buffy.post.PostUrl;
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
 * Controller class for managing URL-related operations.
 * <p>
 * This controller provides endpoints for managing URLs within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
class URLController extends BasePostEntityController<PostUrl> {

    @SuppressWarnings("MethodReturnAlwaysConstant")
    @Override
    protected final String getEntityContext() {
        return "urls";
    }

    //
    // CREATE POST URL
    //

    /**
     * Add a new URL to the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add a new URLs
     * to a specific post given by its Id.
     *
     * @param postId                       The Id of the post into which the new URL should be added.
     * @param postUrlConfigRequest         A PostUrlConfigRequest object, representing the new URL to add.
     * @param authentication               The authentication details of the user making the request.
     * @return a ResponseEntity containing the created URL
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add a new URL to the post given by its Id")
    @ApiResponse(responseCode = "201", description = "Successfully added URL to post")
    @PostMapping(value = "/${api.version}/posts/{postId}/urls", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostConfigResponse> addPostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add URL to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A URL object", required = true,
                    schema = @Schema(implementation = PostUrlConfigRequest.class))
            @Valid PostUrlConfigRequest postUrlConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostUrl adding URL for user={}, postId={}", username, postId);
        StopWatch createTimer = createStarted();
        String entityIdent = getStagingPostService().addPostUrl(username, postId, postUrlConfigRequest);
        createTimer.stop();
        return finalizeAddEntity(username, createTimer, postId, entityIdent);
    }

    //
    // RETRIEVE POST URLs
    //

    /**
     * Get all URLs in the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * URLs in a specific post given by its Id.
     *
     * @param postId          The Id of the post to fetch URLs from.
     * @param offset          The number of items to skip before returning results.
     * @param limit           The maximum number of items to return.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post URLs.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all URLs in a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post URLs",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostUrl.class))))
    @GetMapping(value = "/${api.version}/posts/{postId}/urls", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostUrl>> getPostUrls(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch URLs from", required = true)
            Long postId,
            //
            @Parameter(description = "The number of items to skip before returning results")
            @Valid @RequestParam(name = "offset", required = false)
            @Positive
            Integer offset,
            //
            @Parameter(description = "The maximum number of items to return")
            @Valid @RequestParam(name = "limit", required = false)
            @Positive
            Integer limit,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostUrls for user={}, postId={}", username, postId);
        StopWatch retrieveTimer = createStarted();
        List<PostUrl> postUrls = getStagingPostService().findById(username, postId).getPostUrls();
        return finalizeRetrieveEntities(username, retrieveTimer, postId, postUrls, offset, limit);
    }

    /**
     * Get a single URL in the post given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve a single
     * URL in a post given by its Id.
     *
     * @param postId               The Id of the post to fetch URLs from.
     * @param urlIdent             The identifier of the URL to fetch.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post URL.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a single URL in a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post URL",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostUrl.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/urls/{urlIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostUrl> getPostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch the URL from", required = true)
            Long postId,
            //
            @PathVariable("urlIdent")
            @Parameter(description = "The identifier of the URL to fetch", required = true)
            String urlIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostUrls for user={}, postId={}, urlIdent={}", username, postId, urlIdent);
        StopWatch retrieveTimer = createStarted();
        PostUrl postUrl = getStagingPostService().findUrlByIdent(username, postId, urlIdent);
        retrieveTimer.stop();
        return finalizeRetrieveEntity(username, retrieveTimer, postId, urlIdent, postUrl);
    }

    //
    // UPDATE POST URL
    //

    /**
     * Update all post URLs on a post a given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post URLs on a post given by its Id.
     *
     * @param postId                              The Id of the post to update.
     * @param postUrlConfigRequests               A list of PostUrlConfigRequest objects representing the URLs on the post.
     * @param httpMethod                          The HTTP method in use, either PATCH or PUT.
     * @param authentication                      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update all post URLs on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post URLs")
    @RequestMapping(value = "/${api.version}/posts/{postId}/urls", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostURLs(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @RequestBody @Valid List<? extends PostUrlConfigRequest> postUrlConfigRequests,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostURLs for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostUrls(username, postId, postUrlConfigRequests, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntities(username, updateTimer, updatedPost, size(postUrlConfigRequests));
    }

    /**
     * Update a post URL given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post URL given by its identifier, on a post given by its Id.
     *
     * @param postId                         The Id of the post to update.
     * @param urlIdent                       The identifier of the URL to update.
     * @param postUrlConfigRequest           A PostUrlConfigRequest, representing the URL to update.
     * @param httpMethod                     The HTTP method in use, either PATCH or PUT.
     * @param authentication                 The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update a post URL on a post given by Ident")
    @ApiResponse(responseCode = "200", description = "Successfully updated post URL")
    @RequestMapping(value = "/${api.version}/posts/{postId}/urls/{urlIdent}", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("urlIdent")
            @Parameter(description = "The identifier of the URL to update", required = true)
            String urlIdent,
            //
            @Valid @RequestBody PostUrlConfigRequest postUrlConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostUrl for user={}, postId={}, urlIdent={}, httpMethod={}", username, postId, urlIdent, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostUrl(username, postId, urlIdent, postUrlConfigRequest, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedPost, urlIdent);
    }

    //
    // DELETE POST URL
    //

    /**
     * Delete all post URLs on a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * post URLs on a post given by its Id.
     *
     * @param postId               The Id of the post from which the URLs will be removed.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all post URLs on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post URLs")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/urls", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostUrls(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete URLs from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostUrls for user={}, postId={}", username, postId);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deletePostUrls(username, postId);
        deleteTimer.stop();
        return finalizeDeleteEntities(username, deleteTimer, updatedPost);
    }

    /**
     * Delete a post URL given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * post URL given by its identifier, on a post given by its Id.
     *
     * @param postId               The Id of the post from which the URL will be removed.
     * @param urlIdent             The identifier of the URL to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a post URL by its identifier on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post URL")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/urls/{urlIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete a URL from", required = true)
            Long postId,
            //
            @PathVariable("urlIdent")
            @Parameter(description = "The identifier of the URL to delete", required = true)
            String urlIdent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostUrl for user={}, postId={}, urlIdent={}", username, postId, urlIdent);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deletePostUrl(username, postId, urlIdent);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, urlIdent, updatedPost);
    }
}
