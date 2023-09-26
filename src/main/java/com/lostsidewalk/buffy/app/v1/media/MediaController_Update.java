package com.lostsidewalk.buffy.app.v1.media;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import com.lostsidewalk.buffy.post.PostMedia;
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

import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Slf4j
@RestController
@Validated
public class MediaController_Update extends MediaController {

    //
    // UPDATE POST MEDIA
    //

    /**
     * Update a post media object on a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post media object on a post given by its Id.
     *
     * @param postId              The Id of the post to update.
     * @param postMedia           A PostMedia object, representing new media to update.
     * @param httpMethod          The HTTP method in use, either PATCH or PUT.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the post media object on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post media")
    @RequestMapping(value = "/${api.version}/posts/{postId}/media", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody PostMedia postMedia,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostMedia for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostMedia(username, postId, postMedia, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostAttributeUpdate(username, stopWatch, postId, "postMedia");
        return ok().body(buildResponseMessage("Updated media on post Id " + postId));
    }
}
