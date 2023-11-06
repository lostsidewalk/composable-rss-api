package com.lostsidewalk.buffy.app.v1.queue;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import com.lostsidewalk.buffy.app.model.v1.request.ExportConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.ExportConfigDTO;
import com.lostsidewalk.buffy.app.model.v1.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.v1.BaseQueueController;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller class for managing queue export configuration-related operations.
 * <p>
 * This controller provides endpoints for managing export configurations within queues. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
public class QueueOptionsController extends BaseQueueController {

    //
    // RETRIEVE EXPORT OPTIONS
    //

    private static final Gson GSON = new Gson();

    /**
     * Get the export configuration from a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * export configuration from a queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to fetch the export options from.
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity containing the fetched export options.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the export options from the queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched export options",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExportConfigDTO.class)))
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/options", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ExportConfigDTO> getExportOptions(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch export options from", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getExportOptions for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, id);
        Serializable serializable = queueDefinition.getExportConfig();
        ExportConfigDTO exportOptions = null;
        if (serializable != null) {
            exportOptions = GSON.fromJson(GSON.toJson(serializable), ExportConfigDTO.class);
            getValidator().validate(exportOptions);
        }
        stopWatch.stop();
        AppLogService.logQueueAttributeFetch(username, stopWatch, id, "exportConfig");
        return ok(exportOptions);
    }

    /**
     * Get the ATOM export configuration for the queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * ATOM export configuration for the queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to fetch the ATOM export options from.
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity containing the fetched ATOM export options.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the ATOM export configuration from the queue given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched ATOM export configuration",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Atom10Config.class)))
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/options/atomConfig", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<Atom10Config> getAtomExportOptions(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch ATOM export configuration from", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getAtomExportOptions for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, id);
        Serializable serializable = queueDefinition.getExportConfig();
        Atom10Config atomConfig = null;
        if (serializable != null) {
            ExportConfigDTO exportOptions = GSON.fromJson(GSON.toJson(serializable), ExportConfigDTO.class);
            atomConfig = exportOptions.getAtomConfig();
            getValidator().validate(atomConfig);
        }
        stopWatch.stop();
        AppLogService.logQueueAttributeFetch(username, stopWatch, id, "atomConfig");
        return ok(atomConfig);
    }

    /**
     * Get the RSS export configuration for the queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve the
     * RSS export configuration for the queue given by its identifier.
     *
     * @param queueIdent The identifier of the queue to fetch the ATOM export options from.
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity containing the fetched RSS export options.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the RSS export configuration from the queue given by Id")
    @ApiResponse(responseCode = "200", description = "Successfully fetched RSS export configuration",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = RSS20Config.class)))
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/options/rssConfig", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<RSS20Config> getRssExportOptions(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch RSS export configuration from", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getRssExportOptions for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, id);
        Serializable serializable = queueDefinition.getExportConfig();
        RSS20Config rssConfig = null;
        if (serializable != null) {
            ExportConfigDTO exportOptions = GSON.fromJson(GSON.toJson(serializable), ExportConfigDTO.class);
            rssConfig = exportOptions.getRssConfig();
            getValidator().validate(rssConfig);
        }
        stopWatch.stop();
        AppLogService.logQueueAttributeFetch(username, stopWatch, id, "rssConfig");
        return ok(rssConfig);
    }

    //
    // UPDATE EXPORT OPTIONS
    //

    /**
     * Update the export options for the queue given by its identifier.
     * <p>
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/options", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
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
        StopWatch updateTimer = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateExportConfig(username, queueId, exportConfigRequest, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "exportConfig");
    }

    /**
     * Update the ATOM export options for the queue given by its identifier.
     * <p>
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/options/atomConfig", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
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
        StopWatch updateTimer = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateAtomExportConfig(username, queueId, atomConfig, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "atomConfig");
    }

    /**
     * Update the RSS export options for the queue given by its identifier.
     * <p>
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/options/rssConfig", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
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
        StopWatch updateTimer = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateRssExportConfig(username, queueId, rssConfig, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "rssConfig");
    }

    //
    // DELETE EXPORT OPTIONS
    //

    /**
     * Delete all queue export options from a queue given by its identifier.
     * <p>
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/options", produces = APPLICATION_JSON_VALUE)
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
        StopWatch deleteTimer = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getQueueDefinitionService().clearExportConfig(username, queueId);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, queueId, "exportConfig");
    }

    /**
     * Delete the ATOM export configuration from a queue given by its identifier.
     * <p>
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/options/atomConfig", produces = APPLICATION_JSON_VALUE)
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
        StopWatch deleteTimer = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getQueueDefinitionService().clearAtomExportConfig(username, queueId);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, queueId, "atomConfig");
    }

    /**
     * Delete the RSS export configuration from a queue given by its identifier.
     * <p>
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/options/rssConfig", produces = APPLICATION_JSON_VALUE)
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
        StopWatch deleteTimer = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getQueueDefinitionService().clearRssExportConfig(username, queueId);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, queueId, "rssConfig");
    }
}
