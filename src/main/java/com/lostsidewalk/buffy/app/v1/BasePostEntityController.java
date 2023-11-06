package com.lostsidewalk.buffy.app.v1;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.publisher.Publisher;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;
import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;

@SuppressWarnings("NestedMethodCall")
@Slf4j
public abstract class BasePostEntityController<T> {

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    PostPublisher postPublisher;

    @Autowired
    Validator validator;

    //
    //
    //

    @SuppressWarnings("MethodReturnAlwaysConstant")
    protected abstract String getEntityContext();

    protected final ResponseEntity<PostConfigResponse> finalizeAddEntity(String username, StopWatch createTimer, Long postId, String entityIdent) throws DataAccessException, DataUpdateException {
        StopWatch finalizeTimer = createStarted();
        StagingPost updatedPost = stagingPostService.findById(username, postId);
        URI createdLocation = URI.create("/posts/" + postId + "/" + getEntityContext() + "/" + entityIdent);
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        finalizeTimer.stop();
        AppLogService.logStagingPostAddEntity(username, createTimer, finalizeTimer, postId, getEntityContext(), entityIdent);
        return created(createdLocation).body(postConfigResponse);
    }

    protected final ResponseEntity<PostConfigResponse> finalizeDeleteEntities(String username, StopWatch deleteTimer, StagingPost updatedPost) throws DataAccessException, DataUpdateException {
        StopWatch finalizeTimer = createStarted();
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        finalizeTimer.stop();
        AppLogService.logStagingPostEntitiesDelete(username, deleteTimer, finalizeTimer, updatedPost.getId(), getEntityContext());
        return ok().body(postConfigResponse);
    }

    protected final ResponseEntity<PostConfigResponse> finalizeDeleteEntity(String username, StopWatch deleteTimer, String entityIdent, StagingPost updatedPost) throws DataAccessException, DataUpdateException {
        StopWatch finalizeTimer = createStarted();
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        finalizeTimer.stop();
        AppLogService.logStagingPostEntityDelete(username, deleteTimer, finalizeTimer, updatedPost.getId(), getEntityContext(), entityIdent);
        return ok().body(postConfigResponse);
    }

    protected final ResponseEntity<List<T>> finalizeRetrieveEntities(String username, StopWatch retrieveTimer, Long postId, List<T> postEntities, Integer offset, Integer limit) {
        StopWatch finalizeTimer = createStarted();
        List<T> paginatedPostEntities = null;
        if (isNotEmpty(postEntities)) {
            paginatedPostEntities = Paginator.paginate(postEntities, offset, limit);
            validator.validate(paginatedPostEntities);
        }
        finalizeTimer.stop();
        AppLogService.logStagingPostEntitiesFetch(username, retrieveTimer, finalizeTimer, postId, getEntityContext(), size(paginatedPostEntities));
        return ok(paginatedPostEntities);
    }

    protected final ResponseEntity<T> finalizeRetrieveEntity(String username, StopWatch retrieveTimer, Long postId, String entityIdent, T entity) {
        StopWatch finalizeTimer = createStarted();
        if (entity != null) {
            validator.validate(entity);
        }
        finalizeTimer.stop();
        AppLogService.logStagingPostEntityFetch(username, retrieveTimer, finalizeTimer, postId, getEntityContext(), entityIdent);
        return ok(entity);
    }

    protected final ResponseEntity<PostConfigResponse> finalizeUpdateEntities(String username, StopWatch updateTimer, StagingPost updatedPost, int entityCt) throws DataAccessException, DataUpdateException {
        StopWatch finalizeTimer = createStarted();
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        finalizeTimer.stop();
        AppLogService.logStagingPostEntitiesUpdate(username, updateTimer, finalizeTimer, updatedPost.getId(), getEntityContext(), entityCt);
        return ok().body(postConfigResponse);
    }

    protected final ResponseEntity<PostConfigResponse> finalizeUpdateEntity(String username, StopWatch updateTimer, StagingPost updatedPost, String entityIdent) throws DataAccessException, DataUpdateException {
        StopWatch finalizeTimer = createStarted();
        PostConfigResponse postConfigResponse = prepareResponse(updatedPost, reDeploy(updatedPost));
        finalizeTimer.stop();
        AppLogService.logStagingPostEntityUpdate(username, updateTimer, finalizeTimer, updatedPost.getId(), getEntityContext(), entityIdent);
        return ok().body(postConfigResponse);
    }

    //
    //
    //

    private Map<String, Publisher.PubResult> reDeploy(StagingPost updatedPost) throws DataAccessException, DataUpdateException {
        String username = updatedPost.getUsername();
        long queueId = updatedPost.getQueueId();
        boolean isPublished = updatedPost.isPublished();
        Map<String, Publisher.PubResult> pubResults = null;
        if (isPublished) {
            pubResults = postPublisher.publishFeed(username, queueId, singletonList(updatedPost)); // TODO: unit test
        }
        return pubResults;
    }

    private PostConfigResponse prepareResponse(StagingPost updatedPost, Map<String, Publisher.PubResult> pubResults) throws DataAccessException {
        String username = updatedPost.getUsername();
        PostDTO postDTO = PostDTO.from(updatedPost, queueDefinitionService.resolveQueueIdent(username, updatedPost.getQueueId()));
        PostConfigResponse postConfigResponse = PostConfigResponse.from(postDTO, pubResults);
        validator.validate(postConfigResponse);
        return postConfigResponse;
    }

    protected final StagingPostService getStagingPostService() {
        return stagingPostService;
    }

    @Override
    public final String toString() {
        return "BasePostEntityController{" +
                "queueDefinitionService=" + queueDefinitionService +
                ", stagingPostService=" + stagingPostService +
                ", postPublisher=" + postPublisher +
                ", validator=" + validator +
                '}';
    }
}
