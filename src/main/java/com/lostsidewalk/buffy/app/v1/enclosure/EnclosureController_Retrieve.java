package com.lostsidewalk.buffy.app.v1.enclosure;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.post.PostEnclosure;
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
public class EnclosureController_Retrieve extends EnclosureController {

    //
    // RETRIEVE POST ENCLOSURES
    //

    /**
     * Get all enclosures in the post given by its Id.
     *
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
    @GetMapping(value = "/${api.version}/posts/{postId}/enclosures", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<PostEnclosure> postEnclosures = stagingPost.getEnclosures();
        if (isNotEmpty(postEnclosures)) {
            postEnclosures = paginator.paginate(postEnclosures, offset, limit);
            validator.validate(postEnclosures);
        }
        stopWatch.stop();
        appLogService.logStagingPostEnclosuresFetch(username, stopWatch, postId, size(postEnclosures));
        return ok(postEnclosures);
    }

    /**
     * Get a single enclosure in the post given by its Id.
     *
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
    @GetMapping(value = "/${api.version}/posts/{postId}/enclosures/{enclosureIdent}", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        PostEnclosure postEnclosure = stagingPostService.findEnclosureByIdent(username, postId, enclosureIdent);
        stopWatch.stop();
        appLogService.logStagingPostEnclosureFetch(username, stopWatch, postId, enclosureIdent);
        return ok(postEnclosure);
    }
}
