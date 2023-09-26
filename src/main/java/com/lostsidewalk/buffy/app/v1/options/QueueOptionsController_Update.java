package com.lostsidewalk.buffy.app.v1.options;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import com.lostsidewalk.buffy.app.model.v1.request.ExportConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.lostsidewalk.buffy.queue.QueueDefinition;
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

import java.io.Serializable;
import java.util.Map;

import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Slf4j
@RestController
@Validated
public class QueueOptionsController_Update extends QueueOptionsController {

    //
    // UPDATE EXPORT OPTIONS
    //

    /**
     * Update the export options for the queue given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * export options of a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to update.
     * @param httpMethod The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing a QueueConfigResponse with deployment details about the updated queue.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the export options on the queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully updated export options")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/options", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateOptions(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier the queue to update", required = true)
            String queueIdent,
            //
            @RequestBody
            @Parameter(description = "The new export options for the queue", required = true)
            @Valid ExportConfigRequest exportConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateOptions for user={}, queueIdent={}, httpMethod={}", username, queueIdent, httpMethod);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        Serializable updatedExportConfig = queueDefinitionService.updateExportConfig(username, queueId, exportConfigRequest, isPatch(httpMethod));
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, queueId, "exportConfig", pubResults);
        return ok().body(queueConfigResponse);
    }

    /**
     * Update the ATOM export options for the queue given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * ATOM export options of a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to update.
     * @param httpMethod The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing a QueueConfigResponse with deployment details about the updated queue.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the ATOM export options on the queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully updated ATOM export options")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/options/atomConfig", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateAtomOptions(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update", required = true)
            String queueIdent,
            //
            @RequestBody
            @Parameter(description = "The new ATOM export options for the queue", required = true)
            @Valid
            Atom10Config atomConfig,
            //
            HttpMethod httpMethod,
            Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateAtomOptions for user={}, queueIdent={}, httpMethod={}", username, queueIdent, httpMethod);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        Atom10Config updatedAtomExportConfig = queueDefinitionService.updateAtomExportConfig(username, queueId, atomConfig, isPatch(httpMethod));
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, queueId, "atomConfig", pubResults);
        return ok().body(queueConfigResponse);
    }

    /**
     * Update the RSS export options for the queue given by its identifier.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the
     * RSS export options of a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to update.
     * @param httpMethod The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing a QueueConfigResponse with deployment details about the updated queue.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the RSS export options on the queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully updated RSS export options")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/options/rssConfig", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateRssOptions(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update", required = true)
            String queueIdent,
            //
            @RequestBody
            @Parameter(description = "The new RSS export options for the queue", required = true)
            @Valid
            RSS20Config rssConfig,
            //
            HttpMethod httpMethod,
            Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateRssOptions for user={}, queueIdent={}, httpMethod={}", username, queueIdent, httpMethod);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        RSS20Config updatedRssExportConfig = queueDefinitionService.updateRssExportConfig(username, queueId, rssConfig, isPatch(httpMethod));
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, queueId, "rssConfig", pubResults);
        return ok().body(queueConfigResponse);
    }
}
