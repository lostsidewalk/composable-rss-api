package com.lostsidewalk.buffy.app.v1.queue;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataConflictException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.request.QueueAuthUpdateRequest;
import com.lostsidewalk.buffy.app.model.v1.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.lostsidewalk.buffy.app.utils.HttpUtils.isPatch;
import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.StringUtils.length;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Slf4j
@RestController
@Validated
public class QueueController_Update extends QueueController {

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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = StopWatch.createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        QueueDefinition queueDefinition = queueDefinitionService.updateQueue(username, queueId, queueConfigRequest, isPatch(httpMethod));
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueUpdate(username, stopWatch, queueId, pubResults);
        return ok(queueConfigResponse);
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/ident", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> updateQueueIdent(
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
        StopWatch stopWatch = StopWatch.createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        String updatedQueueIdent = queueDefinitionService.updateQueueIdent(username, id, ident);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "ident", pubResults);
        return ok().body(buildResponseMessage("Successfully updated queue Id " + id));
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/title", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
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
        StopWatch stopWatch = StopWatch.createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        String updatedQueueTitle = queueDefinitionService.updateQueueTitle(username, id, title);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "title", pubResults);
        return ok().body(queueConfigResponse);
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/description", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
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
        StopWatch stopWatch = StopWatch.createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        String updatedQueueDescription = queueDefinitionService.updateQueueDescription(username, id, description);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "description", pubResults);
        return ok().body(queueConfigResponse);
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/generator", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
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
        StopWatch stopWatch = StopWatch.createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        String updatedQueueGenertor = queueDefinitionService.updateQueueGenerator(username, id, generator);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "generator", pubResults);
        return ok().body(queueConfigResponse);
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/copyright", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
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
        StopWatch stopWatch = StopWatch.createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        String updatedQueueCopyright = queueDefinitionService.updateQueueCopyright(username, id, copyright);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "copyright", pubResults);
        return ok().body(queueConfigResponse);
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/language", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
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
        StopWatch stopWatch = StopWatch.createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        String updatedQueueLanguage = queueDefinitionService.updateQueueLanguage(username, id, language);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "language", pubResults);
        return ok().body(queueConfigResponse);
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/auth", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        boolean isRequired = queueAuthUpdateRequest.getIsRequired();
        Boolean updatedAuthRequirement = queueDefinitionService.updateQueueAuthenticationRequirement(username, id, isRequired);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "isAuthenticated", pubResults);
        return ok().body(queueConfigResponse);
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
    @RequestMapping(value = "/${api.version}/queues/{queueIdent}/imgsrc", method = {PUT, PATCH}, produces = {APPLICATION_JSON_VALUE}, consumes = {ALL_VALUE})
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
        StopWatch stopWatch = createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        String updatedQueueImgSource = queueDefinitionService.updateQueueImageSource(username, id, imgSource);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeUpdate(username, stopWatch, id, "queueImgSrc", pubResults);
        return ok().body(queueConfigResponse);
    }
}
