package com.lostsidewalk.buffy.app.v1.author;

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
public class AuthorController_Delete extends AuthorController {

    /**
     * Delete all authors from a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * authors from a post given by its Id.
     *
     * @param postId         The Id of the post from which authors will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all authors from a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post authors")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/authors", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostAuthors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete authors from", required = true)
            Long postId,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostAuthors for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.deletePostAuthors(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostAuthorsDelete(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Deleted authors from post Id " + postId));
    }

    /**
     * Delete an author from a post given its Id and author identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete an
     * author from a post given by its Id and the author's identifier.
     *
     * @param postId         The Id of the post from which the author will be removed.
     * @param authorIdent    The identifier of the author to delete.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete an author from a post by Id and author ident")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post author")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/authors/{authorIdent}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostAuthor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete the author from", required = true)
            Long postId,
            @PathVariable("authorIdent")
            @Parameter(description = "The identifier of the author to delete", required = true)
            String authorIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostAuthor for user={}, postId={}, authorIdent={}", username, postId, authorIdent);
        StopWatch stopWatch = createStarted();
        stagingPostService.deleteAuthor(username, postId, authorIdent);
        stopWatch.stop();
        appLogService.logStagingPostAuthorDelete(username, stopWatch, postId, authorIdent);
        return ok().body(buildResponseMessage("Deleted author '" + authorIdent + "' from post Id " + postId));
    }
}
