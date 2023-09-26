package com.lostsidewalk.buffy.app.v1.author;

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
public class AuthorController_Retrieve extends AuthorController {

    /**
     * Get all authors in a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * authors in a specific post given by its Id.
     *
     * @param postId         The Id of the post to fetch authors from.
     * @param offset         The number of items to skip before returning results.
     * @param limit          The maximum number of items to return.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post authors.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all authors in a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post authors",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostPerson.class))))
    @GetMapping(value = "/${api.version}/posts/{postId}/authors", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostPerson>> getPostAuthors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch authors from", required = true)
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
        log.debug("getPostAuthors for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<PostPerson> postAuthors = stagingPost.getAuthors();
        if (isNotEmpty(postAuthors)) {
            postAuthors = paginator.paginate(postAuthors, offset, limit);
            validator.validate(postAuthors);
        }
        stopWatch.stop();
        appLogService.logStagingPostAuthorsFetch(username, stopWatch, postId, size(postAuthors));
        return ok(postAuthors);
    }

    /**
     * Get a single author given by its identifier on a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve a single
     * author given by its identifier on a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the author from.
     * @param authorIdent    The identifier of the author to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post author.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a single author in a post by its Id and author ident")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post author",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostPerson.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/authors/{authorIdent}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostPerson> getPostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch the author from", required = true)
            Long postId,
            @PathVariable("authorIdent")
            @Parameter(description = "The identifier of the author to fetch", required = true)
            String authorIdent,
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostAuthors for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        PostPerson postAuthor = stagingPostService.findAuthorByIdent(username, postId, authorIdent);
        stopWatch.stop();
        appLogService.logStagingPostAuthorFetch(username, stopWatch, postId, authorIdent);
        return ok(postAuthor);
    }
}
