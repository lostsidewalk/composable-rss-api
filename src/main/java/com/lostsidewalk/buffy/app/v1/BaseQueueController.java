package com.lostsidewalk.buffy.app.v1;

import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.model.v1.response.QueueConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.publisher.Publisher;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.ZoneId;
import java.util.Map;

import static org.apache.commons.lang3.time.StopWatch.createStarted;
import static org.springframework.http.ResponseEntity.ok;

@Slf4j
public class BaseQueueController {

    protected static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    PostPublisher postPublisher;

    @Autowired
    Validator validator;

    protected final ResponseEntity<QueueConfigResponse> finalizeDeleteEntity(String username, StopWatch deleteTimer, Long queueId, String attrName) throws DataAccessException, DataUpdateException {
        StopWatch finalizeTimer = createStarted();
        Map<String, Publisher.PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueDefinition queueDefinition = queueDefinitionService.findByQueueId(username, queueId);
        QueueConfigResponse queueConfigResponse = prepareResponse(queueDefinition, pubResults);
        finalizeTimer.stop();
        AppLogService.logQueueAttributeDelete(username, deleteTimer, finalizeTimer, queueId, attrName, pubResults);
        return ok().body(queueConfigResponse);
    }

    protected final ResponseEntity<QueueConfigResponse> finalizeUpdateEntity(String username, StopWatch updateTimer, QueueDefinition queueDefinition, String attrName) throws DataAccessException, DataUpdateException {
        StopWatch finalizeTimer = createStarted();
        long queueId = queueDefinition.getId();
        Map<String, Publisher.PubResult> pubResults = postPublisher.publishFeed(username, queueId);
        QueueConfigResponse queueConfigResponse = prepareResponse(queueDefinition, pubResults);
        finalizeTimer.stop();
        AppLogService.logQueueAttributeUpdate(username, updateTimer, finalizeTimer, queueId, attrName, pubResults);
        return ok().body(queueConfigResponse);
    }

    protected final QueueConfigResponse prepareResponse(QueueDefinition updatedQueue, Map<String, Publisher.PubResult> pubResults) {
        QueueDTO queueDTO = QueueDTO.from(updatedQueue);
        QueueConfigResponse queueConfigResponse = QueueConfigResponse.from(queueDTO, pubResults);
        validator.validate(queueConfigResponse);
        return queueConfigResponse;
    }

    @Override
    public final String toString() {
        return "BaseQueueController{" +
                "queueDefinitionService=" + queueDefinitionService +
                ", stagingPostService=" + stagingPostService +
                ", postPublisher=" + postPublisher +
                ", validator=" + validator +
                '}';
    }

    protected final QueueDefinitionService getQueueDefinitionService() {
        return queueDefinitionService;
    }

    protected final StagingPostService getStagingPostService() {
        return stagingPostService;
    }

    protected final PostPublisher getPostPublisher() {
        return postPublisher;
    }

    protected final Validator getValidator() {
        return validator;
    }
}
