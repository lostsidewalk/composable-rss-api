package com.lostsidewalk.buffy.app.v1;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostsidewalk.buffy.DataAccessException;
import com.lostsidewalk.buffy.DataUpdateException;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.app.model.v1.response.PostConfigResponse;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Collections.singletonList;

@SuppressWarnings("AbstractClassWithoutAbstractMethods") // not instantiable
@Slf4j
public abstract class BasePostController {

    protected static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    PostPublisher postPublisher;

    @Autowired
    Validator validator;

    protected static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    protected static final DateTimeFormatter ISO_8601_TIMESTAMP_FORMATTER = ISO_INSTANT;

    //
    //
    //

    protected final Map<String, PubResult> reDeploy(StagingPost updatedPost) throws DataAccessException, DataUpdateException {
        String username = updatedPost.getUsername();
        long queueId = updatedPost.getQueueId();
        boolean isPublished = updatedPost.isPublished();
        Map<String, PubResult> pubResults = null;
        if (isPublished) {
            pubResults = postPublisher.publishFeed(username, queueId, singletonList(updatedPost)); // TODO: unit test
        }
        return pubResults;
    }

    protected final PostConfigResponse prepareResponse(StagingPost updatedPost, Map<String, PubResult> pubResults) throws DataAccessException {
        String username = updatedPost.getUsername();
        PostDTO postDTO = PostDTO.from(updatedPost, queueDefinitionService.resolveQueueIdent(username, updatedPost.getQueueId()));
        PostConfigResponse postConfigResponse = PostConfigResponse.from(postDTO, pubResults);
        validator.validate(postConfigResponse);
        return postConfigResponse;
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

    @Override
    public final String toString() {
        return "BasePostController{" +
                "queueDefinitionService=" + queueDefinitionService +
                ", stagingPostService=" + stagingPostService +
                ", postPublisher=" + postPublisher +
                ", validator=" + validator +
                '}';
    }
}
