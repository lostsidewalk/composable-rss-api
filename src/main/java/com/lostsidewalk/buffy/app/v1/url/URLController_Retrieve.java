package com.lostsidewalk.buffy.app.v1.url;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.post.PostUrl;
import com.lostsidewalk.buffy.post.StagingPost;
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

@Slf4j
@RestController
@Validated
public class URLController_Retrieve extends URLController {

    //
    // RETRIEVE POST URLs
    //

    /**
     * Get all URLs in the post given by its Id.
     *
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
    @GetMapping(value = "/${api.version}/posts/{postId}/urls", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<PostUrl> postUrls = stagingPost.getPostUrls();
        if (isNotEmpty(postUrls)) {
            postUrls = paginator.paginate(postUrls, offset, limit);
            validator.validate(postUrls);
        }
        stopWatch.stop();
        appLogService.logStagingPostUrlsFetch(username, stopWatch, postId, size(postUrls));
        return ok(postUrls);
    }

    /**
     * Get a single URL in the post given by its identifier.
     *
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
    @GetMapping(value = "/${api.version}/posts/{postId}/urls/{urlIdent}", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        PostUrl postUrls = stagingPostService.findUrlByIdent(username, postId, urlIdent);
        stopWatch.stop();
        appLogService.logStagingPostUrlFetch(username, stopWatch, postId, urlIdent);
        return ok(postUrls);
    }
}
