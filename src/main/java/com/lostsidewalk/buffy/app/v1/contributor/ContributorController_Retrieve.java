package com.lostsidewalk.buffy.app.v1.contributor;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.post.PostPerson;
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
public class ContributorController_Retrieve extends ContributorController {

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
    @GetMapping(value = "/${api.version}/posts/{postId}/contributors", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<PostPerson> postContributors = stagingPost.getContributors();
        if (isNotEmpty(postContributors)) {
            postContributors = paginator.paginate(postContributors, offset, limit);
            validator.validate(postContributors);
        }
        stopWatch.stop();
        appLogService.logStagingPostContributorsFetch(username, stopWatch, postId, size(postContributors));
        return ok(postContributors);
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
    @GetMapping(value = "/${api.version}/posts/{postId}/contributors/{contributorIdent}", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        PostPerson postContributor = stagingPostService.findContributorByIdent(username, postId, contributorIdent);
        stopWatch.stop();
        appLogService.logStagingPostContributorFetch(username, stopWatch, postId, contributorIdent);
        return ok(postContributor);
    }
}
