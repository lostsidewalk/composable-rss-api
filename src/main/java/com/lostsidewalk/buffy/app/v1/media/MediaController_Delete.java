package com.lostsidewalk.buffy.app.v1.media;

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
public class MediaController_Delete extends MediaController {

    //
    // DELETE POST MEDIA
    //

    /**
     * Delete a post media object from a post given by Id,
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a post media object on a post given by its Id.
     *
     * @param postId               The Id of the post from which the media will be removed.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a post media object from a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post media")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/media", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete media from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostMedia for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.clearPostMedia(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAttributeDelete(username, stopWatch, postId, "postMedia");
        return ok().body(buildResponseMessage("Deleted media from post Id " + postId));
    }
}
