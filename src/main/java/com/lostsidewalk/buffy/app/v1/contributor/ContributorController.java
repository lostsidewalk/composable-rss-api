package com.lostsidewalk.buffy.app.v1.contributor;

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
 * Controller class for managing contributor-related operations.
 * <p>
 * This controller provides endpoints for managing contributor objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
class ContributorController extends BasePostEntityController<PostPerson> {

    @SuppressWarnings("MethodReturnAlwaysConstant")
    @Override
    protected final String getEntityContext() {
        return "contributors";
    }

    /**
     * Add a new contributor to a specific post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new contributor
     * to a specific post given by its Id.
     *
     * @param postId                       The Id of the post to which the new contributor should be added.
     * @param postContributorConfigRequest A PostPersonConfigRequest object representing new contributor to add.
     * @param authentication               The authentication details of the user making the request.
     * @return a ResponseEntity containing the created contributor
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add a new contributor to a post by its Id")
    @ApiResponse(responseCode = "201", description = "Successfully added the contributor to the post")
    @PostMapping(value = "/${api.version}/posts/{postId}/contributors", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostConfigResponse> addPostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add contributor to", required = true)
            Long postId,
            @RequestBody
            @Parameter(description = "A contributor object", required = true,
                    schema = @Schema(implementation = PostPersonConfigRequest.class))
            @Valid PostPersonConfigRequest postContributorConfigRequest,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostContributor adding contributor for user={}, postId={}", username, postId);
        StopWatch createTimer = createStarted();
        String entityIdent = getStagingPostService().addContributor(username, postId, postContributorConfigRequest);
        createTimer.stop();
        return finalizeAddEntity(username, createTimer, postId, entityIdent);
    }

    /**
     * Get all contributors in a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * contributors in a specific post given by its Id.
     *
     * @param postId         The Id of the post to fetch contributors from.
     * @param offset         The number of items to skip before returning results.
     * @param limit          The maximum number of items to return.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post contributors.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all contributors in a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post contributors",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostPerson.class))))
    @GetMapping(value = "/${api.version}/posts/{postId}/contributors", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostPerson>> getPostContributors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch contributors from", required = true)
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
        log.debug("getPostContributors for user={}, postId={}", username, postId);
        StopWatch retrieveTimer = createStarted();
        List<PostPerson> postContributors = getStagingPostService().findById(username, postId).getContributors();
        retrieveTimer.stop();
        return finalizeRetrieveEntities(username, retrieveTimer, postId, postContributors, offset, limit);
    }

    /**
     * Get a single contributor in a post given its Id and the contributor identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve a single
     * contributor in a specific post given by its Id.
     *
     * @param postId           The Id of the post to fetch contributors from.
     * @param contributorIdent The identifier of the contributor to fetch.
     * @param authentication   The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post contributors.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a single contributor in a post by its Id and contributor ident")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post contributor",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostPerson.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/contributors/{contributorIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostPerson> getPostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch the contributor from", required = true)
            Long postId,
            @PathVariable("contributorIdent")
            @Parameter(description = "The identifier of the contributor to fetch", required = true)
            String contributorIdent,
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostContributors for user={}, postId={}", username, postId);
        StopWatch retrieveTimer = createStarted();
        PostPerson postContributor = getStagingPostService().findContributorByIdent(username, postId, contributorIdent);
        retrieveTimer.stop();
        return finalizeRetrieveEntity(username, retrieveTimer, postId, contributorIdent, postContributor);
    }

    /**
     * Update all post contributors on a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post contributors on a post given by its Id.
     *
     * @param postId                        The Id of the post to update.
     * @param postContributorConfigRequests A list of PostPersonConfigRequest objects representing the contributors of the post.
     * @param httpMethod                    The HTTP method in use, either PATCH or PUT.
     * @param authentication                The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update all post contributors on a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post contributors")
    @RequestMapping(value = "/${api.version}/posts/{postId}/contributors", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostContributors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            @RequestBody @Valid List<? extends PostPersonConfigRequest> postContributorConfigRequests,
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContributors for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updateContributors(username, postId, postContributorConfigRequests, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntities(username, updateTimer, updatedPost, size(postContributorConfigRequests));
    }

    /**
     * Update a post contributor given by identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post contributor given by its identifier, on a post given by its Id.
     *
     * @param postId                       The Id of the post to update.
     * @param contributorIdent             The identifier of the contributor to update.
     * @param postContributorConfigRequest A PostPersonConfigRequest representing new contributor to update.
     * @param httpMethod                   The HTTP method in use, either PATCH or PUT.
     * @param authentication               The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update a post contributor by identifier on a post by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post contributor")
    @RequestMapping(value = "/${api.version}/posts/{postId}/contributors/{contributorIdent}", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("contributorIdent")
            @Parameter(description = "The identifier of the contributor to update", required = true)
            String contributorIdent,
            //
            @Valid @RequestBody PostPersonConfigRequest postContributorConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContributor for user={}, postId={}, contributorIdent={}, httpMethod={}", username, postId, contributorIdent, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updateContributor(username, postId, contributorIdent, postContributorConfigRequest, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedPost, contributorIdent);
    }

    /**
     * Delete all contributors from a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * contributors from a post given by its Id.
     *
     * @param postId         The Id of the post from which contributors will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all contributors from a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post contributors")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/contributors", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostContributors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContributors for user={}, postId={}", username, postId);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deleteContributors(username, postId);
        deleteTimer.stop();
        return finalizeDeleteEntities(username, deleteTimer, updatedPost);
    }

    /**
     * Delete a contributor from a post given its Id and contributor identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * contributor from a post given by its Id and the contributor's identifier.
     *
     * @param postId           The Id of the post from which the contributor will be removed.
     * @param contributorIdent The identifier of the contributor to delete.
     * @param authentication   The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a contributor from a post by Id and the contributor ident")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post contributor")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/contributors/{contributorIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete the contributor from", required = true)
            Long postId,
            @PathVariable("contributorIdent")
            @Parameter(description = "The identifier of the contributor to delete", required = true)
            String contributorIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContributor for user={}, postId={}, contributorIdent={}", username, postId, contributorIdent);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deleteContributor(username, postId, contributorIdent);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, contributorIdent, updatedPost);
    }
}
