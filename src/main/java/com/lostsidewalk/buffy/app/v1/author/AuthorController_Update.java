package com.lostsidewalk.buffy.app.v1.author;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostPersonConfigRequest;
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

import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Slf4j
@RestController
@Validated
public class AuthorController_Update extends AuthorController {

    /**
     * Update all post authors on a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post authors on a post given by its Id.
     *
     * @param postId                   The Id of the post to update.
     * @param postAuthorConfigRequests A list of PostPersonConfigRequest objects representing the authors of the post.
     * @param httpMethod               The HTTP method in use, either PATCH or PUT.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update all post authors on a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post authors")
    @RequestMapping(value = "/${api.version}/posts/{postId}/authors", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostAuthors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            @Valid @RequestBody List<PostPersonConfigRequest> postAuthorConfigRequests,
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostAuthors for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updateAuthors(username, postId, postAuthorConfigRequests, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostAuthorsUpdate(username, stopWatch, postId, size(postAuthorConfigRequests));
        return ok().body(buildResponseMessage("Updated authors on post Id " + postId));
    }

    /**
     * Update a post author given by its identifier on a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post author given by its identifier, on a post given by its Id.
     *
     * @param postId                  The Id of the post to update.
     * @param authorIdent             The identifier of the author to update.
     * @param postAuthorConfigRequest A PostPersonConfigRequest object representing the author to update.
     * @param httpMethod              The HTTP method in use, either PATCH or PUT.
     * @param authentication          The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update a post author by identifier on a post by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post author")
    @RequestMapping(value = "/${api.version}/posts/{postId}/authors/{authorIdent}", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            @PathVariable("authorIdent")
            @Parameter(description = "The identifier of the author to update", required = true)
            String authorIdent,
            @Valid @RequestBody PostPersonConfigRequest postAuthorConfigRequest,
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostAuthor for user={}, postId={}, authorIdent={}, httpMethod={}", username, postId, authorIdent, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updateAuthor(username, postId, authorIdent, postAuthorConfigRequest, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostAuthorUpdate(username, stopWatch, postId, authorIdent);
        return ok().body(buildResponseMessage("Updated author '" + authorIdent + "' on post Id " + postId));
    }
}
