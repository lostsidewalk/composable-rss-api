package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.post.StagingPost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
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
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for managing category-related operations.
 *
 * This controller provides endpoints for managing category objects within posts. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class PostCategoryController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    //
    // ADD A CATEGORY TO A POST
    //

    /**
     * Add a new category to the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new category
     * to a specific post identified by its Id.
     *
     * @param postId                 The Id of the post into which the new category should be added.
     * @param postCategory           A string value representing the name of the new category to add.
     * @param authentication         The authentication details of the user making the request.
     * @return a ResponseEntity containing the created category
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add new category to the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully added category to post")
    @PostMapping(value = "/posts/{postId}/categories", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostCategory(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add category to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "The name of a category", required = true,
                    schema = @Schema(implementation = String.class))
            String postCategory,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostCategory adding category for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.addPostCategory(username, postId, postCategory);
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, postId, "postCategories");
        return ok().body(buildResponseMessage("Added category to post Id " + postId));
    }

    //
    // RETRIEVE POST CATEGORIES
    //

    /**
     * Get all categories in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * categories in a specific post identified by its Id.
     *
     * @param postId          The Id of the post to fetch categories from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post categories.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all categories in the post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post categories",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @GetMapping(value = "/posts/{postId}/categories", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<String>> getPostCategories(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch categories from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostCategories for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<String> postCategories = stagingPost.getPostCategories();
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, postId, "postCategories");
        return ok(postCategories);
    }

    //
    // DELETE POST CATEGORY
    //

    /**
     * Delete a post category given by index.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a post category identified by its index, on a post identified by its Id.
     *
     * @param postId               The Id of the post whence the category will be removed.
     * @param categoryName         The name of the category to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post category given by idx, on a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post category")
    @DeleteMapping(value = "/posts/{postId}/categories/{categoryName}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostCategory(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            //
            @PathVariable("categoryName")
            @Parameter(description = "The name of the category to delete", required = true)
            String categoryName,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostCategory for user={}, postId={}, categoryName={}", username, postId, categoryName);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostCategory(username, postId, categoryName);
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, postId, "postCategories");
        return ok().body(buildResponseMessage("Deleted category from post Id " + postId));
    }
}
