package com.lostsidewalk.buffy.app.v1.credentials;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
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

import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Slf4j
@RestController
@Validated
public class QueueCredentialsController_Update extends QueueCredentialsController {

    //
    // UPDATE QUEUE CREDENTIAL
    //

    /**
     * Update the password for the queue credential given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * password of a queue credential object given by its Id, on a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to update.
     * @param credentialId The Id of the queue credential object to update.
     * @param httpMethod The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the password on the queue credential given by Id, on a queue given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully updated password")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/credentials/{credentialId}", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePassword(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update", required = true)
            String queueIdent,
            @PathVariable("credentialId")
            @Parameter(description = "The Id of the credential to update", required = true)
            Long credentialId,
            @RequestBody
            @Parameter(description = "The new password for the credential", required = true)
            @Valid String basicPassword,
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueCredential for user={}, queueIdent={}, credentialId={}, httpMethod={}", username, queueIdent, credentialId, httpMethod);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueCredentialsService.updatePassword(username, queueId, credentialId, basicPassword);
        stopWatch.stop();
        appLogService.logQueueCredentialUpdate(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Updated password for credential Id " + credentialId + " on queue Id " + queueId));
    }
}
