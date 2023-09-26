package com.lostsidewalk.buffy.app.v1.url;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostUrlConfigRequest;
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
public class URLController_Update extends URLController {

    //
    // UPDATE POST URL
    //

    /**
     * Update all post URLs on a post a given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post URLs on a post given by its Id.
     *
     * @param postId                              The Id of the post to update.
     * @param postUrlConfigRequests               A list of PostUrlConfigRequest objects representing the URLs on the post.
     * @param httpMethod                          The HTTP method in use, either PATCH or PUT.
     * @param authentication                      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update all post URLs on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post URLs")
    @RequestMapping(value = "/${api.version}/posts/{postId}/urls", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostURLs(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody List<PostUrlConfigRequest> postUrlConfigRequests,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostURLs for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostUrls(username, postId, postUrlConfigRequests, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostUrlsUpdate(username, stopWatch, postId, size(postUrlConfigRequests));
        return ok().body(buildResponseMessage("Updated URLs on post Id " + postId));
    }

    /**
     * Update a post URL given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post URL given by its identifier, on a post given by its Id.
     *
     * @param postId                         The Id of the post to update.
     * @param urlIdent                       The identifier of the URL to update.
     * @param postUrlConfigRequest           A PostUrlConfigRequest, representing the URL to update.
     * @param httpMethod                     The HTTP method in use, either PATCH or PUT.
     * @param authentication                 The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update a post URL on a post given by Ident")
    @ApiResponse(responseCode = "200", description = "Successfully updated post URL")
    @RequestMapping(value = "/${api.version}/posts/{postId}/urls/{urlIdent}", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostUrl(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("urlIdent")
            @Parameter(description = "The identifier of the URL to update", required = true)
            String urlIdent,
            //
            @Valid @RequestBody PostUrlConfigRequest postUrlConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostUrl for user={}, postId={}, urlIdent={}, httpMethod={}", username, postId, urlIdent, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updatePostUrl(username, postId, urlIdent, postUrlConfigRequest, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostUrlUpdate(username, stopWatch, postId, urlIdent);
        return ok().body(buildResponseMessage("Updated URL '" + urlIdent + "' on post Id " + postId));
    }
}
