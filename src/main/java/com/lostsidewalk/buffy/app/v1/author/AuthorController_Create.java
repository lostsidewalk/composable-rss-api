package com.lostsidewalk.buffy.app.v1.author;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostPersonConfigRequest;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
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

@Slf4j
@RestController
@Validated
public class AuthorController_Create extends AuthorController {

    /**
     * Add a new author to a specific post given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add a new author
     * to a specific post given by its Id.
     *
     * @param postId                  The Id of the post to which the new author should be added.
     * @param postAuthorConfigRequest A PostPersonConfigRequest object representing the new author to add.
     * @param authentication          The authentication details of the user making the request.
     * @return a ResponseEntity containing the created author
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add a new author to a post by its Id")
    @ApiResponse(responseCode = "201", description = "Successfully added the author to the post")
    @PostMapping(value = "/${api.version}/posts/{postId}/authors", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add the author to", required = true)
            Long postId,
            @RequestBody
            @Parameter(description = "An author object", required = true,
                    schema = @Schema(implementation = PostPersonConfigRequest.class))
            @Valid PostPersonConfigRequest postAuthorConfigRequest,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostAuthor adding author for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        String authorIdent = stagingPostService.addAuthor(username, postId, postAuthorConfigRequest);
        URI createdLocation = URI.create("/posts/" + postId + "/authors/" + authorIdent);
        stopWatch.stop();
        appLogService.logStagingPostAddAuthor(username, stopWatch, postId);
        return created(createdLocation)
                .body(buildResponseMessage("Added author '" + authorIdent + "' to post Id " + postId));
    }
}
