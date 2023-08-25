package com.lostsidewalk.buffy.app.model.response;


import com.lostsidewalk.buffy.post.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
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
    @NotNull(message = "{post.error.queue-id-is-null}")
    Long queueId;

    /**
     * The title of the post.
     */
    @NotNull(message = "{post.error.post-title-is-null}")
    @Valid
    ContentObject postTitle;

    /**
     * The description of the post.
     */
    @NotNull(message = "{post.error.post-desc-is-null}")
    @Valid
    ContentObject postDesc;

    /**
     * A list of ContentObject objects (the post content)
     */
    @Valid
    List<ContentObject> postContents;

    /**
     * The post ITunes descriptor
     */
    @Valid
    PostITunes postITunes;

    /**
     * The (primary) post URL
     */
    @NotBlank(message = "{post.error.url-is-blank}")
    @Size(max = 1024, message = "{post.error.url-too-long}")
    String postUrl;

    /**
     * A list of alternate post URLs
     */
    @Valid
    List<PostUrl> postUrls;

    /**
     * The timestamp of when the post was imported.
     */
    @NotNull(message = "{post.error.import-timestamp-is-null}")
    Date importTimestamp;

    /**
     * The comment associated with the post.
     */
    @Size(max = 2048, message = "{post.error.comment-too-long}")
    String postComment;

    /**
     * The rights associated with the post.
     */
    @Size(max = 1024, message = "{post.error.rights-too-long}")
    String postRights;

    /**
     * The list of contributors to the post.
     */
    @Valid
    List<PostPerson> contributors;

    /**
     * The list of authors of the post.
     */
    @Valid
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
    @Valid
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
                    Date lastUpdatedTimestamp, boolean isPublished) {
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
                               Date lastUpdatedTimestamp, boolean isPublished) {
        return new PostDTO(id, queueId, postTitle, postDesc,
                postContents, postITunes, postUrl,
                postUrls, postComment, postRights,
                contributors, authors, postCategories,
                publishTimestamp, expirationTimestamp, enclosures,
                lastUpdatedTimestamp, isPublished);
    }
}
