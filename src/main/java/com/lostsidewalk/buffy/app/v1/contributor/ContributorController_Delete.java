package com.lostsidewalk.buffy.app.v1.contributor;

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
public class ContributorController_Delete extends ContributorController {

    /**
     * Delete all contributors from a post given its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * contributors from a post given by its Id.
     *
     * @param postId         The Id of the post from which contributors will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all contributors from a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post contributors")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/contributors", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostContributors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete", required = true)
            Long postId,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContributors for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.deleteContributors(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostContributorsDelete(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Deleted contributors from post Id " + postId));
    }

    /**
     * Delete a contributor from a post given its Id and contributor identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * contributor from a post given by its Id and the contributor's identifier.
     *
     * @param postId           The Id of the post from which the contributor will be removed.
     * @param contributorIdent The identifier of the contributor to delete.
     * @param authentication   The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a contributor from a post by Id and the contributor ident")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post contributor")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/contributors/{contributorIdent}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete the contributor from", required = true)
            Long postId,
            @PathVariable("contributorIdent")
            @Parameter(description = "The identifier of the contributor to delete", required = true)
            String contributorIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostContributor for user={}, postId={}, contributorIdent={}", username, postId, contributorIdent);
        StopWatch stopWatch = createStarted();
        stagingPostService.deleteContributor(username, postId, contributorIdent);
        stopWatch.stop();
        appLogService.logStagingPostContributorDelete(username, stopWatch, postId, contributorIdent);
        return ok().body(buildResponseMessage("Deleted contributor '" + contributorIdent + "' from post Id " + postId));
    }
}
