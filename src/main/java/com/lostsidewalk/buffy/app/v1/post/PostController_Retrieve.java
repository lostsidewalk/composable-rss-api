package com.lostsidewalk.buffy.app.v1.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostITunes;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@Slf4j
@RestController
@Validated
public class PostController_Retrieve extends PostController {

    /**
     * Get a staging post by its Id.
     *
     * @param postId                 The Id of the post to fetch.
     * @param ifNoneMatch            if-none-match HTTP header value (for e-tag evaluation).
     * @param authentication         The authenticated user's details.
     * @return A ResponseEntity containing the fetched staging post.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a staging post by Id")
    @GetMapping(value = "/${api.version}/posts/{postId}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @ApiResponse(responseCode = "200", description = "Successfully fetched staging post",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostDTO.class)))
    public ResponseEntity<PostDTO> getPost(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch", required = true)
            Long postId,
            //
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            //
            Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostById for user={}, postId={}", username, postId);
        StopWatch stopWatch = StopWatch.createStarted();
        // staging posts
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        String eTag = eTagger.computeEtag(stagingPost);
        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            return status(NOT_MODIFIED).build();
        }
        String queueIdent = queueDefinitionService.resolveQueueIdent(username, stagingPost.getQueueId());
        PostDTO post = preparePostDTO(stagingPost, queueIdent);
        validator.validate(post);
        stopWatch.stop();
        appLogService.logStagingPostFetch(username, stopWatch, 1, 1);
        return ok()
                .eTag(eTag)
                .body(post);
    }

    //
    // RETRIEVE POST INDIVIDUAL FIELDS
    //

    /**
     * Get the status of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the status of a
     * posts given by its Id.  The status of a post is given by the PostPubStatus enumeration.
     *
     * @param postId          The Id of the post to fetch the status from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post status.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the status of a post given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post status",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/status", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPostStatus(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch status from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostStatus for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        String response = null;
        if (stagingPost.isPublished()) {
            response = "PUBLISHED";
        } else if (stagingPost.getPostPubStatus() != null) {
            response = stagingPost.getPostPubStatus().name();
        } else {
            response = StagingPost.PostPubStatus.UNPUBLISHED.name();
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "status");
        return ok(response);
    }

    /**
     * Get the queue identifier of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * queue identifier of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the queue identifier from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post queue identifier.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the queue identifier of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post queue Id",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/queue", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPostQueueId(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch queue Id from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostQueueId for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        String queueIdent = queueDefinitionService.findByQueueId(username, stagingPost.getQueueId()).getIdent();
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "queueId");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueIdent));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueIdent, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the title of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * title of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the title from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post title.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the title of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post title",
            content = @Content(schema = @Schema(implementation = ContentObject.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/title", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ContentObject> getPostTitle(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch title from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostTitle for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        ContentObject postTitle = stagingPost.getPostTitle();
        if (postTitle != null) {
            validator.validate(postTitle);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postTitle");
        return ok(postTitle);
    }

    /**
     * Get the description of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * description of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the description from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post description.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the description of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post description",
            content = @Content(schema = @Schema(implementation = ContentObject.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/description", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ContentObject> getPostDesc(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch description from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostDesc for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        ContentObject postDesc = stagingPost.getPostDesc();
        if (postDesc != null) {
            validator.validate(postDesc);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postDesc");
        return ok(postDesc);
    }

    /**
     * Get the iTunes descriptor of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * iTunes descriptor of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the iTunes metadata from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post iTunes descriptor.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the iTunes descriptor of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post iTunes descriptor",
            content = @Content(schema = @Schema(implementation = PostITunes.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/itunes", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostITunes> getPostITunes(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch iTunes descriptor from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostITunes for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        PostITunes postITunes = stagingPost.getPostITunes();
        if (postITunes != null) {
            validator.validate(postITunes);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postITunes");
        return ok(postITunes);
    }

    /**
     * Get the comment string of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * comment string of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the comment string from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post comment string.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the comment string of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post comment string",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/comment", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPostComment(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch comment string from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostComment for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postComment");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(stagingPost.getPostComment()));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(stagingPost.getPostComment(), headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the rights string of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * rights string of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the rights strings from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post rights string.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the rights string of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post rights string",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/rights", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPostRights(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch rights string from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostRights for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postRights");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(stagingPost.getPostRights()));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(stagingPost.getPostRights(), headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get all categories in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * categories in a specific post given by its Id.
     *
     * @param postId          The Id of the post to fetch categories from.
     * @param offset          The number of items to skip before returning results.
     * @param limit           The maximum number of items to return.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post categories.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all categories in the post given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post categories",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @GetMapping(value = "/${api.version}/posts/{postId}/categories", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<String>> getPostCategories(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch categories from", required = true)
            Long postId,
            //
            @Parameter(name = "The number of items to skip before returning results")
            @Valid @RequestParam(name = "offset", required = false)
            @Positive
            Integer offset,
            //
            @Parameter(name = "The maximum number of items to return")
            @Valid @RequestParam(name = "limit", required = false)
            @Positive
            Integer limit,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostCategories for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        List<String> postCategories = stagingPost.getPostCategories();
        if (isNotEmpty(postCategories)) {
            postCategories = paginator.paginate(postCategories, offset, limit);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, postId, "postCategories");
        return ok(postCategories);
    }

    /**
     * Get the expiration timestamp of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * expiration timestamp of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the expiration timestamp from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post expiration timestamp.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the expiration timestamp of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post expiration timestamp",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/expiration", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getExpirationTimestamp(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch expiration timestamp from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getExpirationTimestamp for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        Date expirationTimestamp = stagingPost.getExpirationTimestamp();
        String responseStr = expirationTimestamp != null ?
                ISO_8601_TIMESTAMP_FORMAT.format(expirationTimestamp) :
                null; // default response
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "expirationTimestamp");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the published timestamp of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * published timestamp of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the published timestamp from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post published timestamp.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the published timestamp of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post published timestamp",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/published", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPublishedTimestamp(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch published timestamp from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPublishedTimestamp for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        Date publishTimestamp = stagingPost.getPublishTimestamp();
        String responseStr = publishTimestamp != null ?
                ISO_8601_TIMESTAMP_FORMAT.format(publishTimestamp) :
                null; // default response
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "publishTimestamp");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the last updated timestamp of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * last updated timestamp of a post given by its Id.
     *
     * @param postId          The Id of the post to fetch the last updated tiemstamp from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post last updated timestamp.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the last updated timestamp of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post last updated timestamp",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/updated", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getLastUpdatedTimestamp(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch last updated timestamp from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getLastUpdatedTimestamp for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        Date lastUpdatedTimestamp = stagingPost.getLastUpdatedTimestamp();
        String responseStr = lastUpdatedTimestamp != null ?
                ISO_8601_TIMESTAMP_FORMAT.format(lastUpdatedTimestamp) :
                null; // default response
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "lastUpdatedTimestamp");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }
}
