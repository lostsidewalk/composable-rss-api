package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.post.PostUrl;
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
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for managing URL-related operations.
 *
 * This controller provides endpoints for managing URLs within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class PostURLController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    //
    // CREATE POST URL
    //

    /**
     * Add a new URL to the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new URLs
     * to a specific post identified by its Id.
     *
     * @param postId                 The Id of the post into which the new URL should be added.
     * @param postUrl                A PostUrl object, representing new URL to add.
     * @param authentication         The authentication details of the user making the request.
     * @return a ResponseEntity containing the URL to add
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add a new URL to the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully added URL to post")
    @PostMapping("/posts/{postId}/urls")
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add URL to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A post URL object", required = true,
                    schema = @Schema(implementation = PostUrl.class))
            @Valid PostUrl postUrl,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostUrl adding URL for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.addPostUrl(username, postId, postUrl);
        stopWatch.stop();
        appLogService.logStagingPostAddUrl(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Added URL to post Id " + postId));
    }

    //
    // RETRIEVE POST URLs
    //

    /**
     * Get all URLs in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * URLs in a specific post identified by its Id.
     *
     * @param postId          The Id of the post to fetch URLs from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post URLs.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all URLs in the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post URLs",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostUrl.class))))
    @GetMapping("/posts/{postId}/urls")
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostUrl>> getPostUrls(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch URLs from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostUrls for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<PostUrl> postUrls = stagingPost.getPostUrls();
        stopWatch.stop();
        appLogService.logStagingPostUrlsFetch(username, stopWatch, postId, size(postUrls));
        return ok(postUrls);
    }

    //
    // UPDATE POST URL
    //

    /**
     * Update a post URL given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post URL identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post to update.
     * @param urlIdx               The index of the URL to update.
     * @param postUrl              A PostUrl, representing the URL to update.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post URL given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post URL")
    @PutMapping("/posts/{postId}/urls/{urlIdx}")
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("urlIdx")
            @Parameter(description = "The index of the URL to update", required = true)
            Integer urlIdx,
            //
            @Valid @RequestBody PostUrl postUrl,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostUrl for user={}, postId={}, urlIdx={}", username, postId, urlIdx);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostUrl(username, postId, urlIdx, postUrl);
        stopWatch.stop();
        appLogService.logStagingPostUrlUpdate(username, stopWatch, postId, urlIdx);
        return ok().body(buildResponseMessage("Updated URL on post Id " + postId));
    }

    //
    // DELETE POST URL
    //

    /**
     * Delete a post URL given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * post URL identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post whence the URL will be removed.
     * @param urlIdx               The index of the URL to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post URL given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post URL")
    @DeleteMapping("/posts/{postId}/urls/{urlIdx}")
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post from which the URL will be removed", required = true)
            Long postId,
            //
            @PathVariable("urlIdx")
            @Parameter(description = "The index of the URL to delete", required = true)
            Integer urlIdx,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostUrl for user={}, postId={}, urlIdx={}", username, postId, urlIdx);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostUrl(username, postId, urlIdx);
        stopWatch.stop();
        appLogService.logStagingPostUrlDelete(username, stopWatch, postId, urlIdx);
        return ok().body(buildResponseMessage("Deleted URL from post Id " + postId));
    }
}
