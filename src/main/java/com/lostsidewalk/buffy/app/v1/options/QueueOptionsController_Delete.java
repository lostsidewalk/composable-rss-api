package com.lostsidewalk.buffy.app.v1.options;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

import java.util.Map;

import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class QueueOptionsController_Delete extends QueueOptionsController {

    //
    // DELETE EXPORT OPTIONS
    //

    /**
     * Delete all queue export options from a queue given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * queue export configuration from a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue from which all export options will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing a QueueConfigResponse with deployment details about the updated queue.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all export options from the queue given by identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted export options")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/options", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueConfigResponse> deleteOptions(
        @PathVariable("queueIdent")
        @Parameter(description = "The identifier of the queue to delete", required = true)
        String queueIdent,
        Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteOptions for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueDefinitionService.clearExportConfig(username, queueId);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, queueId, "exportConfig", pubResults);
        return ok().body(queueConfigResponse);
    }

    /**
     * Delete the ATOM export configuration from a queue given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * ATOM export configuration from a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue from which ATOM export options will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing a QueueConfigResponse with deployment details about the updated queue.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete ATOM export options from the queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted ATOM export options")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/options/atomConfig", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueConfigResponse> deleteAtomOptions(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to delete", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteAtomOptions for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueDefinitionService.clearAtomExportConfig(username, queueId);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, queueId, "atomConfig", pubResults);
        return ok().body(queueConfigResponse);
    }

    /**
     * Delete the RSS export configuration from a queue given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * RSS export configuration from a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue from which RSS export options will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing a QueueConfigResponse with deployment details about the updated queue.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete RSS export options from the queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted RSS export options")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/options/rssConfig", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
public ResponseEntity<QueueConfigResponse> deleteRssOptions(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to delete", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteRssOptions for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueDefinitionService.clearRssExportConfig(username, queueId);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, queueId, "rssConfig", pubResults);
        return ok().body(queueConfigResponse);
    }
}
