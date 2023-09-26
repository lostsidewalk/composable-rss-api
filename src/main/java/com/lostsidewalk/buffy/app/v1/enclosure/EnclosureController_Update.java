package com.lostsidewalk.buffy.app.v1.enclosure;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.PostEnclosureConfigRequest;
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
public class EnclosureController_Update extends EnclosureController {

    //
    // UPDATE POST ENCLOSURE
    //

    /**
     * Update all post enclosures on a post given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update all
     * post enclosures, on a post given by its Id.
     *
     * @param postId                          The Id of the post to update.
     * @param postEnclosureConfigRequests     A list of PostEnclosureConfigRequest objects, representing the enclosures on the post.
     * @param httpMethod                      The HTTP method in use, either PATCH or PUT.
     * @param authentication                  The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update all post enclosures on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated post enclosures")
    @RequestMapping(value = "/${api.version}/posts/{postId}/enclosures", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostEnclosures(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @Valid @RequestBody List<PostEnclosureConfigRequest> postEnclosureConfigRequests,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostEnclosures for user={}, postId={}, httpMethod={}", username, postId, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updateEnclosures(username, postId, postEnclosureConfigRequests, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostEnclosuresUpdate(username, stopWatch, postId, size(postEnclosureConfigRequests));
        return ok().body(buildResponseMessage("Updated enclosures on post Id " + postId));
    }

    /**
     * Update a post enclosure given by its identifier on a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update a
     * post enclosure given by its identifier, on a post given by its Id.
     *
     * @param postId                          The Id of the post to update.
     * @param enclosureIdent                  The identifier of the enclosure to update.
     * @param postEnclosureConfigRequest      A PostEnclosureConfigRequest, representing the enclosure to update.
     * @param httpMethod                      The HTTP method in use, either PATCH or PUT.
     * @param authentication                  The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update a post enclosure on a post given by Ident")
    @ApiResponse(responseCode = "200", description = "Successfully updated post enclosure")
    @RequestMapping(value = "/${api.version}/posts/{postId}/enclosures/{enclosureIdent}", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to update", required = true)
            Long postId,
            //
            @PathVariable("enclosureIdent")
            @Parameter(description = "The identifier of the enclosure to update", required = true)
            String enclosureIdent,
            //
            @Valid @RequestBody PostEnclosureConfigRequest postEnclosureConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostEnclosure for user={}, postId={}, enclosureIdent={}, httpMethod={}", username, postId, enclosureIdent, httpMethod);
        StopWatch stopWatch = createStarted();
        stagingPostService.updateEnclosure(username, postId, enclosureIdent, postEnclosureConfigRequest, isPatch(httpMethod));
        stopWatch.stop();
        appLogService.logStagingPostEnclosureUpdate(username, stopWatch, postId, enclosureIdent);
        return ok().body(buildResponseMessage("Updated enclosure '" + enclosureIdent + "' on post Id " + postId));
    }
}
