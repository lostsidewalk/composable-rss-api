package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.post.PostEnclosure;
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
 * Controller class for managing enclosure-related operations.
 *
 * This controller provides endpoints for managing enclosure objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class PostEnclosureController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    //
    // CREATE POST ENCLOSURE
    //

    /**
     * Add new enclosure to the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new enclosure
     * to a specific post identified by its Id.
     *
     * @param postId                 The Id of the post into which the new enclosure should be added.
     * @param postEnclosure        A PostEnclosure object representing new enclosure to add.
     * @param authentication         The authentication details of the user making the request.
     * @return a ResponseEntity containing the created enclosure
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add new enclosure to the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully added enclosure to post")
    @PostMapping(value = "/posts/{postId}/enclosures", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add enclosure to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A enclosure object", required = true,
                    schema = @Schema(implementation = PostEnclosure.class))
            @Valid PostEnclosure postEnclosure,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostEnclosure adding enclosure for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.addEnclosure(username, postId, postEnclosure);
        stopWatch.stop();
        appLogService.logStagingPostAddEnclosure(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Added enclosure to post Id " + postId));
    }

    //
    // RETRIEVE POST ENCLOSURE
    //

    /**
     * Get all enclosure in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * enclosures in a specific post identified by its Id.
     *
     * @param postId          The Id of the post to fetch enclosures from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post enclosures.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all enclosures in the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post enclosures",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostEnclosure.class))))
    @GetMapping(value = "/posts/{postId}/enclosures", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostEnclosure>> getPostEnclosures(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch enclosures from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostEnclosures for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<PostEnclosure> postEnclosures = stagingPost.getEnclosures();
        stopWatch.stop();
        appLogService.logStagingPostEnclosuresFetch(username, stopWatch, postId, size(postEnclosures));
        return ok(postEnclosures);
    }

    //
    // UPDATE POST ENCLOSURE
    //

    /**
     * Update a post enclosure given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post enclosure identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post to update.
     * @param enclosureIdx       The index of the enclosure to update.
     * @param postEnclosure      A EnclosureObject, representing new enclosure to update.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post enclosure given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post enclosure")
    @PutMapping(value = "/posts/{postId}/enclosures/{enclosureIdx}", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("enclosureIdx")
            @Parameter(description = "The index of the enclosure to update", required = true)
            Integer enclosureIdx,
            //
            @Valid @RequestBody PostEnclosure postEnclosure,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostEnclosure for user={}, postId={}, enclosureIdx={}", username, postId, enclosureIdx);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostEnclosure(username, postId, enclosureIdx, postEnclosure);
        stopWatch.stop();
        appLogService.logStagingPostEnclosureUpdate(username, stopWatch, postId, enclosureIdx);
        return ok().body(buildResponseMessage("Updated enclosure on post Id " + postId));
    }

    //
    // DELETE POST ENCLOSURE
    //

    /**
     * Delete a post enclosure given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a post enclosure identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post whence the enclosure will be removed.
     * @param enclosureIdx       The index of the enclosure to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post enclosure given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post enclosure")
    @DeleteMapping(value = "/posts/{postId}/enclosures/{enclosureIdx}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            //
            @PathVariable("enclosureIdx")
            @Parameter(description = "The index of the enclosure to delete", required = true)
            Integer enclosureIdx,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostEnclosure for user={}, postId={}, enclosureIdx={}", username, postId, enclosureIdx);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostEnclosure(username, postId, enclosureIdx);
        stopWatch.stop();
        appLogService.logStagingPostEnclosureDelete(username, stopWatch, postId, enclosureIdx);
        return ok().body(buildResponseMessage("Deleted enclosure from post Id " + postId));
    }
}
