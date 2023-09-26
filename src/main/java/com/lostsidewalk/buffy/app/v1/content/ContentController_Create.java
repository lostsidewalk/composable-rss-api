package com.lostsidewalk.buffy.app.v1.content;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.ContentObjectConfigRequest;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import com.lostsidewalk.buffy.post.ContentObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;

/**
 * Controller class for creating and adding new content to a post.
 */
@Slf4j
@RestController
@Validated
public class ContentController_Create extends ContentController {

    /**
     * Add new content to the post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new content
     * to a specific post given by its Id.
     *
     * @param postId                   The Id of the post into which the new content should be added.
     * @param postContentConfigRequest A ContentObjectConfigRequest, representing new content to add.
     * @param authentication           The authentication details of the user making the request.
     * @return a ResponseEntity containing the created content
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add new content to the post given by its Id")
    @ApiResponse(responseCode = "201", description = "Successfully add content to post")
    @PostMapping(value = "/${api.version}/posts/{postId}/content", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostContent(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add content to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A content object", required = true,
                    schema = @Schema(implementation = ContentObject.class))
            @Valid ContentObjectConfigRequest postContentConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostContent adding content for for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        String contentIdent = stagingPostService.addContent(username, postId, postContentConfigRequest);
        URI createdLocation = URI.create("/posts/" + postId + "/content/" + contentIdent);
        stopWatch.stop();
        appLogService.logStagingPostAddContent(username, stopWatch, postId);
        return created(createdLocation).body(buildResponseMessage("Added content '" + contentIdent + "' to post Id " + postId));
    }
}
