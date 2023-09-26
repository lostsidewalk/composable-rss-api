package com.lostsidewalk.buffy.app.v1.content;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.post.ContentObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for retrieving content from a post.
 */
@Slf4j
@RestController
@Validated
class ContentController_Retrieve extends ContentController {

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
    @GetMapping(value = "/${api.version}/posts/{postId}/content", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        List<ContentObject> postContent = stagingPostService.findById(username, postId).getPostContents();
        if (isNotEmpty(postContent)) {
            postContent = paginator.paginate(postContent, offset, limit);
            validator.validate(postContent);
        }
        stopWatch.stop();
        appLogService.logStagingPostContentsFetch(username, stopWatch, postId, size(postContent));
        return ok(postContent);
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
    @GetMapping(value = "/${api.version}/posts/{postId}/content/{contentIdent}", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        ContentObject postContent = stagingPostService.findContentByIdent(username, postId, contentIdent);
        stopWatch.stop();
        appLogService.logStagingPostContentFetch(username, stopWatch, postId, contentIdent);
        return ok(postContent);
    }
}
