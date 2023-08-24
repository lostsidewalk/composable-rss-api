package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.post.PostPerson;
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
 * Controller class for managing contributor-related operations.
 *
 * This controller provides endpoints for managing contributor objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class PostContributorController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    //
    // CREATE POST CONTRIBUTOR
    //

    /**
     * Add new contributor to the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new contributor
     * to a specific post identified by its Id.
     *
     * @param postId                 The Id of the post into which the new contributor should be added.
     * @param postContributor        A PostPerson object representing new contributor to add.
     * @param authentication         The authentication details of the user making the request.
     * @return a ResponseEntity containing the created contributor
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add new contributor to the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully added contributor to post")
    @PostMapping(value = "/posts/{postId}/contributors", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add contributor to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A contributor object", required = true,
                    schema = @Schema(implementation = PostPerson.class))
            @Valid PostPerson postContributor,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostContributor adding contributor for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.addContributor(username, postId, postContributor);
        stopWatch.stop();
        appLogService.logStagingPostAddContributor(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Added contributor to post Id " + postId));
    }

    //
    // RETRIEVE POST CONTRIBUTOR
    //

    /**
     * Get all contributor in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * contributors in a specific post identified by its Id.
     *
     * @param postId          The Id of the post to fetch contributors from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post contributors.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all contributors in the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post contributors",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostPerson.class))))
    @GetMapping(value = "/posts/{postId}/contributors", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostPerson>> getPostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch contributors from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostContributors for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<PostPerson> postContributors = stagingPost.getContributors();
        stopWatch.stop();
        appLogService.logStagingPostContributorsFetch(username, stopWatch, postId, size(postContributors));
        return ok(postContributors);
    }

    //
    // UPDATE POST CONTRIBUTOR
    //

    /**
     * Update a post contributor given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post contributor identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post to update.
     * @param contributorIdx       The index of the contributor to update.
     * @param postContributor      A ContributorObject, representing new contributor to update.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post contributor given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post contributor")
    @PutMapping(value = "/posts/{postId}/contributors/{contributorIdx}", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("contributorIdx")
            @Parameter(description = "The index of the contributor to update", required = true)
            Integer contributorIdx,
            //
            @Valid @RequestBody PostPerson postContributor,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContributor for user={}, postId={}, contributorIdx={}", username, postId, contributorIdx);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostContributor(username, postId, contributorIdx, postContributor);
        stopWatch.stop();
        appLogService.logStagingPostContributorUpdate(username, stopWatch, postId, contributorIdx);
        return ok().body(buildResponseMessage("Updated contributor on post Id " + postId));
    }

    //
    // DELETE POST CONTRIBUTOR
    //

    /**
     * Delete a post contributor given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a post contributor identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post whence the contributor will be removed.
     * @param contributorIdx       The index of the contributor to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post contributor given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post contributor")
    @DeleteMapping(value = "/posts/{postId}/contributors/{contributorIdx}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            //
            @PathVariable("contributorIdx")
            @Parameter(description = "The index of the contributor to delete", required = true)
            Integer contributorIdx,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContributor for user={}, postId={}, contributorIdx={}", username, postId, contributorIdx);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostContributor(username, postId, contributorIdx);
        stopWatch.stop();
        appLogService.logStagingPostContributorDelete(username, stopWatch, postId, contributorIdx);
        return ok().body(buildResponseMessage("Deleted contributor from post Id " + postId));
    }
}
