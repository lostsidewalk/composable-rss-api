package com.lostsidewalk.buffy.app.v1.credentials;

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

/**
 * Controller class for deleting queue credentials.
 */
@Slf4j
@RestController
@Validated
public class QueueCredentialsController_Delete extends QueueCredentialsController {

    /**
     * Delete all queue credentials from a queue given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * queue credentials from a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue from which all credentials will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all queue credentials from the queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue credentials")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/credentials", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueueCredentials(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to delete", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueCredentials for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueCredentialsService.deleteQueueCredentials(username, queueId);
        stopWatch.stop();
        appLogService.logQueueCredentialsDelete(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Deleted all credentials from queue Id " + queueId));
    }

    /**
     * Delete a queue credential given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * queue credential given by its Id, on a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue from which the credential will be removed.
     * @param credentialId The Id of the credential to delete.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a queue credential by Id on a queue given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue credential")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/credentials/{credentialId}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueueCredential(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to delete", required = true)
            String queueIdent,
            @PathVariable("credentialId")
            @Parameter(description = "The Id of the credential to delete", required = true)
            Long credentialId,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueCredential for user={}, queueIdent={}, credentialId={}", username, queueIdent, credentialId);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueCredentialsService.deleteQueueCredential(username, queueId, credentialId);
        stopWatch.stop();
        appLogService.logQueueCredentialDelete(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Deleted credential with Id " + credentialId + " from queue Id " + queueId));
    }
}
