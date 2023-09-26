package com.lostsidewalk.buffy.app.v1.credentials;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.QueueCredentialConfigRequest;
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

/**
 * Controller class for creating queue credentials.
 */
@Slf4j
@RestController
@Validated
public class QueueCredentialsController_Create extends QueueCredentialsController {

    /**
     * Add a new credential to the queue given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add a new credential
     * to a specific queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue into which the new credential should be added.
     * @param queueCredentialConfigRequest A QueueCredentialConfigRequest object representing the new credential to add.
     * @param authentication The authentication details of the user making the request.
     * @return a ResponseEntity containing the created credential
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     * @throws DataConflictException If there is a duplicate key.
     */
    @Operation(summary = "Add a new credential to the queue given by its identifier")
    @ApiResponse(responseCode = "201", description = "Successfully added credential to the queue")
    @PostMapping(value = "/${api.version}/queues/{queueIdent}/credentials", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addQueueCredential(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to add the credential to", required = true)
            String queueIdent,
            //
            @RequestBody
            @Parameter(description = "A credential object", required = true,
                    schema = @Schema(implementation = QueueCredentialConfigRequest.class))
            @Valid QueueCredentialConfigRequest queueCredentialConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addQueueCredential adding credential for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        Long credentialId = queueCredentialsService.addCredential(username, queueId, queueCredentialConfigRequest.getBasicUsername(), queueCredentialConfigRequest.getBasicPassword());
        URI createdLocation = URI.create("/queues/" + queueId + "/credentials/" + credentialId);
        stopWatch.stop();
        appLogService.logQueueCredentialCreate(username, stopWatch, queueId);
        return created(createdLocation).body(buildResponseMessage("Added credential Id " + credentialId + " to queue Id " + queueId));
    }
}
