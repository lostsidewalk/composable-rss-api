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
import jakarta.validation.Validator;
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
 * Controller class for managing author-related operations.
 *
 * This controller provides endpoints for managing author objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class PostAuthorController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    Validator validator;

    //
    // CREATE POST AUTHOR
    //

    /**
     * Add new author to the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new author
     * to a specific post identified by its Id.
     *
     * @param postId                 The Id of the post into which the new author should be added.
     * @param postAuthor        A PostPerson object representing new author to add.
     * @param authentication         The authentication details of the user making the request.
     * @return a ResponseEntity containing the created author
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add new author to the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully added author to post")
    @PostMapping(value = "/posts/{postId}/authors", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add author to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A author object", required = true,
                    schema = @Schema(implementation = PostPerson.class))
            @Valid PostPerson postAuthor,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostAuthor adding author for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.addAuthor(username, postId, postAuthor);
        stopWatch.stop();
        appLogService.logStagingPostAddAuthor(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Added author to post Id " + postId));
    }

    //
    // RETRIEVE POST AUTHOR
    //

    /**
     * Get all author in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * authors in a specific post identified by its Id.
     *
     * @param postId          The Id of the post to fetch authors from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post authors.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all authors in the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post authors", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = PostPerson.class))))
    @GetMapping(value = "/posts/{postId}/authors", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostPerson>> getPostAuthors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch authors from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostAuthors for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
            StagingPost stagingPost = stagingPostService.findById(username, postId);
            List<PostPerson> postAuthors = stagingPost.getAuthors();
            stopWatch.stop();
            validator.validate(postAuthors);
            appLogService.logStagingPostAuthorsFetch(username, stopWatch, postId, size(postAuthors));
            return ok(postAuthors);
        }

    //
    // UPDATE POST AUTHOR
    //

    /**
     * Update a post author given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post author identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post to update.
     * @param authorIdx       The index of the author to update.
     * @param postAuthor      A AuthorObject, representing new author to update.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post author given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post author")
    @PutMapping(value = "/posts/{postId}/authors/{authorIdx}", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("authorIdx")
            @Parameter(description = "The index of the author to update", required = true)
            Integer authorIdx,
            //
            @Valid @RequestBody PostPerson postAuthor,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostAuthor for user={}, postId={}, authorIdx={}", username, postId, authorIdx);
        StopWatch stopWatch = createStarted();
            stagingPostService.updatePostAuthor(username, postId, authorIdx, postAuthor);
            stopWatch.stop();
            appLogService.logStagingPostAuthorUpdate(username, stopWatch, postId, authorIdx);
            return ok().body(buildResponseMessage("Updated author on post Id " + postId));
    }

    //
    // DELETE POST AUTHOR
    //

    /**
     * Delete a post author given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a post author identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post whence the author will be removed.
     * @param authorIdx       The index of the author to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post author given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post author")
    @DeleteMapping(value = "/posts/{postId}/authors/{authorIdx}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            //
            @PathVariable("authorIdx")
            @Parameter(description = "The index of the author to delete", required = true)
            Integer authorIdx,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostAuthor for user={}, postId={}, authorIdx={}", username, postId, authorIdx);
        StopWatch stopWatch = createStarted();
            stagingPostService.deletePostAuthor(username, postId, authorIdx);
            stopWatch.stop();
            appLogService.logStagingPostAuthorDelete(username, stopWatch, postId, authorIdx);
            return ok().body(buildResponseMessage("Deleted author from post Id " + postId));
    }
}
