package com.lostsidewalk.buffy.app.v1.contributor;

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
public class ContributorController_Update extends ContributorController {

    /**
     * Update all post contributors on a post given by Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post contributors on a post given by its Id.
     *
     * @param postId                        The Id of the post to update.
     * @param postContributorConfigRequests A list of PostPersonConfigRequest objects representing the contributors of the post.
     * @param httpMethod                    The HTTP method in use, either PATCH or PUT.
     * @param authentication                The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update all post contributors on a post by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post contributors")
    @RequestMapping(value = "/${api.version}/posts/{postId}/contributors", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostContributors(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            @Valid @RequestBody List<PostPersonConfigRequest> postContributorConfigRequests,
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContributors for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updateContributors(username, postId, postContributorConfigRequests, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostContributorsUpdate(username, stopWatch, postId, size(postContributorConfigRequests));
        return ok().body(buildResponseMessage("Updated contributors on post Id " + postId));
    }

    /**
     * Update a post contributor given by identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post contributor given by its identifier, on a post given by its Id.
     *
     * @param postId                       The Id of the post to update.
     * @param contributorIdent             The identifier of the contributor to update.
     * @param postContributorConfigRequest A PostPersonConfigRequest representing new contributor to update.
     * @param httpMethod                   The HTTP method in use, either PATCH or PUT.
     * @param authentication               The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update a post contributor by identifier on a post by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post contributor")
    @RequestMapping(value = "/${api.version}/posts/{postId}/contributors/{contributorIdent}", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostContributor(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            @PathVariable("contributorIdent")
            @Parameter(description = "The identifier of the contributor to update", required = true)
            String contributorIdent,
            @Valid @RequestBody PostPersonConfigRequest postContributorConfigRequest,
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostContributor for user={}, postId={}, contributorIdent={}, httpMethod={}", username, postId, contributorIdent, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updateContributor(username, postId, contributorIdent, postContributorConfigRequest, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostContributorUpdate(username, stopWatch, postId, contributorIdent);
        return ok().body(buildResponseMessage("Updated contributor '" + contributorIdent + "' on post Id " + postId));
    }
}
