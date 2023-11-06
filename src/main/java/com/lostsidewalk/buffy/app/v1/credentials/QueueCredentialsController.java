package com.lostsidewalk.buffy.app.v1.credentials;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.credentials.QueueCredentialsService;
import com.lostsidewalk.buffy.app.etag.ETagger;
import com.lostsidewalk.buffy.app.model.v1.request.QueueCredentialConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils;
import com.lostsidewalk.buffy.queue.QueueCredential;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller class for managing credential-related operations.
 * <p>
 * This controller provides endpoints for managing credential objects within queues. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
class QueueCredentialsController {

    @Autowired
    QueueCredentialsService queueCredentialsService;

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    Validator validator;

    /**
     * Add a new credential to the queue given by its identifier.
     * <p>
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
    @PostMapping(value = "/${api.version}/queues/{queueIdent}/credentials", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessageUtils.ResponseMessage> addQueueCredential(
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
        AppLogService.logQueueCredentialCreate(username, stopWatch, queueId);
        return created(createdLocation).body(buildResponseMessage("Added credential Id " + credentialId + " to queue Id " + queueId));
    }

    /**
     * Get all credentials in the queue given by its identifier. Passwords are masked.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * credentials in a specific queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to fetch credentials from.
     * @param offset The number of items to skip before returning results.
     * @param limit The maximum number of items to return.
     * @param ifNoneMatch if-none-match HTTP header value (for e-tag evaluation).
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched queue credentials.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all credentials in the queue given by its Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue credentials",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = QueueCredential.class))))
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/credentials", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<QueueCredential>> getQueueCredentials(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch credentials from", required = true)
            String queueIdent,
            @Parameter(description = "The number of items to skip before returning results")
            @Valid @RequestParam(name = "offset", required = false)
            @Positive
            Integer offset,
            @Parameter(description = "The maximum number of items to return")
            @Valid @RequestParam(name = "limit", required = false)
            @Positive
            Integer limit,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueCredentials for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        List<QueueCredential> queueCredentials = queueCredentialsService.findByQueueId(username, queueId);
        String eTag = ETagger.computeEtag(queueCredentials);
        if (eTag.equals(ifNoneMatch)) {
            return status(NOT_MODIFIED).build(); // TODO: unit test
        }
        if (isNotEmpty(queueCredentials)) {
            queueCredentials = Paginator.paginate(queueCredentials, offset, limit);
            validator.validate(queueCredentials);
        }
        stopWatch.stop();
        AppLogService.logQueueCredentialsFetch(username, stopWatch, queueId);
        return ok()
                .eTag(eTag)
                .body(queueCredentials);
    }

    /**
     * Get a queue credential by its Id in a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to fetch the credential from.
     * @param credentialId The Id of the credential to fetch.
     * @param ifNoneMatch if-none-match HTTP header value (for e-tag evaluation).
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity containing the fetched queue credential.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a queue credential by Id")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/credentials/{credentialId}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue credential",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QueueDTO.class)))
    public ResponseEntity<QueueCredential> getQueueCredential(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the credential from", required = true)
            String queueIdent,
            @PathVariable("credentialId")
            @Parameter(description = "The Id of the credential to fetch", required = true)
            Long credentialId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueCredential for user={}, queueIdent={}, credentialId={}", username, queueIdent, credentialId);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueCredential queueCredential = queueCredentialsService.findById(username, queueId, credentialId);
        String eTag = ETagger.computeEtag(queueCredential);
        if (eTag.equals(ifNoneMatch)) {
            return status(NOT_MODIFIED).build(); // TODO: unit test
        }
        validator.validate(queueCredential);
        stopWatch.stop();
        AppLogService.logQueueCredentialFetch(username, stopWatch, queueId, credentialId);
        return ok()
                .eTag(eTag)
                .body(queueCredential);
    }

    //
    // UPDATE QUEUE CREDENTIAL
    //

    /**
     * Update the password for the queue credential given by its identifier.
     * <p>
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/credentials/{credentialId}", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessageUtils.ResponseMessage> updatePassword(
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
        AppLogService.logQueueCredentialUpdate(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Updated password for credential Id " + credentialId + " on queue Id " + queueId));
    }

    /**
     * Delete all queue credentials from a queue given by its identifier.
     * <p>
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/credentials", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessageUtils.ResponseMessage> deleteQueueCredentials(
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
        AppLogService.logQueueCredentialsDelete(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Deleted all credentials from queue Id " + queueId));
    }

    /**
     * Delete a queue credential given by its identifier.
     * <p>
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/credentials/{credentialId}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessageUtils.ResponseMessage> deleteQueueCredential(
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
        AppLogService.logQueueCredentialDelete(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Deleted credential with Id " + credentialId + " from queue Id " + queueId));
    }

    @Override
    public String toString() {
        return "QueueCredentialsController{" +
                "queueCredentialsService=" + queueCredentialsService +
                ", queueDefinitionService=" + queueDefinitionService +
                ", validator=" + validator +
                '}';
    }
}
