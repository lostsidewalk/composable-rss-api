package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.PostConfigRequest;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.response.PostDTO;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostITunes;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for managing post-related operations.
 *
 * This controller provides endpoints for managing posts within queues. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class PostController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    Validator validator;

    //
    // CREATE POST
    //

    /**
     * Create new posts in the queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to create new posts
     * in a specific queue identified by its Id.
     *
     * @param queueId                The Id of the queue into which the new posts should be added.
     * @param postConfigRequests     A list of PostConfigRequests, representing new posts to add.
     * @param authentication         The authentication details of the user making the request.
     * @return a ResponseEntity containing the created posts
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Create new posts in the queue given by queueId", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully created posts",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostDTO.class))))
    @PostMapping(value = "/posts/{queueId}", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostDTO>> createPosts(
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to create posts in", required = true)
            Long queueId,
            //
            @RequestBody
            @Parameter(description = "List of post configuration requests", required = true,
                    schema = @Schema(implementation = PostConfigRequest.class))
            List<@Valid PostConfigRequest> postConfigRequests,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createPosts adding {} posts for for user={}, queueId={}", size(postConfigRequests), username, queueId);
        StopWatch stopWatch = createStarted();
        List<PostDTO> createdPosts = new ArrayList<>();
        for (PostConfigRequest postConfigRequest : postConfigRequests) {
            Long postId = stagingPostService.createPost(username, queueId, postConfigRequest);
            StagingPost stagingPost = stagingPostService.findById(username, postId);
            createdPosts.add(convertToDTO(stagingPost));
        }
        stopWatch.stop();
        validator.validate(createdPosts);
        appLogService.logStagingPostCreate(username, stopWatch, postConfigRequests.size(), size(createdPosts));
        return ok(createdPosts);
    }

    //
    // RETRIEVE POSTS
    //

    /**
     * Get all posts in the queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * posts in a specific queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch posts from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched posts.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all posts in the queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched posts",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostDTO.class))))
    @GetMapping(value = "/posts/{queueId}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostDTO>> getPosts(
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch posts from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPosts for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        List<StagingPost> stagingPosts = stagingPostService.getStagingPosts(username, singletonList(queueId));
        stopWatch.stop();
        List<PostDTO> posts = stagingPosts.stream().map(PostController::convertToDTO).toList();
        validator.validate(posts);
        appLogService.logStagingPostFetch(username, stopWatch, 1, size(stagingPosts));
        return ok(posts);
    }

    private static PostDTO convertToDTO(StagingPost stagingPost) {
        return PostDTO.from(stagingPost.getId(),
                stagingPost.getQueueId(),
                stagingPost.getPostTitle(),
                stagingPost.getPostDesc(),
                stagingPost.getPostContents(),
                stagingPost.getPostITunes(),
                stagingPost.getPostUrl(),
                stagingPost.getPostUrls(),
                stagingPost.getPostComment(),
                stagingPost.getPostRights(),
                stagingPost.getContributors(),
                stagingPost.getAuthors(),
                stagingPost.getPostCategories(),
                stagingPost.getPublishTimestamp(),
                stagingPost.getExpirationTimestamp(),
                stagingPost.getEnclosures(),
                stagingPost.getLastUpdatedTimestamp(),
                stagingPost.isPublished());
    }

    //
    // RETRIEVE POST INDIVIDUAL FIELDS
    //

    /**
     * Get the status of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the status of a
     * posts identified by its Id.  The status of a post is given by the PostPubStatus enumeration.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post status.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the status of a post given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post status",
            content = @Content(schema = @Schema(implementation = PostPubStatus.class)))
    @GetMapping(value = "/posts/{postId}/status", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostPubStatus> getPostStatus(
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
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "status");
        return ok(stagingPost.getPostPubStatus());
    }

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    /**
     * Get the queue Id of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * queue Id of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post queue Id.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the queue Id of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post queue Id",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/posts/{postId}/queue", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPostQueueId(
            @RequestHeader("Accept") String acceptHeader,
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
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "queueId");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(stagingPost.getQueueId()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(stagingPost.getQueueId().toString(), headers, OK);
        }
    }

    /**
     * Get the title of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * title of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post title.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the title of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post title",
            content = @Content(schema = @Schema(implementation = ContentObject.class)))
    @GetMapping(value = "/posts/{postId}/title", produces = {APPLICATION_JSON_VALUE})
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
        stopWatch.stop();
        ContentObject postTitle = stagingPost.getPostTitle();
        validator.validate(postTitle);
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postTitle");
        return ok(stagingPost.getPostTitle());
    }

    /**
     * Get the description of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * description of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post description.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the description of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post description",
            content = @Content(schema = @Schema(implementation = ContentObject.class)))
    @GetMapping(value = "/posts/{postId}/desc", produces = {APPLICATION_JSON_VALUE})
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
        stopWatch.stop();
        ContentObject postDesc = stagingPost.getPostDesc();
        validator.validate(postDesc);
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postDesc");
        return ok(stagingPost.getPostDesc());
    }

    /**
     * Get the iTunes descriptor of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * iTunes descriptor of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post iTunes descriptor.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the iTunes descriptor of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post iTunes descriptor",
            content = @Content(schema = @Schema(implementation = PostITunes.class)))
    @GetMapping(value = "/posts/{postId}/itunes", produces = {APPLICATION_JSON_VALUE})
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
        stopWatch.stop();
        PostITunes postITunes = stagingPost.getPostITunes();
        validator.validate(postITunes);
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "postITunes");
        return ok(postITunes);
    }

    /**
     * Get the comment string of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * comment string of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post comment string.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the comment string of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post comment string",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/posts/{postId}/comment", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPostComment(
            @RequestHeader("Accept") String acceptHeader,
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
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(stagingPost.getPostComment()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(stagingPost.getPostComment(), headers, OK);
        }
    }

    /**
     * Get the rights string of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * rights string of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post rights string.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the rights string of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post rights string",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/posts/{postId}/rights", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPostRights(
            @RequestHeader("Accept") String acceptHeader,
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
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(stagingPost.getPostRights()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(stagingPost.getPostRights(), headers, OK);
        }
    }

    private static final SimpleDateFormat ISO_8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    /**
     * Get the expiration timestamp of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * expiration timestamp of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post expiration timestamp.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the expiration timestamp of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post expiration timestamp",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/posts/{postId}/expiration", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getExpirationTimestamp(
            @RequestHeader("Accept") String acceptHeader,
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
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "expirationTimestamp");
        Date expirationTimestamp = stagingPost.getExpirationTimestamp();
        String responseStr = expirationTimestamp != null ?
                ISO_8601_TIMESTAMP_FORMAT.format(expirationTimestamp) :
                EMPTY; // default response
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        }
    }

    /**
     * Get the published timestamp of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * published timestamp of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post published timestamp.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the published timestamp of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post published timestamp",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/posts/{postId}/published", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getPublishedTimestamp(
            @RequestHeader("Accept") String acceptHeader,
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
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "publishTimestamp");
        Date publishTimestamp = stagingPost.getPublishTimestamp();
        String responseStr = publishTimestamp != null ?
                ISO_8601_TIMESTAMP_FORMAT.format(publishTimestamp) :
                EMPTY; // default response
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        }
    }

    /**
     * Get the last updated timestamp of a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * last updated timestamp of a post identified by its Id.
     *
     * @param postId          The Id of the post to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the post last updated timestamp.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the last updated timestamp of a post given by its Id.", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched post last updated timestamp",
            content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping(value = "/posts/{postId}/updated", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getLastUpdatedTimestamp(
            @RequestHeader("Accept") String acceptHeader,
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
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, stagingPost.getId(), "lastUpdatedTimestamp");
        Date lastUpdatedTimestamp = stagingPost.getLastUpdatedTimestamp();
        String responseStr = lastUpdatedTimestamp != null ?
                ISO_8601_TIMESTAMP_FORMAT.format(lastUpdatedTimestamp) :
                EMPTY; // default response
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        }
    }

    //
    // UPDATE POST
    //

    /**
     * Update a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update any
     * attribute of a post identified by its Id.
     *
     * @param id                   The Id of the post to update.
     * @param postUpdateRequest    The request containing the updated post.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostDTO.class)))
    @PutMapping(value = "/posts/{id}", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable("id")
            @Parameter(description = "The Id of the post to update", required = true)
            Long id,
            //
            @Valid @RequestBody PostConfigRequest postUpdateRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePost for user={}, postId={}, postUpdateRequest={}", username, id, postUpdateRequest);
        StopWatch stopWatch = createStarted();
        StagingPost updatedPost = stagingPostService.updatePost(username, id, postUpdateRequest);
        stopWatch.stop();
        PostDTO postUpdatedResponse = convertToDTO(updatedPost);
        validator.validate(postUpdatedResponse);
        appLogService.logStagingPostUpdate(username, stopWatch, id);
        return ok(postUpdatedResponse);
    }

    //
    // UPDATE POST INDIVIDUAL FIELDS
    //

    /**
     * Update the publication status of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * publication status of a post identified by its Id. The status can be 'PUB_PENDING',
     * 'DEPUB_PENDING', or null.
     *
     * @param id                                The Id of the post to update.
     * @param postStatusUpdateRequest           The request containing the updated post status.
     * @param authentication                    The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the publication status of the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post publication status",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DeployResponse.class)))
    @PutMapping(value = "/posts/{id}/status", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<DeployResponse> updatePostStatus(
            @PathVariable("id")
            @Parameter(description = "The Id of the post to update", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated post status", required = true)
            PostStatusUpdateRequest postStatusUpdateRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}", username, id, postStatusUpdateRequest);
        StopWatch stopWatch = createStarted();
        PostPubStatus newStatus = null;
        String newStatusStr = postStatusUpdateRequest.getNewStatus();
        if (isNotBlank(newStatusStr)) {
            newStatus = PostPubStatus.valueOf(newStatusStr);
        }
        List<PubResult> publicationResults = stagingPostService.updatePostPubStatus(username, id, newStatus);
        stopWatch.stop();
        DeployResponse deployResponse = DeployResponse.from(publicationResults);
        validator.validate(deployResponse);
        appLogService.logStagingPostPubStatusUpdate(username, stopWatch, id, postStatusUpdateRequest, publicationResults);
        return ok(deployResponse);
    }

    /**
     * Update the title of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * title of a post identified by its Id.
     *
     * @param id                     The Id of the post to update.
     * @param postTitleUpdateRequest The request containing the updated post title.
     * @param authentication         The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the title of the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post title",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContentObject.class)))
    @PutMapping(value = "/posts/{id}/title", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ContentObject> updatePostTitle(
            @PathVariable("id")
            @Parameter(description = "The Id of the post to update", required = true)
            Long id,
            //
            @Valid @RequestBody ContentObject postTitleUpdateRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostTitle for user={}, postId={}, postTitleUpdateRequest={}", username, id, postTitleUpdateRequest);
        StopWatch stopWatch = createStarted();
        ContentObject updatedPostTitle = stagingPostService.updatePostTitle(username, id, postTitleUpdateRequest);
        stopWatch.stop();
        validator.validate(updatedPostTitle);
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, id, "postTitle");
        return ok(updatedPostTitle);
    }

    /**
     * Update the description of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * description of a post identified by its Id.
     *
     * @param id                     The Id of the post to update.
     * @param postDescUpdateRequest  The request containing the updated post description.
     * @param authentication         The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the description of the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post description",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContentObject.class)))
    @PutMapping(value = "/posts/{id}/desc", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ContentObject> updatePostDescription(
            @PathVariable("id")
            @Parameter(description = "The Id of the post to update", required = true)
            Long id,
            //
            @Valid @RequestBody ContentObject postDescUpdateRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostDesc for user={}, postId={}, postDescUpdateRequest={}", username, id, postDescUpdateRequest);
        StopWatch stopWatch = createStarted();
        ContentObject updatedPostDesc = stagingPostService.updatePostDesc(username, id, postDescUpdateRequest);
        stopWatch.stop();
        validator.validate(updatedPostDesc);
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, id, "postDesc");
        return ok(updatedPostDesc);
    }

    /**
     * Update the iTunes descriptor of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * iTunes descriptor of a post identified by its Id.
     *
     * @param id                       The Id of the post to update.
     * @param postITunesUpdateRequest  The request containing the updated post iTunes descriptor.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the iTunes descriptor of the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post iTunes descriptor",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostITunes.class)))
    @PutMapping(value = "/posts/{id}/itunes", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostITunes> updatePostITunes(
            @PathVariable("id")
            @Parameter(description = "The Id of the post to update", required = true)
            Long id,
            //
            @Valid @RequestBody PostITunes postITunesUpdateRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostITunes for user={}, postId={}, postITunesUpdateRequest={}", username, id, postITunesUpdateRequest);
        StopWatch stopWatch = createStarted();
        PostITunes updatedPostITunes = stagingPostService.updatePostITunes(username, id, postITunesUpdateRequest);
        stopWatch.stop();
        validator.validate(updatedPostITunes);
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, id, "postITunes");
        return ok(updatedPostITunes);
    }

    /**
     * Update the comment string of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * comment string of a post identified by its Id.
     *
     * @param id               The Id of the post to update.
     * @param postComment      The updated post comment string.
     * @param authentication   The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the comment string of the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post comment string",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    @PutMapping(value = "/posts/{id}/comment", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<String> updatePostComment(
            @RequestHeader("Accept") String acceptHeader,
            @RequestHeader("Content-Type") String contentTypeHeader,
            //
            @PathVariable("id")
            @Parameter(description = "The Id of the post to update", required = true)
            Long id,
            //
            @Valid @RequestBody String postComment, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostComment for user={}, postId={}, postComment={}", username, id, postComment);
        StopWatch stopWatch = createStarted();
        String updatedPostComment = stagingPostService.updatePostComment(username, id,
                contentTypeHeader.contains(APPLICATION_JSON_VALUE) ? GSON.fromJson(postComment, String.class ) : postComment);
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, id, "postComment");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(updatedPostComment));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(updatedPostComment, headers, OK);
        }
    }

    /**
     * Update the rights string of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * rights string of a post identified by its Id.
     *
     * @param id               The Id of the post to update.
     * @param postRights       The updated post rights string.
     * @param authentication   The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the rights string of the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post rights string",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    @PutMapping(value = "/posts/{id}/rights", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<String> updatePostRights(
            @RequestHeader("Accept") String acceptHeader,
            @RequestHeader("Content-Type") String contentTypeHeader,
            //
            @PathVariable("id")
            @Parameter(description = "The Id of the post to update", required = true)
            Long id,
            //
            @Valid @RequestBody String postRights, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostRights for user={}, postId={}, postRights={}", username, id, postRights);
        StopWatch stopWatch = createStarted();
        String updatedPostRights = stagingPostService.updatePostRights(username, id,
                contentTypeHeader.contains(APPLICATION_JSON_VALUE) ? GSON.fromJson(postRights, String.class ) : postRights);
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, id, "postRights");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(updatedPostRights));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(updatedPostRights, headers, OK);
        }
    }

    /**
     * Update the expiration timestamp of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * expiration timestamp of a post identified by its Id.
     *
     * @param id                        The Id of the post to update.
     * @param expirationTimestamp       The updated post expiration timestamp.
     * @param authentication            The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the expiration timestamp of the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated post expiration timestamp",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    @PutMapping(value = "/posts/{id}/expiration", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<String> updateExpirationTimestamp(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("id")
            @Parameter(description = "The Id of the post to update", required = true)
            Long id,
            //
            @Valid @RequestBody String expirationTimestamp, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateExpirationTimestamp for user={}, postId={}, expirationTimestamp={}", username, id, expirationTimestamp);
        StopWatch stopWatch = createStarted();
        Date updatedExpirationTimestamp;
        try {
            updatedExpirationTimestamp = stagingPostService.updateExpirationTimestamp(username, id, ISO_8601_TIMESTAMP_FORMAT.parse(expirationTimestamp));
        } catch (ParseException e) {
            return new ResponseEntity<>(BAD_REQUEST);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, id, "expirationTimestamp");
        String responseStr = ISO_8601_TIMESTAMP_FORMAT.format(updatedExpirationTimestamp);
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        }
    }

    //
    // DELETE POST
    //

    /**
     * Delete a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * a post identified by its Id.
     *
     * @param id                   The Id of the post to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post")
    @DeleteMapping(value = "/posts/{id}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePost(
            @PathVariable("id")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePost for user={}, postId={}", username, id);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostPubStatus(username, id, DEPUB_PENDING);
        stagingPostService.deletePost(username, id);
        stopWatch.stop();
        appLogService.logStagingPostDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted post Id " + id));
    }

    //
    // DELETE POST INDIVIDUAL FIELDS
    //

    /**
     * Delete the iTunes descriptor for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the iTunes descriptor from a post identified by its Id.
     *
     * @param id                   The Id of the post to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the iTunee descriptor from a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post iTunes descriptor")
    @DeleteMapping(value = "/posts/{id}/itunes", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostITunes(
            @PathVariable("id")
            @Parameter(description = "The Id of the post with the iTunes descriptor to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostITunes for user={}, postId={}", username, id);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostITunes(username, id);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, id, "postITunes");
        return ok().body(buildResponseMessage("Deleted iTunes descriptor from post Id " + id));
    }

    /**
     * Delete the comment string for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the comment string from a post identified by its Id.
     *
     * @param id                   The Id of the post to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the comment string from a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post comment string")
    @DeleteMapping(value = "/posts/{id}/comment", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostComment(
            @PathVariable("id")
            @Parameter(description = "The Id of the post with the comment string to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostComment for user={}, postId={}", username, id);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostComment(username, id);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, id, "postComment");
        return ok().body(buildResponseMessage("Deleted comment string from post Id " + id));
    }

    /**
     * Delete the rights string for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the rights string from a post identified by its Id.
     *
     * @param id                   The Id of the post to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the rights string from a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post rights string")
    @DeleteMapping(value = "/posts/{id}/rights", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostRights(
            @PathVariable("id")
            @Parameter(description = "The Id of the post with the rights string to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostRights for user={}, postId={}", username, id);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostRights(username, id);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, id, "postRights");
        return ok().body(buildResponseMessage("Deleted rights string from post Id " + id));
    }

    /**
     * Delete the expiration timestamp for a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the expiration timestamp from a post identified by its Id.
     *
     * @param id                   The Id of the post to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the expiration timestamp from a post given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted post expiration timestamp")
    @DeleteMapping(value = "/posts/{id}/expiration", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteExpirationTimestamp(
            @PathVariable("id")
            @Parameter(description = "The Id of the post with the expiration timestamp to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteExpirationTimestamp for user={}, postId={}", username, id);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearExpirationTimestamp(username, id);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, id, "expirationTimestamp");
        return ok().body(buildResponseMessage("Deleted expiration timestamp string from post Id " + id));
    }
}
