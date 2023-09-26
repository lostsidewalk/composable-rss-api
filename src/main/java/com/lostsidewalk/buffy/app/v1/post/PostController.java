package com.lostsidewalk.buffy.app.v1.post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostsidewalk.buffy.app.audit.AppLogService;
import com.lostsidewalk.buffy.app.etag.ETagger;
import com.lostsidewalk.buffy.app.model.v1.response.PostDTO;
import com.lostsidewalk.buffy.app.paginator.Paginator;
import com.lostsidewalk.buffy.app.post.StagingPostService;
import com.lostsidewalk.buffy.app.queue.QueueDefinitionService;
import com.lostsidewalk.buffy.post.StagingPost;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;

/**
 * Controller class for managing post-related operations.
 *
 * This controller provides endpoints for managing posts within queues. Authenticated
 * users with the "VERIFIED_ROLE" have access to these operations.
 */
class PostController {

    @Autowired
     AppLogService appLogService;

    @Autowired
    StagingPostService stagingPostService;

    @Autowired
    QueueDefinitionService queueDefinitionService;

    @Autowired
    Paginator paginator;

    @Autowired
    Validator validator;

    @Autowired
    ETagger eTagger;

    protected static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    protected static final SimpleDateFormat ISO_8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

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
