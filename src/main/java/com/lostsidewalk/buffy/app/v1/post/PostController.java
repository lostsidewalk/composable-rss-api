package com.lostsidewalk.buffy.app.v1.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.etag.ETagger;
import com.lostsidewalk.buffy.app.model.v1.request.PostConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
import com.lostsidewalk.buffy.app.model.v1.response.PostDeleteResponse;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.v1.BasePostController;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostITunes;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.lostsidewalk.buffy.app.audit.AppLogService.*;
import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller class for managing post-related operations.
 * <p>
 * This controller provides endpoints for managing staging posts and subsidiary entities. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
public class PostController extends BasePostController {

    /**
     * Get a staging post by its Id.
     *
     * @param postId         The Id of the post to fetch.
     * @param ifNoneMatch    if-none-match HTTP header value (for e-tag evaluation).
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity containing the fetched staging post.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a staging post by Id")
    @GetMapping(value = "/${api.version}/posts/{postId}", produces = APPLICATION_JSON_VALUE)
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
        StopWatch stopWatch = createStarted();
        // staging posts
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        String eTag = ETagger.computeEtag(stagingPost);
        if (eTag.equals(ifNoneMatch)) {
            return status(NOT_MODIFIED).build(); // TODO: unit test
        }
        String queueIdent = getQueueDefinitionService().resolveQueueIdent(username, stagingPost.getQueueId());
        PostDTO post = PostDTO.from(stagingPost, queueIdent);
        getValidator().validate(post);
        stopWatch.stop();
        logStagingPostFetch(username, stopWatch, 1, 1);
        return ok()
                .eTag(eTag)
                .body(post);
    }

    //
    // RETRIEVE POST INDIVIDUAL FIELDS
    //

    /**
     * Get the queue identifier of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * queue identifier of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the queue identifier from.
     * @param authentication The authentication details of the user making the request.
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        String queueIdent = getQueueDefinitionService().resolveQueueIdent(username, stagingPost.getQueueId());
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "queueId");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueIdent));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueIdent, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
        }
    }

    /**
     * Get the title of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * title of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the title from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the post title.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the title of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post title",
            content = @Content(schema = @Schema(implementation = ContentObject.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/title", produces = APPLICATION_JSON_VALUE)
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        ContentObject postTitle = stagingPost.getPostTitle();
        if (postTitle != null) {
            getValidator().validate(postTitle);
        }
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postTitle");
        return ok(postTitle);
    }

    /**
     * Get the description of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * description of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the description from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the post description.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the description of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post description",
            content = @Content(schema = @Schema(implementation = ContentObject.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/description", produces = APPLICATION_JSON_VALUE)
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        ContentObject postDesc = stagingPost.getPostDesc();
        if (postDesc != null) {
            getValidator().validate(postDesc);
        }
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postDesc");
        return ok(postDesc);
    }

    /**
     * Get the iTunes descriptor of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * iTunes descriptor of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the iTunes metadata from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the post iTunes descriptor.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the iTunes descriptor of a post given by its Id.")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post iTunes descriptor",
            content = @Content(schema = @Schema(implementation = PostITunes.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/itunes", produces = APPLICATION_JSON_VALUE)
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        PostITunes postITunes = stagingPost.getPostITunes();
        if (postITunes != null) {
            getValidator().validate(postITunes);
        }
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postITunes");
        return ok(postITunes);
    }

    /**
     * Get the comment string of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * comment string of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the comment string from.
     * @param authentication The authentication details of the user making the request.
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postComment");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(stagingPost.getPostComment()));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(stagingPost.getPostComment(), headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
        }
    }

    /**
     * Get the rights string of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * rights string of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the rights strings from.
     * @param authentication The authentication details of the user making the request.
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postRights");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(stagingPost.getPostRights()));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(stagingPost.getPostRights(), headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
        }
    }

    /**
     * Get all categories in the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * categories in a specific post given by its Id.
     *
     * @param postId         The Id of the post to fetch categories from.
     * @param offset         The number of items to skip before returning results.
     * @param limit          The maximum number of items to return.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post categories.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all categories in the post given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post categories",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = String.class))))
    @GetMapping(value = "/${api.version}/posts/{postId}/categories", produces = APPLICATION_JSON_VALUE)
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        List<String> postCategories = stagingPost.getPostCategories();
        if (isNotEmpty(postCategories)) {
            postCategories = Paginator.paginate(postCategories, offset, limit);
        }
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, postId, "postCategories");
        return ok(postCategories);
    }

    /**
     * Get the expiration timestamp of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * expiration timestamp of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the expiration timestamp from.
     * @param authentication The authentication details of the user making the request.
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        String responseStr = Optional.ofNullable(stagingPost.getExpirationTimestamp())
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZONE_ID))
                .map(ISO_8601_TIMESTAMP_FORMATTER::format)
                .orElse(null);
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "expirationTimestamp");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
        }
    }

    /**
     * Get the published timestamp of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * published timestamp of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the published timestamp from.
     * @param authentication The authentication details of the user making the request.
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        String responseStr = Optional.ofNullable(stagingPost.getPublishTimestamp())
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZONE_ID))
                .map(ISO_8601_TIMESTAMP_FORMATTER::format)
                .orElse(null);
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "publishTimestamp");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
        }
    }

    /**
     * Get the last updated timestamp of a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * last updated timestamp of a post given by its Id.
     *
     * @param postId         The Id of the post to fetch the last updated tiemstamp from.
     * @param authentication The authentication details of the user making the request.
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
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        String responseStr = Optional.ofNullable(stagingPost.getLastUpdatedTimestamp())
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZONE_ID))
                .map(ISO_8601_TIMESTAMP_FORMATTER::format)
                .orElse(null);
        stopWatch.stop();
        logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "lastUpdatedTimestamp");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
        }
    }

    //
    // UPDATE POST
    //

    /**
     * Update a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update any
     * attribute of a post given by its Id.
     *
     * @param postId            The Id of the post to update.
     * @param postUpdateRequest The request containing the updated post.
     * @param httpMethod        The HTTP method in use, either PATCH or PUT.
     * @param authentication    The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostConfigResponse.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePost(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody PostConfigRequest postUpdateRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePost for user={}, postId={}, postUpdateRequest={}, httpMethod={}", username, postId, postUpdateRequest, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePost(username, postId, postUpdateRequest, isPatch(httpMethod));
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostUpdate(username, stopWatch, postId);
        return ok(postConfigResponse);
    }

    //
    // UPDATE POST INDIVIDUAL FIELDS
    //

    /**
     * Update the title of the post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * title of a post given by its Id.
     *
     * @param postId                 The Id of the post to update.
     * @param postTitleUpdateRequest The request containing the updated post title.
     * @param httpMethod             The HTTP method in use, either PATCH or PUT.
     * @param authentication         The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the title of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post title",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostConfigResponse.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/title", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostTitle(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody ContentObject postTitleUpdateRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostTitle for user={}, postId={}, postTitleUpdateRequest={}, httpMethod={}", username, postId, postTitleUpdateRequest, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostTitle(username, postId, postTitleUpdateRequest, isPatch(httpMethod));
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeUpdate(username, stopWatch, postId, "postTitle");
        return ok(postConfigResponse);
    }

    /**
     * Update the description of the post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * description of a post given by its Id.
     *
     * @param postId                The Id of the post to update.
     * @param postDescUpdateRequest The request containing the updated post description.
     * @param httpMethod            The HTTP method in use, either PATCH or PUT.
     * @param authentication        The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the description of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post description",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostConfigResponse.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/description", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostDescription(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody ContentObject postDescUpdateRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostDesc for user={}, postId={}, postDescUpdateRequest={}, httpMethod={}", username, postId, postDescUpdateRequest, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostDesc(username, postId, postDescUpdateRequest, isPatch(httpMethod));
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeUpdate(username, stopWatch, postId, "postDesc");
        return ok(postConfigResponse);
    }

    /**
     * Update the iTunes descriptor of the post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * iTunes descriptor of a post given by its Id.
     *
     * @param postId                  The Id of the post to update.
     * @param postITunesUpdateRequest The request containing the updated post iTunes descriptor.
     * @param httpMethod              The HTTP method in use, either PATCH or PUT.
     * @param authentication          The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the iTunes descriptor of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post iTunes descriptor",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostConfigResponse.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/itunes", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostITunes(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody PostITunes postITunesUpdateRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostITunes for user={}, postId={}, postITunesUpdateRequest={}, httpMethod={}", username, postId, postITunesUpdateRequest, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostITunes(username, postId, postITunesUpdateRequest, isPatch(httpMethod));
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeUpdate(username, stopWatch, postId, "postITunes");
        return ok(postConfigResponse);
    }

    /**
     * Update the comment string of the post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * comment string of a post given by its Id.
     *
     * @param postId         The Id of the post to update.
     * @param postComment    The updated post comment string.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the comment string of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post comment string",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostConfigResponse.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/comment", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostComment(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            @RequestHeader("Content-Type") String contentTypeHeader,
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody String postComment,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostComment for user={}, postId={}, postComment={}, httpMethod={}", username, postId, postComment, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostComment(username, postId,
                contentTypeHeader.contains(APPLICATION_JSON_VALUE) ? GSON.fromJson(postComment, String.class) : postComment);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeUpdate(username, stopWatch, postId, "postComment");
        return ok(postConfigResponse);
    }

    /**
     * Update the rights string of the post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * rights string of a post given by its Id.
     *
     * @param postId         The Id of the post to update.
     * @param postRights     The updated post rights string.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the rights string of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post rights string",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostConfigResponse.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/rights", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostRights(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            @RequestHeader("Content-Type") String contentTypeHeader,
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody String postRights,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostRights for user={}, postId={}, postRights={}, httpMethod={}", username, postId, postRights, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostRights(username, postId,
                contentTypeHeader.contains(APPLICATION_JSON_VALUE) ? GSON.fromJson(postRights, String.class) : postRights);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeUpdate(username, stopWatch, postId, "postRights");
        return ok(postConfigResponse);
    }

    /**
     * Update the categories of the post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * categories of a post given by its Id.
     *
     * @param postId         The Id of the post to update.
     * @param postCategories The updated post categories.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the categories of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post rights string",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostConfigResponse.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/categories", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updatePostCategories(
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody List<String> postCategories,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostCategories for user={}, postId={}, postCategories={}, httpMethod={}", username, postId, postCategories, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = getStagingPostService().updatePostCategories(username, postId, postCategories);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeUpdate(username, stopWatch, postId, "postCategories");
        return ok(postConfigResponse);
    }

    /**
     * Update the expiration timestamp of the post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * expiration timestamp of a post given by its Id.
     *
     * @param postId              The Id of the post to update.
     * @param expirationTimestamp The updated post expiration timestamp.
     * @param httpMethod          The HTTP method in use, either PATCH or PUT.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the expiration timestamp of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post expiration timestamp",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostConfigResponse.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/expiration", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> updateExpirationTimestamp(
            //
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody String expirationTimestamp,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateExpirationTimestamp for user={}, postId={}, expirationTimestamp={}, httpMethod={}", username, postId, expirationTimestamp, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost;
        try {
            updatedPost = getStagingPostService().updateExpirationTimestamp(username, postId,
                    Date.from(Instant.from(ISO_8601_TIMESTAMP_FORMATTER.parse(GSON.fromJson(expirationTimestamp, String.class)))));
        } catch (DateTimeParseException e) {
            return badRequest().build();
        }
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeUpdate(username, stopWatch, postId, "expirationTimestamp");
        return ok(postConfigResponse);
    }

    //
    // DELETE POST
    //

    /**
     * Delete a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * a post given by its Id.
     *
     * @param postId         The Id of the post to delete.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post")
    @DeleteMapping(value = "/${api.version}/posts/{postId}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostDeleteResponse> deletePost(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePost for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = getStagingPostService().findById(username, postId);
        Map<String, PubResult> pubResults = null;
        if (stagingPost.isPublished()) {
            StagingPost updatedPost = getStagingPostService().updatePostPubStatus(username, postId, DEPUB_PENDING);
            long queueId = getStagingPostService().resolveQueueId(username, postId);
            pubResults = getPostPublisher().publishFeed(
                    username,
                    queueId,
                    singletonList(updatedPost)
            );
        }
        getStagingPostService().deleteById(username, postId);
        PostDeleteResponse postDeleteResponse = PostDeleteResponse.from("Deleted post Id " + postId, pubResults);
        getValidator().validate(postDeleteResponse);
        stopWatch.stop();
        logStagingPostDelete(username, stopWatch, postId);
        return ok().body(postDeleteResponse);
    }

    //
    // DELETE POST INDIVIDUAL FIELDS
    //

    /**
     * Delete the iTunes descriptor for a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the iTunes descriptor from a post given by its Id.
     *
     * @param postId         The Id of the post to delete the iTunes descriptor from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the iTunes descriptor from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post iTunes descriptor")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/itunes", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostITunes(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the iTunes descriptor to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostITunes for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        getStagingPostService().clearPostITunes(username, postId);
        StagingPost updatedPost = getStagingPostService().findById(username, postId);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeDelete(username, stopWatch, postId, "postITunes");
        return ok().body(postConfigResponse);
    }

    /**
     * Delete the comment string for a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the comment string from a post given by its Id.
     *
     * @param postId         The Id of the post to delete the comment string from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the comment string from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post comment string")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/comment", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostComment(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the comment string to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostComment for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        getStagingPostService().clearPostComment(username, postId);
        StagingPost updatedPost = getStagingPostService().findById(username, postId);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeDelete(username, stopWatch, postId, "postComment");
        return ok().body(postConfigResponse);
    }

    /**
     * Delete the rights string for a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the rights string from a post given by its Id.
     *
     * @param postId         The Id of the post to delete the rights string from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the rights string from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post rights string")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/rights", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostRights(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the rights string to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostRights for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        getStagingPostService().clearPostRights(username, postId);
        StagingPost updatedPost = getStagingPostService().findById(username, postId);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeDelete(username, stopWatch, postId, "postRights");
        return ok().body(postConfigResponse);
    }

    /**
     * Delete the categories for a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the categories from a post given by its Id.
     *
     * @param postId         The Id of the post to delete the categories from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the categories from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post categories")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/categories", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deletePostCategories(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the rights string to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostCategories for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        getStagingPostService().clearPostCategories(username, postId);
        StagingPost updatedPost = getStagingPostService().findById(username, postId);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeDelete(username, stopWatch, postId, "categories");
        return ok().body(postConfigResponse);
    }

    /**
     * Delete the expiration timestamp for a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the expiration timestamp from a post given by its Id.
     *
     * @param postId         The Id of the post to delete the expiration timestamp from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the expiration timestamp from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post expiration timestamp")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/expiration", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostConfigResponse> deleteExpirationTimestamp(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post with the expiration timestamp to delete", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteExpirationTimestamp for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        getStagingPostService().clearExpirationTimestamp(username, postId);
        StagingPost updatedPost = getStagingPostService().findById(username, postId);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        stopWatch.stop();
        logStagingPostAttributeDelete(username, stopWatch, postId, "expirationTimestamp");
        return ok().body(postConfigResponse);
    }
}
