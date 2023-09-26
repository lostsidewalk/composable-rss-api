package com.lostsidewalk.buffy.app.v1.media;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.post.PostMedia;
import com.lostsidewalk.buffy.post.StagingPost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class MediaController_Retrieve extends MediaController {

    //
    // RETRIEVE POST MEDIA
    //

    /**
     * Get the media in the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * media in a post given by its Id.
     *
     * @param postId          The Id of the post to fetch media from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched post media.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the media in the post given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched post media",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = PostMedia.class)))
    @GetMapping(value = "/${api.version}/posts/{postId}/media", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostMedia> getPostMedia(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to fetch media from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPostMedias for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        StagingPost stagingPost = stagingPostService.findById(username, postId);
        PostMedia postMedia = stagingPost.getPostMedia();
        if (postMedia != null) {
            validator.validate(postMedia);
        }
        stopWatch.stop();
        appLogService.logStagingPostAttributeFetch(username, stopWatch, postId, "postMedia");
        return ok(postMedia);
    }
}
