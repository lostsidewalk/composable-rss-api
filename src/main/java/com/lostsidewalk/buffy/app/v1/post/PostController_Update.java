package com.lostsidewalk.buffy.app.v1.post;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
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

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Slf4j
@RestController
@Validated
public class PostController_Update extends PostController {

    //
    // UPDATE POST
    //

    /**
     * Update a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update any
     * attribute of a post given by its Id.
     *
     * @param postId                      The Id of the post to update.
     * @param postUpdateRequest           The request containing the updated post.
     * @param httpMethod                  The HTTP method in use, either PATCH or PUT.
     * @param authentication              The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostDTO.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostDTO> updatePost(
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
        StagingPost updatedPost = stagingPostService.updatePost(username, postId, postUpdateRequest, isPatch(httpMethod));
        String queueIdent = queueDefinitionService.findByQueueId(username, updatedPost.getQueueId()).getIdent();
        PostDTO postUpdatedResponse = preparePostDTO(updatedPost, queueIdent);
        validator.validate(postUpdatedResponse);
        stopWatch.stop();
        appLogService.logStagingPostUpdate(username, stopWatch, postId);
        return ok(postUpdatedResponse);
    }

    //
    // UPDATE POST INDIVIDUAL FIELDS
    //

    /**
     * Update the publication status of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * publication status of a post given by its Id. The status can be 'PUB_PENDING',
     * 'DEPUB_PENDING', or null.
     *
     * @param postId                            The Id of the post to update.
     * @param postStatusUpdateRequest           The request containing the updated post status.
     * @param httpMethod                        The HTTP method in use, either PATCH or PUT.
     * @param authentication                    The authentication details of the user making the request.
     * @return A ResponseEntity containing a list of DeployResponse objects indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the publication status of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post publication status",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = DeployResponse.class))))
    @RequestMapping(value = "/${api.version}/posts/{postId}/status", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<Map<String, DeployResponse>> updatePostStatus(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated post status", required = true)
            PostStatusUpdateRequest postStatusUpdateRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}, httpMethod={}", username, postId, postStatusUpdateRequest, httpMethod);
        StopWatch stopWatch = createStarted();
        StagingPost.PostPubStatus newStatus = null;
        String newStatusStr = postStatusUpdateRequest.getNewStatus();
        if (isNotBlank(newStatusStr)) {
            newStatus = StagingPost.PostPubStatus.valueOf(newStatusStr);
        }
        Map<String, PubResult> publicationResults = stagingPostService.updatePostPubStatus(username, postId, newStatus);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(publicationResults);
        validator.validate(deployResponses);
        stopWatch.stop();
        appLogService.logStagingPostPubStatusUpdate(username, stopWatch, postId, postStatusUpdateRequest, publicationResults);
        return ok(deployResponses);
    }

    /**
     * Update the title of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * title of a post given by its Id.
     *
     * @param postId                      The Id of the post to update.
     * @param postTitleUpdateRequest      The request containing the updated post title.
     * @param httpMethod                  The HTTP method in use, either PATCH or PUT.
     * @param authentication              The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the title of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post title",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContentObject.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/title", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ContentObject> updatePostTitle(
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
        ContentObject updatedPostTitle = stagingPostService.updatePostTitle(username, postId, postTitleUpdateRequest, isPatch(httpMethod));
        if (updatedPostTitle != null) {
            validator.validate(updatedPostTitle);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postTitle");
        return ok(updatedPostTitle);
    }

    /**
     * Update the description of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * description of a post given by its Id.
     *
     * @param postId                      The Id of the post to update.
     * @param postDescUpdateRequest       The request containing the updated post description.
     * @param httpMethod                  The HTTP method in use, either PATCH or PUT.
     * @param authentication              The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the description of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post description",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ContentObject.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/description", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ContentObject> updatePostDescription(
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
        ContentObject updatedPostDesc = stagingPostService.updatePostDesc(username, postId, postDescUpdateRequest, isPatch(httpMethod));
        if (updatedPostDesc != null) {
            validator.validate(updatedPostDesc);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postDesc");
        return ok(updatedPostDesc);
    }

    /**
     * Update the iTunes descriptor of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * iTunes descriptor of a post given by its Id.
     *
     * @param postId                      The Id of the post to update.
     * @param postITunesUpdateRequest     The request containing the updated post iTunes descriptor.
     * @param httpMethod                  The HTTP method in use, either PATCH or PUT.
     * @param authentication              The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the iTunes descriptor of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post iTunes descriptor",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PostITunes.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/itunes", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostITunes> updatePostITunes(
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
        PostITunes updatedPostITunes = stagingPostService.updatePostITunes(username, postId, postITunesUpdateRequest, isPatch(httpMethod));
        if (updatedPostITunes != null) {
            validator.validate(updatedPostITunes);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postITunes");
        return ok(updatedPostITunes);
    }

    /**
     * Update the comment string of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * comment string of a post given by its Id.
     *
     * @param postId                      The Id of the post to update.
     * @param postComment                 The updated post comment string.
     * @param httpMethod                  The HTTP method in use, either PATCH or PUT.
     * @param authentication              The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the comment string of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post comment string",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/comment", method = {PUT, PATCH}, produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<String> updatePostComment(
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
        String updatedPostComment = stagingPostService.updatePostComment(username, postId,
                contentTypeHeader.contains(APPLICATION_JSON_VALUE) ? GSON.fromJson(postComment, String.class ) : postComment);
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postComment");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(updatedPostComment));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(updatedPostComment, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Update the rights string of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * rights string of a post given by its Id.
     *
     * @param postId                      The Id of the post to update.
     * @param postRights                  The updated post rights string.
     * @param httpMethod                  The HTTP method in use, either PATCH or PUT.
     * @param authentication              The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the rights string of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post rights string",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/rights", method = {PUT, PATCH}, produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<String> updatePostRights(
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
        String updatedPostRights = stagingPostService.updatePostRights(username, postId,
                contentTypeHeader.contains(APPLICATION_JSON_VALUE) ? GSON.fromJson(postRights, String.class ) : postRights);
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postRights");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(updatedPostRights));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(updatedPostRights, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Update the categories of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * categories of a post given by its Id.
     *
     * @param postId                      The Id of the post to update.
     * @param postCategories              The updated post categories.
     * @param httpMethod                  The HTTP method in use, either PATCH or PUT.
     * @param authentication              The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the categories of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post rights string",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/categories", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<String> updatePostCategories(
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
        List<String> updatedPostCategories = stagingPostService.updatePostCategories(username, postId, postCategories);
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postCategories");
        return ok(GSON.toJson(updatedPostCategories));
    }

    /**
     * Update the expiration timestamp of the post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * expiration timestamp of a post given by its Id.
     *
     * @param postId                      The Id of the post to update.
     * @param expirationTimestamp         The updated post expiration timestamp.
     * @param httpMethod                  The HTTP method in use, either PATCH or PUT.
     * @param authentication              The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the expiration timestamp of the post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post expiration timestamp",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = String.class)))
    @RequestMapping(value = "/${api.version}/posts/{postId}/expiration", method = {PUT, PATCH}, produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<String> updateExpirationTimestamp(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
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
        Date updatedExpirationTimestamp;
        try {
            updatedExpirationTimestamp = stagingPostService.updateExpirationTimestamp(username, postId, ISO_8601_TIMESTAMP_FORMAT.parse(expirationTimestamp));
        } catch (ParseException e) {
            return badRequest().build();
        }
        String responseStr = ISO_8601_TIMESTAMP_FORMAT.format(updatedExpirationTimestamp);
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "expirationTimestamp");
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
