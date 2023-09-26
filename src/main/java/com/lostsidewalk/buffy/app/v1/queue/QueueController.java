package com.lostsidewalk.buffy.app.v1.queue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostsidewalk.buffy.PostPublisher;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.etag.ETagger;
import com.lostsidewalk.buffy.app.model.v1.response.ExportConfigDTO;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
import com.lostsidewalk.buffy.app.model.v1.response.QueueDTO;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.post.StagingPost;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.text.SimpleDateFormat;

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

    protected static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    protected static final SimpleDateFormat ISO_8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Autowired
    AppLogService appLogService;

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    PostPublisher postPublisher;

    @Autowired
    Paginator paginator;

    @Autowired
    Validator validator;

    @Autowired
    ETagger eTagger;

    protected static QueueDTO prepareQueueDTO(QueueDefinition q) {
        Serializable exportConfig = q.getExportConfig();
        return QueueDTO.from(q.getId(),
                q.getIdent(),
                q.getTitle(),
                q.getDescription(),
                q.getGenerator(),
                q.getTransportIdent(),
                q.getIsAuthenticated(),
                exportConfig == null ? null : GSON.fromJson(GSON.toJson(exportConfig), ExportConfigDTO.class),
                q.getCopyright(),
                q.getLanguage(),
                q.getQueueImgSrc(),
                q.getLastDeployed(),
                q.getIsAuthenticated()
        );
    }

    protected static PostDTO preparePostDTO(StagingPost stagingPost, String queueIdent) {
        return PostDTO.from(stagingPost.getId(),
                queueIdent,
                stagingPost.getPostTitle(),
                stagingPost.getPostDesc(),
                stagingPost.getPostContents(),
                stagingPost.getPostITunes(),
                stagingPost.getPostUrl(),
                stagingPost.getPostUrls(),
                stagingPost.getPostComment(),
                stagingPost.getPostRights(),
                stagingPost.getContributors(),
                stagingPost.getAuthors(),
                stagingPost.getPostCategories(),
                stagingPost.getPublishTimestamp(),
                stagingPost.getExpirationTimestamp(),
                stagingPost.getEnclosures(),
                stagingPost.getLastUpdatedTimestamp(),
                stagingPost.isPublished());
    }
}
