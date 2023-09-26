package com.lostsidewalk.buffy.app.v1.content;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.ContentObjectConfigRequest;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller class for updating content on a post.
 */
@Slf4j
@RestController
@Validated
class ContentController_Update extends ContentController {

    //
    // UPDATE POST CONTENT
    //

    /**
     * Update all post content on a post a given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post content on a post given by its Id.
     *
     * @param postId                    The Id of the post to update.
     * @param postContentConfigRequests A list of ContentObjectConfigRequest objects representing the content of the post.
     * @param httpMethod                The HTTP method in use, either PATCH or PUT.
     * @param authentication            The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post contents on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post contents")
    @RequestMapping(value = "/${api.version}/posts/{postId}/content", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostContents(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody List<ContentObjectConfigRequest> postContentConfigRequests,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        //
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContents for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updateContents(username, postId, postContentConfigRequests, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostContentsUpdate(username, stopWatch, postId, size(postContentConfigRequests));
        return ok().body(buildResponseMessage("Updated contents on post Id " + postId));
    }

    /**
     * Update a post content object given by its identifier on a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post content object given by its identifier, on a post given by its Id.
     *
     * @param postId                   The Id of the post to update.
     * @param contentIdent             The identifier of the content object to update.
     * @param postContentConfigRequest A ContentObjectConfigRequest, representing new content to update.
     * @param httpMethod               The HTTP method in use, either PATCH or PUT.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post content given by Ident, on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post content")
    @RequestMapping(value = "/${api.version}/posts/{postId}/content/{contentIdent}", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("contentIdent")
            @Parameter(description = "The identifier of the content to update", required = true)
            String contentIdent,
            //
            @Valid @RequestBody ContentObjectConfigRequest postContentConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        //
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContent for user={}, postId={}, contentIdent={}, httpMethod={}", username, postId, contentIdent, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updateContent(username, postId, contentIdent, postContentConfigRequest, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostContentUpdate(username, stopWatch, postId, contentIdent);
        return ok().body(buildResponseMessage("Updated content '" + contentIdent + "' on post Id " + postId));
    }
}
