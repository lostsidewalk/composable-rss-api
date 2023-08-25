package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.QueueAuthUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.request.QueueStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.QueueDTO;
import com.lostsidewalk.buffy.app.model.response.ResponseMessage;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.lostsidewalk.buffy.queue.QueueDefinition.QueueStatus;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.length;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for managing queue-related operations.
 * <p>
 * This controller provides endpoints for managing queues, including fetching, creating,
 * updating, and deleting queue configurations. Authenticated users with the "VERIFIED_ROLE"
 * have access to these operations.
 */
@Slf4j
@RestController
@Validated
public class QueueController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    Validator validator;

    //
    // CREATE QUEUES
    //

    /**
     * Create new queue definitions.
     *
     * @param queueConfigRequests List of queue configuration requests.
     * @param authentication      The authenticated user's details.
     * @return A ResponseEntity containing the created queue configurations.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Create a new queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully created queue configurations",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = QueueDTO.class))))
//    @ApiResponse(responseCode = "400", description = "Validation error in request body")
    @PostMapping(value = "/queues", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<QueueDTO>> createQueue(
            @RequestBody
            @Parameter(description = "List of queue configuration requests", required = true,
                    schema = @Schema(implementation = QueueConfigRequest.class))
            List<@Valid QueueConfigRequest> queueConfigRequests,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createQueue adding {} queues for user={}", size(queueConfigRequests), username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<QueueDefinition> createdQueues = new ArrayList<>();
        List<QueueDTO> queueConfigResponse = new ArrayList<>();
        // for ea. queue config request
        for (QueueConfigRequest queueConfigRequest : queueConfigRequests) {
            // create the queue
            Long queueId = queueDefinitionService.createQueue(username, queueConfigRequest);
            // re-fetch this queue definition
            QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
            createdQueues.add(queueDefinition);
            // build queue config responses to return the front-end
            queueConfigResponse.add(convertToDTO(queueDefinition));
        }
        stopWatch.stop();
        validator.validate(queueConfigResponse);
        appLogService.logQueueCreate(username, stopWatch, queueConfigRequests.size(), size(createdQueues));
        return ok(queueConfigResponse);
    }

    //
    // RETRIEVE QUEUES
    //

    /**
     * Get all queue definitions for the authenticated user.
     *
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity containing the fetched queue definitions.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all queue definitions", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @GetMapping(value = "/queues", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue definitions",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = QueueDTO.class))))
    public ResponseEntity<List<QueueDTO>> getQueues(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueues for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        // queue definitions
        List<QueueDefinition> queueDefinitions = queueDefinitionService.findByUser(username);
        stopWatch.stop();
        List<QueueDTO> queues = queueDefinitions.stream().map(QueueController::convertToDTO).toList();
        validator.validate(queues);
        appLogService.logQueueFetch(username, stopWatch, size(queueDefinitions));
        return ok(queues);
    }

    private static QueueDTO convertToDTO(QueueDefinition q) {
        return QueueDTO.from(q.getId(),
                q.getIdent(),
                q.getTitle(),
                q.getDescription(),
                q.getGenerator(),
                q.getTransportIdent(),
                q.getIsAuthenticated(),
                q.getExportConfig(),
                q.getCopyright(),
                q.getLanguage(),
                q.getQueueImgSrc(),
                q.getLastDeployed(),
                q.getIsAuthenticated()
        );
    }

    //
    // RETRIEVE QUEUE INDIVIDUAL FIELDS
    //

    /**
     * Get the status of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the status of a
     * queue identified by its Id.  The status of a post is given by the QueueStatus enumeration.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue status.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the status of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue status",
            content = @Content(schema = @Schema(implementation = QueueStatus.class)))
    @GetMapping(value = "/queues/{queueId}/status", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueStatus> getQueueStatus(
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch status from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueStatus for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "status");
        return ok(queueDefinition.getQueueStatus());
    }

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    /**
     * Get the ident string of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the ident string of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue ident string.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the ident string of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue ident string")
    @GetMapping(value = "/queues/{queueId}/ident", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueIdent(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the ident string from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueIdent for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "ident");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getIdent()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getIdent(), headers, OK);
        }
    }

    /**
     * Get the title of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the title of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue title.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the title of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue title")
    @GetMapping(value = "/queues/{queueId}/title", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueTitle(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the title from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueTitle for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "title");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getTitle()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getTitle(), headers, OK);
        }
    }

    /**
     * Get the description of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get description of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue description.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the description of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue description")
    @GetMapping(value = "/queues/{queueId}/desc", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueDescription(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the description from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueDescription for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "description");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getDescription()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getDescription(), headers, OK);
        }
    }

    /**
     * Get the generator of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the generator of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue generator.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the generator of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue generator")
    @GetMapping(value = "/queues/{queueId}/generator", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueGenerator(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the generator from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueGenerator for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "generator");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getGenerator()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getGenerator(), headers, OK);
        }
    }

    /**
     * Get the transport identifier of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the transport identifier of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue transport identifier.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the transport identifier of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue transport identifier")
    @GetMapping(value = "/queues/{queueId}/transport", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueTransport(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the transport identifier from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueTransport for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "transportIdent");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getTransportIdent()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getTransportIdent(), headers, OK);
        }
    }

    /**
     * Get the copyright of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the copyright of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue copyright.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the copyright of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue copyright")
    @GetMapping(value = "/queues/{queueId}/copyright", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueCopyright(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the copyright from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueCopyright for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "copyright");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getCopyright()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getCopyright(), headers, OK);
        }
    }

    /**
     * Get the language of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the language of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue language.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the language of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue language")
    @GetMapping(value = "/queues/{queueId}/language", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueLanguage(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the language from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueLanguage for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "language");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getLanguage()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getLanguage(), headers, OK);
        }
    }

    private static final SimpleDateFormat ISO_8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    /**
     * Get the deployed timestamp of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the deployed timestamp of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue deployed timestamp.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the deployed timestamp of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue deployed timestamp")
    @GetMapping(value = "/queues/{queueId}/deployed", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueDeployedTimestamp(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the deployed timestamp from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueDeployedTimestamp for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "lastDeployed");
        Date lastDeployedTimestamp = queueDefinition.getLastDeployed();
        String responseStr = lastDeployedTimestamp != null ?
                ISO_8601_TIMESTAMP_FORMAT.format(lastDeployedTimestamp) :
                EMPTY; // default response
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        }
    }

    /**
     * Get the authentication requirement of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the authentication requirement of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue authentication requirement.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the authentication requirement of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue authentication requirement")
    @GetMapping(value = "/queues/{queueId}/auth", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueAuthRequirement(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the authentication requirement from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueAuthRequirement for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "isAuthenticated");
        String responseStr = Boolean.toString(isTrue(queueDefinition.getIsAuthenticated()));
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        }
    }

    /**
     * Get the image source of a queue given by its Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the image source of a
     * queue identified by its Id.
     *
     * @param queueId         The Id of the queue to fetch.
     * @param authentication  The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue image source.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the image source of a queue given by its Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue image source")
    @GetMapping(value = "/queues/{queueId}/imgsrc", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueImageSource(
            @RequestHeader("Accept") String acceptHeader,
            //
            @PathVariable("queueId")
            @Parameter(description = "The Id of the queue to fetch the image source from", required = true)
            Long queueId,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueImageSource for user={}, queueId={}", username, queueId);
        StopWatch stopWatch = createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "queueImgSrc");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getQueueImgSrc()));
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getQueueImgSrc(), headers, OK);
        }
    }

    //
    // UPDATE QUEUE
    //

    /**
     * Update the properties of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the properties
     * of an existing queue identified by itsId. The queue's configuration properties are provided
     * in the request body.
     *
     * @param id                 The Id of the queue to be updated.
     * @param queueConfigRequest The updated queue configuration properties.
     * @param authentication     The authentication details of the user making the request.
     * @return A ResponseEntity containing the updated queue configuration along with a thumbnail.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Update the properties of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue configuration",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QueueDTO.class)))
    @PutMapping(value = "/queues/{id}", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueDTO> updateQueue(
            //
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to be updated", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue configuration properties", required = true,
                    schema = @Schema(implementation = QueueConfigRequest.class))
            QueueConfigRequest queueConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueue for user={}, queueId={}", username, id);
        StopWatch stopWatch = StopWatch.createStarted();
        QueueDefinition queueDefinition = queueDefinitionService.updateQueue(username, id, queueConfigRequest);
        stopWatch.stop();
        QueueDTO response = convertToDTO(queueDefinition);
        validator.validate(response);
        appLogService.logQueueUpdate(username, stopWatch, id);
        return ok(response);
    }

    //
    // UPDATE QUEUE INDIVIDUAL FIELDS
    //

    /**
     * Change the status of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the status
     * of an existing queue. The status change can be used to mark a queue for import or
     * un-mark it for import.
     *
     * @param id                       The Id of the queue to update the status for.
     * @param queueStatusUpdateRequest The request containing the updated queue status information.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the status update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the status of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue status")
//    @ApiResponse(responseCode = "400", description = "Validation error in request body")
//    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PutMapping(value = "/queues/{id}/status", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updateQueueStatus(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the status for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The request containing the updated queue status information", required = true,
                    schema = @Schema(implementation = QueueStatusUpdateRequest.class))
            QueueStatusUpdateRequest queueStatusUpdateRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueStatus for user={}, queueId={}, queueStatusUpdateRequest={}", username, id, queueStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        queueDefinitionService.updateQueueStatus(username, id, queueStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logQueueStatusUpdate(username, stopWatch, id, queueStatusUpdateRequest, 1);
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    /**
     * Change the ident string of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the ident string
     * of an existing queue.
     *
     * @param id                       The Id of the queue to update the status for.
     * @param ident                    The updated queue ident string.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the ident update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the ident string of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue ident")
//    @ApiResponse(responseCode = "400", description = "Validation error in request body")
//    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PutMapping(value = "/queues/{id}/ident", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updateQueueIdent(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the ident string for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue ident string", required = true)
            String ident, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueIdent for user={}, queueId={}, ident={}", username, id, ident);
        StopWatch stopWatch = StopWatch.createStarted();
        String updatedQueueIdent = queueDefinitionService.updateQueueIdent(username, id, ident);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "ident");
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    /**
     * Change the title of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the title
     * of an existing queue.
     *
     * @param id                       The Id of the queue to update the status for.
     * @param title                    The updated queue title.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the title update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the title of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue title")
//    @ApiResponse(responseCode = "400", description = "Validation error in request body")
//    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PutMapping(value = "/queues/{id}/title", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updateQueueTitle(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the title for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue title", required = true)
            String title, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueTitle for user={}, queueId={}, title={}", username, id, title);
        StopWatch stopWatch = StopWatch.createStarted();
        String updatedQueueTitle = queueDefinitionService.updateQueueTitle(username, id, title);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "title");
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    /**
     * Change the description of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the description
     * of an existing queue.
     *
     * @param id                       The Id of the queue to update the status for.
     * @param description                    The updated queue description.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the description update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the description of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue description")
//    @ApiResponse(responseCode = "400", description = "Validation error in request body")
//    @ApiResponse(responseCode = "500", description = "Internal server error")
    @PutMapping(value = "/queues/{id}/desc", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updateQueueDescription(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the description for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue description", required = true)
            String description, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueDescription for user={}, queueId={}, description={}", username, id, description);
        StopWatch stopWatch = StopWatch.createStarted();
        String updatedQueueDescription = queueDefinitionService.updateQueueDescription(username, id, description);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "description");
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    /**
     * Change the generator of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the generator
     * of an existing queue.
     *
     * @param id                       The Id of the queue to update the status for.
     * @param generator                    The updated queue generator.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the generator update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the generator of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue generator")
//    @ApiResponse(responseCode = "400", generator = "Validation error in request body")
//    @ApiResponse(responseCode = "500", generator = "Internal server error")
    @PutMapping(value = "/queues/{id}/generator", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updateQueueGenerator(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the generator for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue generator", required = true)
            String generator, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueGenerator for user={}, queueId={}, generator={}", username, id, generator);
        StopWatch stopWatch = StopWatch.createStarted();
        String updatedQueueGenertor = queueDefinitionService.updateQueueGenerator(username, id, generator);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "generator");
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    /**
     * Change the copyright of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the copyright
     * of an existing queue.
     *
     * @param id                       The Id of the queue to update the status for.
     * @param copyright                    The updated queue copyright.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the copyright update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the copyright of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue copyright")
//    @ApiResponse(responseCode = "400", copyright = "Validation error in request body")
//    @ApiResponse(responseCode = "500", copyright = "Internal server error")
    @PutMapping(value = "/queues/{id}/copyright", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updateQueueCopyright(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the copyright for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue copyright", required = true)
            String copyright, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueCopyright for user={}, queueId={}, copyright={}", username, id, copyright);
        StopWatch stopWatch = StopWatch.createStarted();
        String updatedQueueCopyright = queueDefinitionService.updateQueueCopyright(username, id, copyright);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "copyright");
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    /**
     * Change the language of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the language
     * of an existing queue.
     *
     * @param id                       The Id of the queue to update the status for.
     * @param language                 The updated queue language.
     * @param authentication           The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the language update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the language of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue language")
//    @ApiResponse(responseCode = "400", language = "Validation error in request body")
//    @ApiResponse(responseCode = "500", language = "Internal server error")
    @PutMapping(value = "/queues/{id}/language", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updateQueueLanguage(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the language for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue language", required = true)
            String language, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueLanguage for user={}, queueId={}, language={}", username, id, language);
        StopWatch stopWatch = StopWatch.createStarted();
        String updatedQueueLanguage = queueDefinitionService.updateQueueLanguage(username, id, language);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "language");
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    /**
     * Change the authentication requirement of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the authentication requirement
     * of an existing queue.
     *
     * @param id                             The Id of the queue to fetch.
     * @param queueAuthUpdateRequest          The request containing the updated authentication requirement.
     * @param authentication                 The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the language update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue update data.
     */
    @Operation(summary = "Change the authentication requirements of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue authentication requirement")
    @PutMapping(value = "/queues/{id}/auth", produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> updateQueueAuthRequirement(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the authentication requirement for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated authentication requirement", required = true)
            QueueAuthUpdateRequest queueAuthUpdateRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueAuthRequirement for user={}, queue={}, queueAuthUpdateRequest={}", username, id, queueAuthUpdateRequest);
        StopWatch stopWatch = createStarted();
        boolean isRequired = queueAuthUpdateRequest.getIsRequired();
        Boolean updatedAuthRequirement = queueDefinitionService.updateQueueAuthenticationRequirement(username, id, isRequired);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "isAuthenticated");
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    /**
     * Change the image source of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the image source
     * of an existing queue.
     *
     * @param id                  The Id of the queue to fetch.
     * @param imgSource           The source for the queue thumbnail image.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the image source update.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Change the image source of an existing queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully updated queue image source")
    @PutMapping(value = "/queues/{id}/imgsrc", produces = {APPLICATION_JSON_VALUE}, consumes = {TEXT_PLAIN_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> updateQueueImageSource(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to update the image source for", required = true)
            Long id,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated image source", required = true)
            String imgSource, // TODO: fix this
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueImageSource for user={}, queue={}, imgSourceLen={}", username, id, length(imgSource));
        StopWatch stopWatch = createStarted();
        String updatedQueueImgSource = queueDefinitionService.updateQueueImageSource(username, id, imgSource);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "queueImgSrc");
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
    }

    //
    // DELETE QUEUE
    //

    /**
     * Delete a queue.
     *
     * @param id             The Id of the queue to be deleted.
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a queue", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue")
//    @ApiResponse(responseCode = "500", description = "Internal server error")
    @DeleteMapping(value = "/queues/{id}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueue(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue to be deleted", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueue for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.updateQueuePubStatus(username, id, DEPUB_PENDING);
        queueDefinitionService.deleteById(username, id);
        stopWatch.stop();
        appLogService.logQueueDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted queue Id " + id));
    }

    //
    // DELETE QUEUE INDIVIDUAL FIELDS
    //

    /**
     * Delete the title for a queue given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the title from a queue identified by its Id.
     *
     * @param id                   The Id of the queue to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the title from a queue given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue title")
    @DeleteMapping(value = "/queues/{id}/title", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueueTitle(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue with the title to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueTitle for user={}, queueId={}", username, id);
        StopWatch stopWatch = createStarted();
        queueDefinitionService.clearQueueTitle(username, id);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "title");
        return ok().body(buildResponseMessage("Deleted title from queue Id " + id));
    }

    /**
     * Delete the description for a queue given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the description from a queue identified by its Id.
     *
     * @param id                   The Id of the queue to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the description from a queue given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue description")
    @DeleteMapping(value = "/queues/{id}/desc", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueueDescription(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue with the description to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueDescription for user={}, queueId={}", username, id);
        StopWatch stopWatch = createStarted();
        queueDefinitionService.clearQueueDescription(username, id);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "description");
        return ok().body(buildResponseMessage("Deleted description from queue Id " + id));
    }

    /**
     * Delete the generator for a queue given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the generator from a queue identified by its Id.
     *
     * @param id                   The Id of the queue to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the generator from a queue given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue generator")
    @DeleteMapping(value = "/queues/{id}/generator", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueueGenerator(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue with the generator to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueGenerator for user={}, queueId={}", username, id);
        StopWatch stopWatch = createStarted();
        queueDefinitionService.clearQueueGenerator(username, id);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "generator");
        return ok().body(buildResponseMessage("Deleted generator from queue Id " + id));
    }

    /**
     * Delete the copyright for a queue given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the copyright from a queue identified by its Id.
     *
     * @param id                   The Id of the queue to delete.
     * @param authentication       The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the copyright from a queue given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue copyright")
    @DeleteMapping(value = "/queues/{id}/copyright", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueueCopyright(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue with the copyright to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueCopyright for user={}, queueId={}", username, id);
        StopWatch stopWatch = createStarted();
        queueDefinitionService.clearQueueCopyright(username, id);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "copyright");
        return ok().body(buildResponseMessage("Deleted copyright from queue Id " + id));
    }

    /**
     * Delete the image source for a queue given by Id.
     *
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete
     * the image source from a queue identified by its Id.
     *
     * @param id                  The Id of the queue to fetch.
     * @param authentication      The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the image source from a queue given by Id", security = @SecurityRequirement(name = "VERIFIED_ROLE"))
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue image source")
    @DeleteMapping(value = "/queues/{id}/imgsrc", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<ResponseMessage> deleteQueueImageSource(
            @PathVariable("id")
            @Parameter(description = "The Id of the queue with the image source to delete", required = true)
            Long id,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueImageSource for user={}, queue={}", username, id);
        StopWatch stopWatch = createStarted();
        queueDefinitionService.clearQueueImageSource(username, id);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "queueImgSrc");
        return ok().body(buildResponseMessage("Deleted queue image from queue Id " + id));
    }
}
