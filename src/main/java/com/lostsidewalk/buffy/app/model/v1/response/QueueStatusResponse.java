package com.lostsidewalk.buffy.app.model.v1.response;

import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * A response model for queue status operations.
 */
@Slf4j
@Data
public class QueueStatusResponse {

    /**
     * The number of published posts in the queue.
     */
    int publishedCt;

    /**
     * The number of posts inthe queue, grouped by PostPubStatus (i.e., PUB_PENDING, DEPUB_PENDING, ARCHIVED, etc.).
     */
    Map<PostPubStatus, Integer> countByStatus;

    private QueueStatusResponse(int publishedCt, Map<PostPubStatus, Integer> countByStatus) {
        this.publishedCt = publishedCt;
        this.countByStatus = countByStatus;
    }

    /**
     * Static factory method to convert the post-status counts into QueueStatusResponse data transfer objects.
     *
     * @param publishedCt       A count of the number of published posts in a queue.
     * @param countByStatus     A count of the number of posts in a queue, grouped by status.
     * @return a QueueStatusResponse object
     */
    public static QueueStatusResponse from(int publishedCt, Map<PostPubStatus, Integer> countByStatus) {
        return new QueueStatusResponse(publishedCt, countByStatus);
    }
}
