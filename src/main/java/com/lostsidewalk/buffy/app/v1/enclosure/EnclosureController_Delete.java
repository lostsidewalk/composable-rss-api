package com.lostsidewalk.buffy.app.v1.enclosure;

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
public class EnclosureController_Delete extends EnclosureController {

    //
    // DELETE POST ENCLOSURE
    //

    /**
     * Delete all post enclosures on a post given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * post enclosures on a post given by its Id.
     *
     * @param postId The Id of the post from which the enclosures will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all post enclosures on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post enclosures")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/enclosures", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostEnclosures(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete enclosures from", required = true)
            Long postId,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostEnclosures for user={}, postId={}", username, postId);
        StopWatch stopWatch = createStarted();
        stagingPostService.deleteEnclosures(username, postId);
        stopWatch.stop();
        appLogService.logStagingPostEnclosuresDelete(username, stopWatch, postId);
        return ok().body(buildResponseMessage("Deleted all enclosures from post Id " + postId));
    }

    /**
     * Delete a post enclosure given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * post enclosure given by its identifier, on a post given by its Id.
     *
     * @param postId The Id of the post from which the enclosure will be removed.
     * @param enclosureIdent The identifier of the enclosure to delete.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a post enclosure by its identifier on a post given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted post enclosure")
    @DeleteMapping(value = "/${api.version}/posts/{postId}/enclosures/{enclosureIdent}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePostEnclosure(
            @PathVariable("postId")
            @Parameter(description = "The Id of the post to delete an enclosure from", required = true)
            Long postId,
            //
            @PathVariable("enclosureIdent")
            @Parameter(description = "The identifier of the enclosure to delete", required = true)
            String enclosureIdent,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePostEnclosure for user={}, postId={}, enclosureIdent={}", username, postId, enclosureIdent);
        StopWatch stopWatch = createStarted();
        stagingPostService.deleteEnclosure(username, postId, enclosureIdent);
        stopWatch.stop();
        appLogService.logStagingPostEnclosureDelete(username, stopWatch, postId, enclosureIdent);
        return ok().body(buildResponseMessage("Deleted enclosure '" + enclosureIdent + "' from post Id " + postId));
    }
}
