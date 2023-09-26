package com.lostsidewalk.buffy.app.v1.queue;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.queue.QueueDefinition;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

@Slf4j
@RestController
@Validated
public class QueueController_Retrieve extends QueueController {

    //
    // RETRIEVE QUEUES
    //

    /**
     * Get all queue definitions for the authenticated user.
     *
     * @param offset         The number of items to skip before returning results.
     * @param limit          The maximum number of items to return.
     * @param ifNoneMatch    if-none-match HTTP header value (for e-tag evaluation).
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity containing the fetched queue definitions.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all queue definitions")
    @GetMapping(value = "/${api.version}/queues", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue definitions",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = QueueDTO.class))))
    public ResponseEntity<List<QueueDTO>> getQueues(
            //
            @Parameter(name = "The number of items to skip before returning results")
            @Valid @RequestParam(name = "offset", required = false)
            @Positive
            Integer offset,
            //
            @Parameter(name = "The maximum number of items to return")
            @Valid @RequestParam(name = "limit", required = false)
            @Positive
            Integer limit,
            //
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueues for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        // queue definitions
        List<QueueDefinition> queueDefinitions = queueDefinitionService.findByUser(username);
        String eTag = eTagger.computeEtag(queueDefinitions);
        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            return status(NOT_MODIFIED).build();
        }
        List<QueueDTO> queues = queueDefinitions.stream().map(QueueController::prepareQueueDTO).toList();
        if (isNotEmpty(queues)) {
            queues = paginator.paginate(queues, offset, limit);
            validator.validate(queues);
        }
        stopWatch.stop();
        appLogService.logQueueFetch(username, stopWatch, size(queueDefinitions));
        return ok()
                .eTag(eTag)
                .body(queues);
    }

    /**
     * Get a queue definition by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param ifNoneMatch    if-none-match HTTP header value (for e-tag evaluation).
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity containing the fetched queue definition.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get a queue definition by identifier")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue definition",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QueueDTO.class)))
    public ResponseEntity<QueueDTO> getQueueById(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch", required = true)
            String queueIdent,
            //
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            //
            Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueById for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = StopWatch.createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        // queue definitions
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        String eTag = eTagger.computeEtag(queueDefinition);
        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            return status(NOT_MODIFIED).build();
        }
        QueueDTO queue = prepareQueueDTO(queueDefinition);
        validator.validate(queue);
        stopWatch.stop();
        appLogService.logQueueFetch(username, stopWatch, 1);
        return ok()
                .eTag(eTag)
                .body(queue);
    }

    //
    // RETRIEVE QUEUE INDIVIDUAL FIELDS
    //

    /**
     * Get the title of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the title of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue title.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the title of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue title")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/title", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueTitle(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the title from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueTitle for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        String queueTitle = queueDefinition.getTitle();
        queueTitle = (queueTitle == null ? EMPTY : queueTitle);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "title");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueTitle));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueTitle, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the description of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get description of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue description.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the description of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue description")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/description", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueDescription(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the description from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueDescription for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        String queueDescription = queueDefinition.getDescription();
        queueDescription = (queueDescription == null ? EMPTY : queueDescription);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "description");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueDescription));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDescription, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the generator of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the generator of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue generator.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the generator of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue generator")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/generator", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueGenerator(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the generator from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueGenerator for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        String queueGenerator = queueDefinition.getGenerator();
        queueGenerator = (queueGenerator == null ? EMPTY : queueGenerator);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "generator");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueGenerator));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueGenerator, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the transport identifier of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the transport identifier of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue transport identifier.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the transport identifier of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue transport identifier")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/transport", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueTransport(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the transport identifier from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueTransport for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "transportIdent");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getTransportIdent()));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getTransportIdent(), headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the copyright of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the copyright of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue copyright.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the copyright of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue copyright")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/copyright", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueCopyright(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the copyright from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueCopyright for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        String queueCopyright = queueDefinition.getCopyright();
        queueCopyright = (queueCopyright == null ? EMPTY : queueCopyright);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "copyright");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueCopyright));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueCopyright, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the language of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the language of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue language.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the language of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue language")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/language", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueLanguage(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the language from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueLanguage for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "language");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getLanguage()));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getLanguage(), headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the deployed timestamp of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the deployed timestamp of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue deployed timestamp.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the deployed timestamp of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue deployed timestamp")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/deployed", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueDeployedTimestamp(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the deployed timestamp from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueDeployedTimestamp for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        Date lastDeployedTimestamp = queueDefinition.getLastDeployed();
        String responseStr = lastDeployedTimestamp != null ?
                ISO_8601_TIMESTAMP_FORMAT.format(lastDeployedTimestamp) :
                null; // default response
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "lastDeployed");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the authentication requirement of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the authentication requirement of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue authentication requirement.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the authentication requirement of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue authentication requirement")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/auth", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueAuthRequirement(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the authentication requirement from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueAuthRequirement for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        String responseStr = Boolean.toString(isTrue(queueDefinition.getIsAuthenticated()));
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "isAuthenticated");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    /**
     * Get the image source of a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to get the image source of a
     * queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the queue image source.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get the image source of a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched queue image source")
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/imgsrc", produces = {TEXT_PLAIN_VALUE, APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<String> getQueueImageSource(
            @RequestHeader(value = "Accept", required = false, defaultValue = APPLICATION_JSON_VALUE) String acceptHeader,
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch the image source from", required = true)
            String queueIdent,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getQueueImageSource for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        String queueImgSrc = queueDefinition.getQueueImgSrc();
        queueImgSrc = (queueImgSrc == null ? EMPTY : queueImgSrc);
        stopWatch.stop();
        appLogService.logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "queueImgSrc");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueImgSrc));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueImgSrc, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build();
        }
    }

    //
    // RETRIEVE POSTS
    //

    /**
     * Get all posts in the queue given by its Id.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to retrieve all
     * posts in a queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to fetch posts from.
     * @param offset         The number of items to skip before returning results.
     * @param limit          The maximum number of items to return.
     * @param ifNoneMatch    if-none-match HTTP header value (for e-tag evaluation).
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity containing the fetched posts.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Get all posts in the queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully fetched posts",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostDTO.class))))
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/posts", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostDTO>> getPosts(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch posts from", required = true)
            String queueIdent,
            //
            @Parameter(name = "The number of items to skip before returning results")
            @Valid @RequestParam(name = "offset", required = false)
            @Positive
            Integer offset,
            //
            @Parameter(name = "The maximum number of items to return")
            @Valid @RequestParam(name = "limit", required = false)
            @Positive
            Integer limit,
            //
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            //
            Authentication authentication
    ) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getPosts for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        List<StagingPost> stagingPosts = stagingPostService.getStagingPosts(username, singletonList(queueId));
        String eTag = eTagger.computeEtag(stagingPosts);
        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            return status(NOT_MODIFIED).build();
        }
        List<PostDTO> posts = stagingPosts.stream().map(p -> preparePostDTO(p, queueIdent)).toList();
        if (isNotEmpty(posts)) {
            posts = paginator.paginate(posts, offset, limit);
            validator.validate(posts);
        }
        stopWatch.stop();
        appLogService.logStagingPostFetch(username, stopWatch, 1, size(stagingPosts));
        return ok()
                .eTag(eTag)
                .body(posts);
    }
}
