package com.lostsidewalk.buffy.app.v1.queue;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.model.v1.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.ResponseMessage;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.transaction.Transactional;
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

import static com.lostsidewalk.buffy.app.utils.ResponseMessageUtils.buildResponseMessage;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

/**
 * Controller class for deleting queues and their attributes.
 */
@Slf4j
@RestController
@Validated
public class QueueController_Delete extends QueueController {

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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/posts", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deletePosts(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to delete posts from", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deletePosts for user={}, queueIdent={}", username, queueIdent);
        StopWatch stopWatch = createStarted();
        long queueId = queueDefinitionService.resolveQueueId(username, queueIdent);
        stagingPostService.deleteByQueueId(username, queueId);
        stopWatch.stop();
        appLogService.logStagingPostsDelete(username, stopWatch, queueId);
        return ok().body(buildResponseMessage("Deleted posts from queue Id " + queueId));
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}", produces = {APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('API_ROLE_VERIFIED')")
    @Transactional
    public ResponseEntity<ResponseMessage> deleteQueue(
            @PathVariable("queueIdent")
            @Parameter(description = "The identifier of the queue to be deleted", required = true)
            String queueIdent,
            Authentication authentication
    ) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteQueue for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        postPublisher.unpublishFeed(username, id);
        queueDefinitionService.deleteById(username, id);
        stopWatch.stop();
        appLogService.logQueueDelete(username, stopWatch, 1);
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/title", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueDefinitionService.clearQueueTitle(username, id);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "title", pubResults);
        return ok().body(queueConfigResponse);
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/description", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueDefinitionService.clearQueueDescription(username, id);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "description", pubResults);
        return ok().body(queueConfigResponse);
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/generator", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueDefinitionService.clearQueueGenerator(username, id);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "generator", pubResults);
        return ok().body(queueConfigResponse);
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/copyright", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueDefinitionService.clearQueueCopyright(username, id);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "copyright", pubResults);
        return ok().body(queueConfigResponse);
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
    @DeleteMapping(value = "/${api.version}/queues/{queueIdent}/imgsrc", produces = {APPLICATION_JSON_VALUE})
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
        StopWatch stopWatch = createStarted();
        long id = queueDefinitionService.resolveQueueId(username, queueIdent);
        queueDefinitionService.clearQueueImageSource(username, id);
        Map<String, PubResult> pubResults = postPublisher.publishFeed(username, id);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        QueueDTO queueDTO = prepareQueueDTO(queueDefinition);
        Map<String, DeployResponse> deployResponses = DeployResponse.from(pubResults);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, deployResponses);
        validator.validate(queueConfigResponse);
        stopWatch.stop();
        appLogService.logQueueAttributeDelete(username, stopWatch, id, "queueImgSrc", pubResults);
        return ok().body(queueConfigResponse);
    }
}
