package com.lostsidewalk.buffy.app.v1.credentials;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.queue.QueueCredential;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

/**
 * Controller class for retrieving queue credentials.
 */
@Slf4j
@RestController
@Validated
public class QueueCredentialsController_Retrieve extends QueueCredentialsController {

    /**
     * Get all credentials in the queue given by its identifier. Passwords are masked.
     *
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
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/credentials", produces = {APPLICATION_JSON_VALUE})
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
        String eTag = eTagger.computeEtag(queueCredentials);
        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            return status(NOT_MODIFIED).build();
        }
        if (isNotEmpty(queueCredentials)) {
            queueCredentials = paginator.paginate(queueCredentials, offset, limit);
            validator.validate(queueCredentials);
        }
        stopWatch.stop();
        appLogService.logQueueCredentialsFetch(username, stopWatch, queueId);
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
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/credentials/{credentialId}", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = StopWatch.createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueCredential queueCredential = queueCredentialsService.findById(username, queueId, credentialId);
        String eTag = eTagger.computeEtag(queueCredential);
        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            return status(NOT_MODIFIED).build();
        }
        validator.validate(queueCredential);
        stopWatch.stop();
        appLogService.logQueueCredentialFetch(username, stopWatch, queueId, credentialId);
        return ok()
                .eTag(eTag)
                .body(queueCredential);
    }
}
