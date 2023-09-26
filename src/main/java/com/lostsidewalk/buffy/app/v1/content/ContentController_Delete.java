package com.lostsidewalk.buffy.app.v1.content;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for deleting content from a post.
 */
@Slf4j
@RestController
@Validated
class ContentController_Delete extends ContentController {

    //
    // DELETE POST CONTENT
    //

    /**
     * Delete all post contents on a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * post content on a post given by its Id.
     *
     * @param postId         The Id of the post from which the content will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all post contents on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post contents")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/content", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostContents(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete content from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContents for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostContents(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostContentsDelete(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Deleted contents from post Id " + postId));
    }

    /**
     * Delete a post content object given by its identifier on a post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * post content object given by its identifier, on a post given by its Id.
     *
     * @param postId         The Id of the post from which the content object will be removed.
     * @param contentIdent   The identifier of the content object to delete.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the post content given by Ident, on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post content")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/content/{contentIdent}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete content from", required = true)
            Long postId,
            //
            @PathVariable("contentIdent")
            @Parameter(description = "The identifier of the content to delete", required = true)
            String contentIdent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContent for user={}, postId={}, contentIdent={}", username, postId, contentIdent);
        StopWatch stopWatch = createStarted();
        stagingPostService.deleteContent(username, postId, contentIdent);
        stopWatch.stop();
        appLogService.logStagingPostContentDelete(username, stopWatch, postId, contentIdent);
        return ok().body(buildResponseMessage("Deleted content '" + contentIdent + "' from post Id " + postId));
    }
}
