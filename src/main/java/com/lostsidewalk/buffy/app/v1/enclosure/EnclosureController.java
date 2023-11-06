package com.lostsidewalk.buffy.app.v1.enclosure;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostEnclosureConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.v1.BasePostEntityController;
import com.lostsidewalk.buffy.post.PostEnclosure;
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
 * Controller class for managing enclosure-related operations.
 * <p>
 * This controller provides endpoints for managing enclosure objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
class EnclosureController extends BasePostEntityController<PostEnclosure> {

    @SuppressWarnings("MethodReturnAlwaysConstant")
    @Override
    protected final String getEntityContext() {
        return "enclosures";
    }

    //
    // CREATE POST ENCLOSURE
    //

    /**
     * Add a new enclosure to the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add a new enclosure
     * to a specific post given by its Id.
     *
     * @param postId The Id of the post into which the new enclosure should be added.
     * @param postEnclosureConfigRequest A PostEnclosureConfigRequest object representing the new enclosure to add.
     * @param authentication The authentication details of the user making the request.
     * @return a ResponseEntity containing the created enclosure
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add a new enclosure to the post given by its Id")
    @ApiResponse(responseCode = "201", description = "Successfully added enclosure to post")
    @PostMapping(value = "/${api.version}/posts/{postId}/enclosures", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostConfigResponse> addPostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add the enclosure to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "An enclosure object", required = true,
                    schema = @Schema(implementation = PostEnclosureConfigRequest.class))
            @Valid PostEnclosureConfigRequest postEnclosureConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostEnclosure adding enclosure for user={}, postId={}", username, postId);
        StopWatch createTimer = createStarted();
        String enclosureIdent = getStagingPostService().addEnclosure(username, postId, postEnclosureConfigRequest);
        createTimer.stop();
        return finalizeAddEntity(username, createTimer, postId, enclosureIdent);
    }

    //
    // RETRIEVE POST ENCLOSURES
    //

    /**
     * Get all enclosures in the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * enclosures in a specific post given by its Id.
     *
     * @param postId          The Id of the post to fetch enclosures from.
     * @param offset          The number of items to skip before returning results.
     * @param limit           The maximum number of items to return.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post enclosures.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all enclosures in a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post enclosures",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostEnclosure.class))))
    @GetMapping(value = "/${api.version}/posts/{postId}/enclosures", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostEnclosure>> getPostEnclosures(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch enclosures from", required = true)
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
        log.debug("getPostEnclosures for user={}, postId={}", username, postId);
        StopWatch retrieveTimer = createStarted();
        List<PostEnclosure> postEnclosures = getStagingPostService().findById(username, postId).getEnclosures();
        return finalizeRetrieveEntities(username, retrieveTimer, postId, postEnclosures, offset, limit);
    }

    /**
     * Get a single enclosure in the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve a single
     * enclosure in a specific post given by its Id.
     *
     * @param postId               The Id of the post to fetch enclosures from.
     * @param enclosureIdent       The identifier of the enclosure to fetch.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post enclosure.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a single enclosure in a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post enclosure",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostEnclosure.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/enclosures/{enclosureIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostEnclosure> getPostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch the enclosure from", required = true)
            Long postId,
            //
            @PathVariable("enclosureIdent")
            @Parameter(description = "The identifier of the enclosure to fetch", required = true)
            String enclosureIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostEnclosures for user={}, postId={}, enclosureIdent={}", username, postId, enclosureIdent);
        StopWatch retrieveTimer = createStarted();
        PostEnclosure postEnclosure = getStagingPostService().findEnclosureByIdent(username, postId, enclosureIdent);
        retrieveTimer.stop();
        return finalizeRetrieveEntity(username, retrieveTimer, postId, enclosureIdent, postEnclosure);
    }

    //
    // UPDATE POST ENCLOSURE
    //

    /**
     * Update all post enclosures on a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post enclosures, on a post given by its Id.
     *
     * @param postId                          The Id of the post to update.
     * @param postEnclosureConfigRequests     A list of PostEnclosureConfigRequest objects, representing the enclosures on the post.
     * @param httpMethod                      The HTTP method in use, either PATCH or PUT.
     * @param authentication                  The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update all post enclosures on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post enclosures")
    @RequestMapping(value = "/${api.version}/posts/{postId}/enclosures", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostEnclosures(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @RequestBody @Valid List<? extends PostEnclosureConfigRequest> postEnclosureConfigRequests,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostEnclosures for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updateEnclosures(username, postId, postEnclosureConfigRequests, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntities(username, updateTimer, updatedPost, size(postEnclosureConfigRequests));
    }

    /**
     * Update a post enclosure given by its identifier on a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post enclosure given by its identifier, on a post given by its Id.
     *
     * @param postId                          The Id of the post to update.
     * @param enclosureIdent                  The identifier of the enclosure to update.
     * @param postEnclosureConfigRequest      A PostEnclosureConfigRequest, representing the enclosure to update.
     * @param httpMethod                      The HTTP method in use, either PATCH or PUT.
     * @param authentication                  The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update a post enclosure on a post given by Ident")
    @ApiResponse(responseCode = "200", description = "Successfully updated post enclosure")
    @RequestMapping(value = "/${api.version}/posts/{postId}/enclosures/{enclosureIdent}", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("enclosureIdent")
            @Parameter(description = "The identifier of the enclosure to update", required = true)
            String enclosureIdent,
            //
            @Valid @RequestBody PostEnclosureConfigRequest postEnclosureConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostEnclosure for user={}, postId={}, enclosureIdent={}, httpMethod={}", username, postId, enclosureIdent, httpMethod);
        StopWatch updateTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().updateEnclosure(username, postId, enclosureIdent, postEnclosureConfigRequest, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedPost, enclosureIdent);
    }

    //
    // DELETE POST ENCLOSURE
    //

    /**
     * Delete all post enclosures on a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * post enclosures on a post given by its Id.
     *
     * @param postId The Id of the post from which the enclosures will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all post enclosures on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post enclosures")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/enclosures", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostEnclosures(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete enclosures from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostEnclosures for user={}, postId={}", username, postId);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deleteEnclosures(username, postId);
        deleteTimer.stop();
        return finalizeDeleteEntities(username, deleteTimer, updatedPost);
    }

    /**
     * Delete a post enclosure given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * post enclosure given by its identifier, on a post given by its Id.
     *
     * @param postId The Id of the post from which the enclosure will be removed.
     * @param enclosureIdent The identifier of the enclosure to delete.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a post enclosure by its identifier on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post enclosure")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/enclosures/{enclosureIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete an enclosure from", required = true)
            Long postId,
            //
            @PathVariable("enclosureIdent")
            @Parameter(description = "The identifier of the enclosure to delete", required = true)
            String enclosureIdent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostEnclosure for user={}, postId={}, enclosureIdent={}", username, postId, enclosureIdent);
        StopWatch deleteTimer = createStarted();
        StagingPost updatedPost = getStagingPostService().deleteEnclosure(username, postId, enclosureIdent);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, enclosureIdent, updatedPost);
    }
}
