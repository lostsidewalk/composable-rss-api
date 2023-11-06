package com.lostsidewalk.buffy.app.v1.queue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.etag.ETagger;
import com.lostsidewalk.buffy.app.model.v1.request.PostConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.request.QueueAuthUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.*;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils;
import com.lostsidewalk.buffy.app.v1.BaseQueueController;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.lostsidewalk.buffy.app.audit.AppLogService.*;
import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.PUB_PENDING;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.length;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * Controller class for managing queue-related operations.
 * <p>
 * This controller provides endpoints for managing queue and subsidiary entities. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
@SuppressWarnings("DesignForExtension")
@Slf4j
@RestController
@Validated
public class QueueController extends BaseQueueController {

    //
    // CREATE QUEUES
    //

    /**
     * Create a new feed queue with the provided configuration.
     * <p>
     * This endpoint is used to create a new feed queue with the provided configuration.
     * A feed queue is a structure used to manage, organize, and publish syndicated content in various formats
     * such as RSS 2.0, ATOM, and JSON.
     *
     * @param queueConfigRequest A queue configuration request.
     * @param authentication     The authenticated user's details.
     * @return A ResponseEntity containing a QueueConfigResponse with creation and deployment details about the created queue.
     * @throws DataAccessException   If there's an issue accessing data.
     * @throws DataUpdateException   If there's an issue updating data.
     * @throws DataConflictException If there is a duplicate key.
     */
    @Operation(summary = "Create a new feed queue")
    @ApiResponse(responseCode = "201", description = "Successfully created queue",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QueueConfigResponse.class)))
    @PostMapping(value = "/${api.version}/queues", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueConfigResponse> createQueue(
            @RequestBody
            @Parameter(description = "A queue configuration request", required = true,
                    schema = @Schema(implementation = QueueConfigRequest.class))
            @Valid QueueConfigRequest queueConfigRequest,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createQueue adding queue for user={}", username);
        StopWatch stopWatch = createStarted();
        // create the queue
        Long queueId = getQueueDefinitionService().createQueue(username, queueConfigRequest);
        // re-fetch this queue definition
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        QueueDTO queueDTO = QueueDTO.from(queueDefinition);
        Map<String, PubResult> pubResults = getPostPublisher().publishFeed(username, queueId);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, pubResults);
        getValidator().validate(queueConfigResponse);
        String rss20Url = pubResults.get("RSS_20").getUserIdentUrl();
        URI createdLocation = URI.create(rss20Url);
        stopWatch.stop();
        logQueueCreate(username, stopWatch, pubResults);
        return created(createdLocation).body(queueConfigResponse);
    }

    //
    // CREATE POST
    //

    /**
     * Create new posts in the specified queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to create new posts
     * in a queue given by its Id.
     *
     * @param queueIdent            The identifier of the queue into which the new posts should be added.
     * @param postConfigRequests    A list of PostConfigRequests, representing new posts to add.
     * @param authentication        The authentication details of the user making the request.
     * @return a ResponseEntity containing the created posts
     * @throws DataAccessException   If there's an issue accessing data.
     * @throws DataUpdateException   If there's an issue updating data.
     * @throws DataConflictException If there is a duplicate key.
     */
    @Operation(summary = "Create new posts in the specified queue")
    @ApiResponse(responseCode = "201", description = "Successfully created posts",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = PostCreateResponse.class))))
    @PostMapping(value = "/${api.version}/queues/{queueIdent}/posts", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<PostCreateResponse> createPosts(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to create posts in", required = true)
            String queueIdent,
            //
            @RequestBody
            @Parameter(description = "List of post configuration requests", required = true,
                    schema = @Schema(implementation = PostConfigRequest.class))
            List<? extends @Valid PostConfigRequest> postConfigRequests,
            //
            Authentication authentication
    ) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createPosts adding {} posts for for user={}, queueIdent={}", size(postConfigRequests), username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        List<Long> createdPostIds = new ArrayList<>(size(postConfigRequests));
        for (PostConfigRequest postConfigRequest : postConfigRequests) {
            Long postId = getStagingPostService().createPost(username, queueId, postConfigRequest);
            createdPostIds.add(postId);
        }
        Map<String, PubResult> pubResults = null;
        if (getQueueDefinitionService().isAutoDeploy(username, queueId)) {
            List<StagingPost> updatedPosts = getStagingPostService().updatePostPubStatus(username, createdPostIds, PUB_PENDING);
            pubResults = getPostPublisher().publishFeed(username, queueId, updatedPosts); // TODO: unit test
        } // else post created in manual deployment mode (do nothing)
        URI createdLocation = URI.create("/posts/" + queueId);
        PostCreateResponse postCreateResponse = PostCreateResponse.from(createdPostIds, pubResults);
        getValidator().validate(postCreateResponse);
        stopWatch.stop();
        logStagingPostCreate(username, stopWatch, postConfigRequests.size(), size(createdPostIds));
        return created(createdLocation).body(postCreateResponse);
    }

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    private static final DateTimeFormatter ISO_8601_TIMESTAMP_FORMATTER = ISO_INSTANT;

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
    @GetMapping(value = "/${api.version}/queues", produces = APPLICATION_JSON_VALUE)
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
        StopWatch stopWatch = createStarted();
        // queue definitions
        List<QueueDefinition> queueDefinitions = getQueueDefinitionService().findByUser(username);
        String eTag = ETagger.computeEtag(queueDefinitions);
        if (eTag.equals(ifNoneMatch)) {
            return status(NOT_MODIFIED).build(); // TODO: unit test
        }
        List<QueueDTO> queues = queueDefinitions.stream().map(QueueDTO::from).toList();
        if (isNotEmpty(queues)) {
            queues = Paginator.paginate(queues, offset, limit);
            getValidator().validate(queues);
        }
        stopWatch.stop();
        logQueueFetch(username, stopWatch, size(queueDefinitions));
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
    @GetMapping(value = "/${api.version}/queues/{queueIdent}", produces = APPLICATION_JSON_VALUE)
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
        StopWatch stopWatch = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        // queue definitions
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        String eTag = ETagger.computeEtag(queueDefinition);
        if (eTag.equals(ifNoneMatch)) {
            return status(NOT_MODIFIED).build(); // TODO: unit test
        }
        QueueDTO queue = QueueDTO.from(queueDefinition);
        getValidator().validate(queue);
        stopWatch.stop();
        logQueueFetch(username, stopWatch, 1);
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        String queueTitle = queueDefinition.getTitle();
        queueTitle = (queueTitle == null ? EMPTY : queueTitle);
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "title");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueTitle));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueTitle, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        String queueDescription = queueDefinition.getDescription();
        queueDescription = (queueDescription == null ? EMPTY : queueDescription);
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "description");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueDescription));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDescription, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        String queueGenerator = queueDefinition.getGenerator();
        queueGenerator = (queueGenerator == null ? EMPTY : queueGenerator);
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "generator");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueGenerator));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueGenerator, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "transportIdent");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getTransportIdent()));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getTransportIdent(), headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        String queueCopyright = queueDefinition.getCopyright();
        queueCopyright = (queueCopyright == null ? EMPTY : queueCopyright);
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "copyright");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueCopyright));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueCopyright, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "language");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueDefinition.getLanguage()));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueDefinition.getLanguage(), headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        String responseStr = Optional.ofNullable(queueDefinition.getLastDeployed())
                .map(Date::toInstant)
                .map(instant -> ZonedDateTime.ofInstant(instant, ZONE_ID))
                .map(ISO_8601_TIMESTAMP_FORMATTER::format)
                .orElse(null);
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "lastDeployed");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        String responseStr = Boolean.toString(isTrue(queueDefinition.getIsAuthenticated()));
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "isAuthenticated");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(responseStr));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(responseStr, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = getQueueDefinitionService().findByQueueId(username, queueId);
        String queueImgSrc = queueDefinition.getQueueImgSrc();
        queueImgSrc = (queueImgSrc == null ? EMPTY : queueImgSrc);
        stopWatch.stop();
        logQueueAttributeFetch(username, stopWatch, queueDefinition.getId(), "queueImgSrc");
        if (acceptHeader.contains(APPLICATION_JSON_VALUE) || acceptHeader.contains(ALL_VALUE)) {
            return ok(GSON.toJson(queueImgSrc));
        } else if (acceptHeader.contains(TEXT_PLAIN_VALUE)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(TEXT_PLAIN);
            return new ResponseEntity<>(queueImgSrc, headers, OK);
        } else {
            return status(HttpStatusCode.valueOf(406)).build(); // TODO: unit test
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
     * @param status         Limit the results to posts with the provided status.
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
    @GetMapping(value = "/${api.version}/queues/{queueIdent}/posts", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<List<PostDTO>> getPosts(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to fetch posts from", required = true)
            String queueIdent,
            //
            @Parameter(name = "Limit the results to posts with the provided status")
            @RequestParam(name = "status", required = false)
            PostPubStatus status,
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
        log.debug("getPosts for user={}, queueIdent={}, status={}", username, queueIdent, status);
        StopWatch stopWatch = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        List<StagingPost> stagingPosts = getStagingPostService().getStagingPosts(username, singletonList(queueId), status);
        String eTag = ETagger.computeEtag(stagingPosts);
        if (eTag.equals(ifNoneMatch)) {
            return status(NOT_MODIFIED).build(); // TODO: unit test
        }
        List<PostDTO> posts = stagingPosts.stream().map(p -> PostDTO.from(p, queueIdent)).toList();
        if (isNotEmpty(posts)) {
            posts = Paginator.paginate(posts, offset, limit);
            getValidator().validate(posts);
        }
        stopWatch.stop();
        logStagingPostFetch(username, stopWatch, 1, size(stagingPosts));
        return ok()
                .eTag(eTag)
                .body(posts);
    }

    //
    // UPDATE QUEUE
    //

    /**
     * Update the properties of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the properties
     * of an existing queue given by its identifier. The queue's configuration properties are provided
     * in the request body.
     *
     * @param queueIdent         The identifier of the queue to be updated.
     * @param queueConfigRequest The updated queue configuration properties.
     * @param httpMethod         The HTTP method in use, either PATCH or PUT.
     * @param authentication     The authentication details of the user making the request.
     * @return A ResponseEntity containing the updated queue configuration along with a thumbnail.
     * @throws DataAccessException   If there's an issue accessing data.
     * @throws DataUpdateException   If there's an issue updating data.
     * @throws DataConflictException If there is a duplicate key.
     */
    @Operation(summary = "Update the properties of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue configuration",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = QueueDTO.class, name = "queue-dto", title = "queue-dto")))
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueue(
            //
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to be updated", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue configuration properties", required = true,
                    schema = @Schema(implementation = QueueConfigRequest.class))
            QueueConfigRequest queueConfigRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueue for user={}, queueIdent={}, httpMethod={}", username, queueIdent, httpMethod);
        StopWatch updateTimer = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueue(username, queueId, queueConfigRequest, isPatch(httpMethod));
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "queue");
    }

    //
    // UPDATE QUEUE INDIVIDUAL FIELDS
    //

    /**
     * Change the identifier of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the identifier
     * of an existing queue.
     *
     * @param queueIdent     The identifier of the queue to update.
     * @param ident          The updated queue identifier.
     * @param authentication The authentication details of the user making the request.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @return A ResponseEntity indicating the success of the identifier update.
     * @throws DataAccessException   If there's an issue accessing data.
     * @throws DataUpdateException   If there's an issue updating data.
     * @throws DataConflictException If there is a duplicate key.
     */
    @Operation(summary = "Change the identifier of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue identifier")
//    @ApiResponse(responseCode = "400", description = "Validation error in request body")
//    @ApiResponse(responseCode = "500", description = "Internal server error")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/ident", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueueIdent(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the identifier for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue identifier", required = true)
            String ident,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException, DataConflictException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueIdent for user={}, queueIdent={}, ident={}, httpMethod={}", username, queueIdent, ident, httpMethod);
        StopWatch updateTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueueIdent(username, id, ident);
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "ident");
    }

    /**
     * Change the title of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the title
     * of an existing queue.
     *
     * @param queueIdent     The identifier of the queue to update the status for.
     * @param title          The updated queue title.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the title update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the title of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue title")
//    @ApiResponse(responseCode = "400", description = "Validation error in request body")
//    @ApiResponse(responseCode = "500", description = "Internal server error")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/title", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueueTitle(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the title for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue title", required = true)
            String title,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueTitle for user={}, queueIdent={}, title={}, httpMethod={}", username, queueIdent, title, httpMethod);
        StopWatch updateTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueueTitle(username, id, title);
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "title");
    }

    /**
     * Change the description of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the description
     * of an existing queue.
     *
     * @param queueIdent     The identifier of the queue to update the status for.
     * @param description    The updated queue description.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the description update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the description of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue description")
//    @ApiResponse(responseCode = "400", description = "Validation error in request body")
//    @ApiResponse(responseCode = "500", description = "Internal server error")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/description", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueueDescription(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the description for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue description", required = true)
            String description,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueDescription for user={}, queueIdent={}, description={}, httpMethod={}", username, queueIdent, description, httpMethod);
        StopWatch updateTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueueDescription(username, id, description);
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "description");
    }

    /**
     * Change the generator of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the generator
     * of an existing queue.
     *
     * @param queueIdent     The identifier of the queue to update the status for.
     * @param generator      The updated queue generator.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the generator update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the generator of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue generator")
//    @ApiResponse(responseCode = "400", generator = "Validation error in request body")
//    @ApiResponse(responseCode = "500", generator = "Internal server error")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/generator", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueueGenerator(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the generator for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue generator", required = true)
            String generator,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueGenerator for user={}, queueIdent={}, generator={}, httpMethod={}", username, queueIdent, generator, httpMethod);
        StopWatch updateTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueueGenerator(username, id, generator);
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "generator");
    }

    /**
     * Change the copyright of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the copyright
     * of an existing queue.
     *
     * @param queueIdent     The identifier of the queue to update the status for.
     * @param copyright      The updated queue copyright.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the copyright update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the copyright of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue copyright")
//    @ApiResponse(responseCode = "400", copyright = "Validation error in request body")
//    @ApiResponse(responseCode = "500", copyright = "Internal server error")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/copyright", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueueCopyright(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the copyright for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue copyright", required = true)
            String copyright,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueCopyright for user={}, queueIdent={}, copyright={}, httpMethod={}", username, queueIdent, copyright, httpMethod);
        StopWatch updateTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueueCopyright(username, id, copyright);
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "copyright");
    }

    /**
     * Change the language of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the language
     * of an existing queue.
     *
     * @param queueIdent     The identifier of the queue to update the status for.
     * @param language       The updated queue language.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the language update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Change the language of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue language")
//    @ApiResponse(responseCode = "400", language = "Validation error in request body")
//    @ApiResponse(responseCode = "500", language = "Internal server error")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/language", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueueLanguage(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the language for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated queue language", required = true)
            String language,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueLanguage for user={}, queueIdent={}, language={}, httpMethod={}", username, queueIdent, language, httpMethod);
        StopWatch updateTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueueLanguage(username, id, language);
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "language");
    }

    /**
     * Change the authentication requirement of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the authentication requirement
     * of an existing queue.
     *
     * @param queueIdent             The identifier of the queue to fetch.
     * @param queueAuthUpdateRequest The request containing the updated authentication requirement.
     * @param httpMethod             The HTTP method in use, either PATCH or PUT.
     * @param authentication         The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the language update.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue update data.
     */
    @Operation(summary = "Change the authentication requirements of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue authentication requirement")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/auth", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueConfigResponse> updateQueueAuthRequirement(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the authentication requirement for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated authentication requirement", required = true)
            QueueAuthUpdateRequest queueAuthUpdateRequest,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueAuthRequirement for user={}, queueIdent={}, queueAuthUpdateRequest={}, httpMethod={}", username, queueIdent, queueAuthUpdateRequest, httpMethod);
        StopWatch updateTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        boolean isRequired = queueAuthUpdateRequest.getIsRequired();
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueueAuthenticationRequirement(username, id, isRequired);
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "isAuthenticated");
    }

    /**
     * Change the image source of an existing queue.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to update the image source
     * of an existing queue.
     *
     * @param queueIdent     The identifier of the queue to fetch.
     * @param imgSource      The source for the queue thumbnail image.
     * @param httpMethod     The HTTP method in use, either PATCH or PUT.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the image source update.
     * @throws DataAccessException If there's an issue accessing data.
     */
    @Operation(summary = "Change the image source of an existing queue")
    @ApiResponse(responseCode = "200", description = "Successfully updated queue image source")
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/imgsrc", method = {PUT, PATCH}, produces = APPLICATION_JSON_VALUE, consumes = ALL_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueConfigResponse> updateQueueImageSource(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to update the image source for", required = true)
            String queueIdent,
            //
            @Valid
            @RequestBody
            @Parameter(description = "The updated image source", required = true)
            String imgSource,
            //
            HttpMethod httpMethod,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateQueueImageSource for user={}, queueIdent={}, imgSourceLen={}, httpMethod={}", username, queueIdent, length(imgSource), httpMethod);
        StopWatch updateTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        QueueDefinition updatedQueue = getQueueDefinitionService().updateQueueImageSource(username, id, imgSource);
        updateTimer.stop();
        return finalizeUpdateEntity(username, updateTimer, updatedQueue, "queueImgSrc");
    }

    /**
     * Delete all posts from a queue given by its identifier.
     * <p>
     * This endpoint allows authenticated users with the "VERIFIED_ROLE" to delete all
     * posts from a queue.
     *
     * @param queueIdent     The identifier of the queue from which all posts will be removed.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete all posts from a queue")
    @ApiResponse(responseCode = "200", description = "Successfully deleted posts")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/posts", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<PostDeleteResponse> deletePosts(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to delete posts from", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePosts for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getStagingPostService().deleteByQueueId(username, queueId);
        Map<String, PubResult> pubResults = getPostPublisher().publishFeed(username, queueId);
        PostDeleteResponse postDeleteResponse = PostDeleteResponse.from("Deleted posts from queue Id " + queueId, pubResults);
        getValidator().validate(postDeleteResponse);
        stopWatch.stop();
        logStagingPostsDelete(username, stopWatch, queueId);
        return ok().body(postDeleteResponse);
    }

    /**
     * Delete a queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to be deleted.
     * @param authentication The authenticated user's details.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessageUtils.ResponseMessage> deleteQueue(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to be deleted", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueue for user={}", username);
        StopWatch stopWatch = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getPostPublisher().unpublishFeed(username, id);
        getQueueDefinitionService().deleteById(username, id);
        stopWatch.stop();
        logQueueDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted queue Id " + id));
    }

    /**
     * Delete the title for a queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to delete the title from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the title from a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue title")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/title", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> deleteQueueTitle(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue with the title to delete", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueTitle for user={}, queueIdent={}", username, queueIdent);
        StopWatch deleteTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getQueueDefinitionService().clearQueueTitle(username, id);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, id, "title");
    }

    /**
     * Delete the description for a queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to delete the description from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the description from a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue description")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/description", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> deleteQueueDescription(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue with the description to delete", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueDescription for user={}, queueIdent={}", username, queueIdent);
        StopWatch deleteTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getQueueDefinitionService().clearQueueDescription(username, id);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, id, "description");
    }

    /**
     * Delete the generator for a queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to delete the generator from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the generator from a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue generator")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/generator", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> deleteQueueGenerator(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue with the generator to delete", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueGenerator for user={}, queueIdent={}", username, queueIdent);
        StopWatch deleteTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getQueueDefinitionService().clearQueueGenerator(username, id);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, id, "generator");
    }

    /**
     * Delete the copyright for a queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to delete the copyright from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the copyright from a queue given by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue copyright")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/copyright", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<QueueConfigResponse> deleteQueueCopyright(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue with the copyright to delete", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueCopyright for user={}, queueIdent={}", username, queueIdent);
        StopWatch deleteTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getQueueDefinitionService().clearQueueCopyright(username, id);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, id, "copyright");
    }

    /**
     * Delete the image source for a queue given by its identifier.
     *
     * @param queueIdent     The identifier of the queue to delete the image source from.
     * @param authentication The authentication details of the user making the request.
     * @return A ResponseEntity indicating the success of the delete operation.
     * @throws DataAccessException If there's an issue accessing data.
     * @throws DataUpdateException If there's an issue updating data.
     */
    @Operation(summary = "Delete the image source from a queue givne by its identifier")
    @ApiResponse(responseCode = "200", description = "Successfully deleted queue image source")
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/imgsrc", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    public ResponseEntity<QueueConfigResponse> deleteQueueImageSource(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue with the image source to delete", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueueImageSource for user={}, queueIdent={}", username, queueIdent);
        StopWatch deleteTimer = createStarted();
        long id = getQueueDefinitionService().resolveQueueId(username, queueIdent);
        getQueueDefinitionService().clearQueueImageSource(username, id);
        deleteTimer.stop();
        return finalizeDeleteEntity(username, deleteTimer, id, "queueImgSrc");
    }
}
