package com.lostsidewalk.buffy.app.v1.options;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import com.lostsidewalk.buffy.app.model.v1.response.ExportConfigDTO;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class QueueOptionsController_Retrieve extends QueueOptionsController {

    //
    // RETRIEVE EXPORT OPTIONS
    //

    private static final Gson GSON = new Gson();

    /**
     * Get the export configuration from a queue given by its identifier.
     *
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
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/options", produces = {APPLICATION_JSON_VALUE})
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
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        Serializable s = queueDefinition.getExportConfig();
        ExportConfigDTO exportOptions = null;
        if (s != null) {
            exportOptions = GSON.fromJson(GSON.toJson(s), ExportConfigDTO.class);
            validator.validate(exportOptions);
        }
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, id, "exportConfig");
        return ok(exportOptions);
    }

    /**
     * Get the ATOM export configuration for the queue given by its identifier.
     *
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
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/options/atomConfig", produces = {APPLICATION_JSON_VALUE})
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
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        Serializable s = queueDefinition.getExportConfig();
        Atom10Config  atomConfig = null;
        if (s != null) {
            ExportConfigDTO exportOptions = GSON.fromJson(GSON.toJson(s), ExportConfigDTO.class);
            atomConfig = exportOptions.getAtomConfig();
            validator.validate(atomConfig);
        }
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, id, "atomConfig");
        return ok(atomConfig);
    }

    /**
     * Get the RSS export configuration for the queue given by its identifier.
     *
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
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/options/rssConfig", produces = {APPLICATION_JSON_VALUE})
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
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        Serializable s = queueDefinition.getExportConfig();
        RSS20Config rssConfig = null;
        if (s != null) {
            ExportConfigDTO exportOptions = GSON.fromJson(GSON.toJson(s), ExportConfigDTO.class);
            rssConfig = exportOptions.getRssConfig();
            validator.validate(rssConfig);
        }
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, id, "rssConfig");
        return ok(rssConfig);
    }
}
