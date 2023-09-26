package com.lostsidewalk.buffy.app.v1.url;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostUrlConfigRequest;
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
public class URLController_Create extends URLController {

    //
    // CREATE POST URL
    //

    /**
     * Add a new URL to the post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add a new URLs
     * to a specific post given by its Id.
     *
     * @param postId                       The Id of the post into which the new URL should be added.
     * @param postUrlConfigRequest         A PostUrlConfigRequest object, representing the new URL to add.
     * @param authentication               The authentication details of the user making the request.
     * @return a ResponseEntity containing the created URL
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add a new URL to the post given by its Id")
    @ApiResponse(responseCode = "201", description = "Successfully added URL to post")
    @PostMapping(value = "/${api.version}/posts/{postId}/urls", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addPostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to add URL to", required = true)
            Long postId,
            //
            @RequestBody
            @Parameter(description = "A URL object", required = true,
                    schema = @Schema(implementation = PostUrlConfigRequest.class))
            @Valid PostUrlConfigRequest postUrlConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addPostUrl adding URL for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        String urlIdent = stagingPostService.addPostUrl(username, postId, postUrlConfigRequest);
        URI createdLocation = URI.create("/posts/" + postId + "/urls/" + urlIdent);
        stopWatch.stop();
        appLogService.logStagingPostAddUrl(username, stopWatch, postId);
        return created(createdLocation).body(buildResponseMessage("Added URL '" + urlIdent + "' to post Id " + postId));
    }
}
