package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.post.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;

@Slf4j
@Data
public class PostDTO {

    /**
     * The unique identifier of the post.
     */
    Long id;

    /**
     * The ID of the queue to which the post belongs.
     */
    Long queueId;

    /**
     * The title of the post.
     */
    ContentObject postTitle;

    /**
     * The description of the post.
     */
    ContentObject postDesc;

    /**
     * A list of ContentObject objects (the post content)
     */
    List<ContentObject> postContents;

    /**
     * The post ITunes descriptor
     */
    PostITunes postITunes;

    /**
     * The (primary) post URL
     */
    String postUrl;

    /**
     * A list of alternate post URLs
     */
    List<PostUrl> postUrls;

    /**
     * The timestamp of when the post was imported.
     */
    Date importTimestamp;

    /**
     * The comment associated with the post.
     */
    String postComment;

    /**
     * The rights associated with the post.
     */
    String postRights;

    /**
     * The list of contributors to the post.
     */
    List<PostPerson> contributors;

    /**
     * The list of authors of the post.
     */
    List<PostPerson> authors;

    /**
     * The list of categories associated with the post.
     */
    List<String> postCategories;

    /**
     * The timestamp of when the post was published.
     */
    Date publishTimestamp;

    /**
     * The timestamp of when the post will expire.
     */
    Date expirationTimestamp;

    /**
     * The list of enclosures associated with the post.
     */
    List<PostEnclosure> enclosures;

    /**
     * The timestamp of the last update to the post.
     */
    Date lastUpdatedTimestamp;

    /**
     * Indicates whether the post is published.
     */
    boolean isPublished;

    private PostDTO(Long id, Long queueId, ContentObject postTitle, ContentObject postDesc,
                    List<ContentObject> postContents, PostITunes postITunes, String postUrl,
                    List<PostUrl> postUrls, String postComment, String postRights,
                    List<PostPerson> contributors, List<PostPerson> authors, List<String> postCategories,
                    Date publishTimestamp, Date expirationTimestamp, List<PostEnclosure> enclosures,
                    Date lastUpdatedTimestamp, boolean isPublished)
    {
        this.id = id;
        this.queueId = queueId;
        this.postTitle = postTitle;
        this.postDesc = postDesc;
        this.postContents = postContents;
        this.postITunes = postITunes;
        this.postUrl = postUrl;
        this.postUrls = postUrls;
        this.postComment = postComment;
        this.postRights = postRights;
        this.contributors = contributors;
        this.authors = authors;
        this.postCategories = postCategories;
        this.publishTimestamp = publishTimestamp;
        this.expirationTimestamp = expirationTimestamp;
        this.enclosures = enclosures;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        this.isPublished = isPublished;
    }

    /**
     * Static factory method to create a Post data transfer object.
     */
    public static PostDTO from(Long id, Long queueId, ContentObject postTitle, ContentObject postDesc,
                               List<ContentObject> postContents, PostITunes postITunes, String postUrl,
                               List<PostUrl> postUrls, String postComment, String postRights,
                               List<PostPerson> contributors, List<PostPerson> authors, List<String> postCategories,
                               Date publishTimestamp, Date expirationTimestamp, List<PostEnclosure> enclosures,
                               Date lastUpdatedTimestamp, boolean isPublished)
    {
        return new PostDTO(id, queueId, postTitle, postDesc,
                postContents, postITunes, postUrl,
                postUrls, postComment, postRights,
                contributors, authors, postCategories,
                publishTimestamp, expirationTimestamp, enclosures,
                lastUpdatedTimestamp, isPublished);
    }
}
