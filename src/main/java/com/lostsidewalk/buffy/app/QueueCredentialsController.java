package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.credentials.QueueCredentialsService;
import com.lostsidewalk.buffy.app.model.request.QueueCredentialConfigRequest;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.queue.QueueCredential;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for managing credential-related operations.
 *
 * This controller provides endpoints for managing credential objects within queues. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class QueueCredentialsController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    QueueCredentialsService queueCredentialsService;

    @Autowired
    Validator validator;

    //
    // CREATE QUEUE CREDENTIAL
    //

    /**
     * Add new credential to the queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to add new credential
     * to a specific queue identified by its Id.
     *
     * @param queueId                              The Id of the queue into which the new credential should be added.
     * @param queueCredentialConfigRequest         A QueueCredentialConfigRequest object representing new credential to add.
     * @param authentication                       The authentication details of the user making the request.
     * @return a ResponseEntity containing the created credential
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Add new credential to the queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully added credential to queue")
    @PostMapping(value = "/queues/{queueId}/credentials", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> addQueueCredential(
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to add credential to", required = true)
            Long queueId,
            //
            @RequestBody
            @Parameter(description = "A credential object", required = true,
                    schema = @Schema(implementation = QueueCredentialConfigRequest.class))
            @Valid QueueCredentialConfigRequest queueCredentialConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("addQueueCredential adding credential for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        queueCredentialsService.addCredential(username, queueId, queueCredentialConfigRequest.getBasicUsername(), queueCredentialConfigRequest.getBasicPassword());
        stopWatch.stop();
        appLogService.logQueueCredentialCreate(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Added credential to queue Id " + queueId));
    }

    //
    // RETRIEVE QUEUE CREDENTIAL
    //

    /**
     * Get all credential in the queue given by its Id.  Passwords are masked.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * credentials in a specific queue identified by its Id.
     *
     * @param queueId          The Id of the queue to fetch credentials from.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched queue credentials.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all credentials in the queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue credentials",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = QueueCredential.class))))
    @GetMapping(value = "/queues/{queueId}/credentials", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<QueueCredential>> getQueueCredential(
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch credentials from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueCredentials for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        List<QueueCredential> queueCredentials = queueCredentialsService.findByQueueId(username, queueId);
        stopWatch.stop();
        validator.validate(queueCredentials);
        appLogService.logQueueCredentialsFetch(username, stopWatch, queueId);
        return ok(queueCredentials);
    }

    //
    // UPDATE QUEUE CREDENTIAL
    //

    /**
     * Update a password for the queue credential given by username.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * password of a queue credential object identified by its username, on a queue identified by its Id.
     *
     * @param queueId               The Id of the queue to update.
     * @param basicUsername         The basic username of the queue credential object to update.
     * @param authentication        The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the update operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the password on the queue credential given by username, on a queue given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated password")
    @PutMapping(value = "/queues/{queueId}/credentials/{basicUsername}", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updatePassword(
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to update", required = true)
            Long queueId,
            //
            @PathVariable("basicUsername")
            @Parameter(description = "The basic username of the credential to update", required = true)
            String basicUsername,
            //
            @Valid @RequestBody String basicPassword, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueCredential for user={}, queueId={}, basicUsername={}", username, queueId, basicUsername);
        StopWatch stopWatch = createStarted();
        queueCredentialsService.updatePassword(username, queueId, basicUsername, basicPassword);
        stopWatch.stop();
        appLogService.logQueueCredentialUpdate(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Updated credential on queue Id " + queueId));
    }

    //
    // DELETE QUEUE CREDENTIAL
    //

    /**
     * Delete a queue credential given by username.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete a
     * a queue credential identified by its index, on a queue identified by its Id.
     *
     * @param queueId               The Id of the queue whence the credential will be removed.
     * @param basicUsername         The username of the credential to delete.
     * @param authentication        The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the queue credential given by username, on a queue given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue credential")
    @DeleteMapping(value = "/queues/{queueId}/credentials/{basicUsername}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueueCredential(
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to delete", required = true)
            Long queueId,
            //
            @PathVariable("basicUsername")
            @Parameter(description = "The username of the credential to delete", required = true)
            String basicUsername,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueCredential for user={}, queueId={}, basicUsername={}", username, queueId, basicUsername);
        StopWatch stopWatch = createStarted();
        queueCredentialsService.deleteQueueCredential(username, queueId, basicUsername);
        stopWatch.stop();
        appLogService.logQueueCredentialDelete(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Deleted credential from queue Id " + queueId));
    }
}
