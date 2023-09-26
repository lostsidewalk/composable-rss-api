package com.lostsidewalk.buffy.app.v1.url;

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

@Slf4j
@RestController
@Validated
public class URLController_Delete extends URLController {

    //
    // DELETE POST URL
    //

    /**
     * Delete all post URLs on a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * post URLs on a post given by its Id.
     *
     * @param postId               The Id of the post from which the URLs will be removed.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all post URLs on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post URLs")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/urls", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostUrls(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete URLs from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostUrls for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostUrls(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostUrlsDelete(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Deleted URLs from post Id " + postId));
    }

    /**
     * Delete a post URL given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * post URL given by its identifier, on a post given by its Id.
     *
     * @param postId               The Id of the post from which the URL will be removed.
     * @param urlIdent               The identifier of the URL to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a post URL by its identifier on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post URL")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/urls/{urlIdent}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete a URL from", required = true)
            Long postId,
            //
            @PathVariable("urlIdent")
            @Parameter(description = "The identifier of the URL to delete", required = true)
            String urlIdent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostUrl for user={}, postId={}, urlIdent={}", username, postId, urlIdent);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostUrl(username, postId, urlIdent);
        stopWatch.stop();
        appLogService.logStagingPostUrlDelete(username, stopWatch, postId, urlIdent);
        return ok().body(buildResponseMessage("Deleted URL '" + urlIdent + "' from post Id " + postId));
    }
}
