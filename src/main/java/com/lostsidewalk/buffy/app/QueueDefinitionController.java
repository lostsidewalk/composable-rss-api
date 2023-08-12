package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.feed.QueueDefinitionService;
import com.lostsidewalk.buffy.app.model.request.FeedStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.response.QueueFetchResponse;
import com.lostsidewalk.buffy.app.model.response.ThumbnailConfigResponse;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.thumbnail.ThumbnailService;
import com.lostsidewalk.buffy.model.RenderedThumbnail;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.lostsidewalk.buffy.app.ResponseMessageUtils.buildResponseMessage;
import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static com.lostsidewalk.buffy.app.utils.ThumbnailUtils.getImage;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static java.util.Optional.ofNullable;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class QueueDefinitionController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    ThumbnailService thumbnailService;

    @Autowired
    Validator validator;

    @Value("${comprss.thumbnail.size}")
    int thumbnailSize;
    //
    // get feed definitions
    //
    @GetMapping("/queues")
    @Secured({VERIFIED_ROLE})
    public ResponseEntity<QueueFetchResponse> getQueues(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("getFeeds for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        // feed definitions
        List<QueueDefinition> feedDefinitions = queueDefinitionService.findByUser(username);
        stopWatch.stop();
        appLogService.logFeedFetch(username, stopWatch, size(feedDefinitions));
        return ok(
                QueueFetchResponse.from(feedDefinitions)
        );
    }
    //
    // create feed definitions
    //
    @PostMapping("/queues/")
    @Secured({VERIFIED_ROLE})
//    @Transactional
    public ResponseEntity<List<QueueConfigResponse>> createQueue(@RequestBody List<@Valid QueueConfigRequest> queueConfigRequests, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("createFeed adding {} feeds for user={}", size(queueConfigRequests), username);
        StopWatch stopWatch = StopWatch.createStarted();
        List<QueueDefinition> createdQueues = new ArrayList<>();
        List<QueueConfigResponse> queueConfigResponse = new ArrayList<>();
        // for ea. feed config request
        for (QueueConfigRequest queueConfigRequest : queueConfigRequests) {
            // create the feed
            Long queueId = queueDefinitionService.createFeed(username, queueConfigRequest);
            // re-fetch this feed definition
            QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
            createdQueues.add(queueDefinition);
            // build feed config responses to return the front-end
            queueConfigResponse.add(QueueConfigResponse.from(
                    queueDefinition,
                    buildThumbnail(queueDefinition))
            );
        }
        stopWatch.stop();
        appLogService.logFeedCreate(username, stopWatch, size(queueConfigRequests), size(createdQueues));

        return ok(queueConfigResponse);
    }
    //
    // update feed definition
    //
    @PutMapping("/queues/{id}")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<QueueConfigResponse> updateQueue(@PathVariable("id") Long id, @Valid @RequestBody QueueConfigRequest queueConfigRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeed for user={}, queueId={}", username, id);
        StopWatch stopWatch = StopWatch.createStarted();
        // (1) update the feed
        queueDefinitionService.updateFeed(username, id, queueConfigRequest);
        // (2) re-fetch this feed definition and query definitions and return to front-end
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, id);
        // (4) thumbnail the feed
        byte[] thumbnail = buildThumbnail(queueDefinition);
        stopWatch.stop();
        appLogService.logFeedUpdate(username, stopWatch, id);
        return ok(QueueConfigResponse.from(
                queueDefinition,
                thumbnail)
        );
    }
    //
    // update feed status
    //
    /**
     * ENABLED -- mark the feed for import
     * DISABLED -- un-mark the feed for import
     */
    @PutMapping("/queues/status/{id}")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updateFeedStatus(@PathVariable("id") Long id, @Valid @RequestBody FeedStatusUpdateRequest feedStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updateFeedStatus for user={}, postId={}, feedStatusUpdateRequest={}", username, id, feedStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        queueDefinitionService.updateFeedStatus(username, id, feedStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logFeedStatusUpdate(username, stopWatch, id, feedStatusUpdateRequest, 1);
        return ok().body(buildResponseMessage("Successfully updated feed Id " + id));
    }
    //
    // preview thumbnail
    //
    @PostMapping("/queues/thumbnail")
    @Secured({VERIFIED_ROLE})
    public ResponseEntity<ThumbnailConfigResponse> previewThumbnailConfig(@RequestParam("file") MultipartFile imageFile, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("previewThumbnailConfig for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        byte[] image = null;
        List<String> errors = new ArrayList<>();
        try (InputStream is = imageFile.getInputStream()) {
            image = getImage(imageFile.getOriginalFilename(), is.readAllBytes(), this.thumbnailSize);
        } catch (IOException e) {
            errors.add(imageFile.getOriginalFilename() + ": " + e.getMessage());
        }
        try {
            validateThumbnail(image);
        } catch (ValidationException e) {
            errors.add(e.getMessage());
        }
        stopWatch.stop();
        appLogService.logThumbnailPreview(username, stopWatch, size(errors));

        return ok(ThumbnailConfigResponse.from(encodeBase64String(image), errors));
    }

    private void validateThumbnail(byte[] image) {
        if (image == null) {
            throw new ValidationException("This format isn't supported.");
        }
    }
    //
    // randomize kitten
    //
    @GetMapping("/queues/thumbnail/random")
    @Secured({VERIFIED_ROLE})
    public ResponseEntity<ThumbnailConfigResponse> previewRandomThumbnail(Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("previewRandomThumbnail for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        String randomEncodedImage = thumbnailService.getRandom();
        stopWatch.stop();
        appLogService.logRandomThumbnailPreview(username, stopWatch);
        return ok(ThumbnailConfigResponse.from(randomEncodedImage));

    }
    //
    // delete feed
    //
    @DeleteMapping("/queues/{id}")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> deleteFeedById(@PathVariable Long id, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("deleteFeedById for user={}", username);
        StopWatch stopWatch = StopWatch.createStarted();
        stagingPostService.updateQueuePubStatus(username, id, DEPUB_PENDING);
        queueDefinitionService.deleteById(username, id);
        stopWatch.stop();
        appLogService.logFeedDelete(username, stopWatch, 1);
        return ok().body(buildResponseMessage("Deleted feed Id " + id));
    }

    //

    private byte[] buildThumbnail(QueueDefinition f) throws DataAccessException {
        if (isNotBlank(f.getQueueImgSrc())) {
            String transportIdent = f.getQueueImgTransportIdent();
            byte[] image = ofNullable(thumbnailService.getThumbnail(transportIdent)).map(RenderedThumbnail::getImage).orElse(null);
            if (image == null) {
                image = ofNullable(thumbnailService.refreshThumbnailFromSrc(transportIdent, f.getQueueImgSrc()))
                        .map(RenderedThumbnail::getImage)
                        .orElse(null);
            }

            return image;
        }

        return null;
    }
}
