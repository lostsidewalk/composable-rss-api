package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.response.PostFetchResponse;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.lostsidewalk.buffy.app.user.UserRoles.VERIFIED_ROLE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
@Validated
public class StagingPostController {

    @Autowired
    AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;
    //
    // get staging posts
    //
    @GetMapping("/staging")
    @Secured({VERIFIED_ROLE})
    public ResponseEntity<PostFetchResponse> getStagingPosts(@RequestParam(required = false) List<Long> queueIds, Authentication authentication) throws DataAccessException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
            log.debug("getStagingPosts for user={}, queueIds={}", username, isEmpty(queueIds) ? "all" : queueIds);
        StopWatch stopWatch = StopWatch.createStarted();
        List<StagingPost> stagingPosts = stagingPostService.getStagingPosts(username, queueIds);
        stopWatch.stop();
        appLogService.logStagingPostFetch(username, stopWatch, size(queueIds), size(stagingPosts));
        return ok(PostFetchResponse.from(stagingPosts));
    }

    /**
     * PUB_PENDING -- mark the post for publication, will be deployed with the feed
     * DEPUB_PENDING -- mark the post for de-publication, will be unpublished and excluded from future deployments
     * null -- clear the current post-pub status
     */
    @PutMapping("/staging/pub-status/{id}")
    @Secured({VERIFIED_ROLE})
    @Transactional
    public ResponseEntity<?> updatePostPubStatus(@PathVariable Long id, @Valid @RequestBody PostStatusUpdateRequest postStatusUpdateRequest, Authentication authentication) throws DataAccessException, DataUpdateException {
        UserDetails userDetails = (UserDetails) authentication.getDetails();
        String username = userDetails.getUsername();
        log.debug("updatePostStatus for user={}, postId={}, postStatusUpdateRequest={}", username, id, postStatusUpdateRequest);
        StopWatch stopWatch = StopWatch.createStarted();
        List<PubResult> publicationResults = stagingPostService.updatePostPubStatus(username, id, postStatusUpdateRequest);
        stopWatch.stop();
        appLogService.logStagingPostPubStatusUpdate(username, stopWatch, id, postStatusUpdateRequest, 1, publicationResults);
        return ok().body(DeployResponse.from(publicationResults));
    }
}
