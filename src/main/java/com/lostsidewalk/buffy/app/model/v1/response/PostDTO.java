package com.lostsidewalk.buffy.app.model.v1.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.post.*;
import com.lostsidewalk.buffy.post.StagingPost.PostPubStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * A response model for a post.
 */
@Slf4j
@Data
@JsonInclude(NON_ABSENT)
public class PostDTO {

    /**
     * The unique identifier of the post.
     */
    Long id;

    /**
     * The identifier of the queue to which the post belongs.
     */
    @NotBlank(message = "{post.error.queue-ident-is-blank}")
    String queueIdent;

    /**
     * The title of the post.
     * <p>
     * RSS2: item -> title
     * ATOM1: entry -> title
     * <p>
     * Example: Venice Film Festival Tries to Quit Sinking
     */
    @NotNull(message = "{post.error.post-title-is-null}")
    @Valid
    ContentObject postTitle;

    /**
     * A synopsis of the post.
     * <p>
     * RSS2: item -> description
     * ATOm1: entry -> summary
     * <p>
     * Example: Some of the most heated chatter at the Venice Film Festival this week was about the way that the arrival of the stars at the Palazzo del Cinema was being staged.
     */
    @NotNull(message = "{post.error.post-desc-is-null}")
    @Valid
    ContentObject postDesc;

    /**
     * A list of (HTML or text) content objects that are attached to the post.
     * <p>
     * RSS2: item -> content
     * ATOM1: entry -> contents
     */
    @Valid
    List<ContentObject> postContents;

    /**
     * The post iTunes descriptor module.
     * <p>
     * RSS2: item -> modules
     * ATOM1: entry -> modules
     */
    @Valid
    PostITunes postITunes;

    /**
     * The (primary) post URL.  This is optional for RSS2 channel items, and required for ATOM1 feed entries.
     * <p>
     * RSS2: item -> link, item -> URI
     * ATOM1: entry -> id
     */
    @Size(max = 1024, message = "{post.error.url-too-long}")
    String postUrl;

    /**
     * A list of alternate post URLs.
     * <p>
     * ATOM1: entry -> otherLinks
     */
    @Valid
    List<PostUrl> postUrls;

    /**
     * The timestamp of when the post was imported.
     */
    @NotNull(message = "{post.error.import-timestamp-is-null}")
    Date importTimestamp;

    /**
     * A URL of a page for comments relating to the post.
     * <p>
     * RSS2: item -> comments
     * <p>
     * Example: http://ekzemplo.com/entry/4403/comments
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    @Size(max = 2048, message = "{post.error.comment-too-long}")
    String postComment;

    /**
     * The rights associated with the post.
     * <p>
     * ATOM1: entry -> rights
     */
    @Size(max = 1024, message = "{post.error.rights-too-long}")
    String postRights;

    /**
     * A list of contributors to the post.
     * <p>
     * ATOM1: entry -> contributors
     */
    @Valid
    List<PostPerson> contributors;

    /**
     * A list of authors of the post.
     * <p>
     * RSS2: item -> author
     * ATOM1: entry -> authors
     */
    @Valid
    List<PostPerson> authors;

    /**
     * A list of categories associated with the post.
     * <p>
     * RSS2: item -> categories
     * ATOm1: entry -> categories
     * <p>
     * Example: Grateful Dead
     */
    List<String> postCategories;

    /**
     * The timestamp of when the post was published.
     * <p>
     * RSS2: item -> pubDate
     * ATOM1: entry -> published
     */
    Date publishTimestamp;

    /**
     * The timestamp of when the post will expire.
     * <p>
     * RSS2: item -> expirationDate
     */
    Date expirationTimestamp;

    /**
     * A list of media objects that are attached to the post.
     * <p>
     * RSS2: item -> enclosures
     */
    @Valid
    List<PostEnclosure> enclosures;

    /**
     * The timestamp of the last update to the post.
     * <p>
     * ATOM1: entry -> updated
     */
    Date lastUpdatedTimestamp;

    /**
     * Indicates whether the post is currently published.
     */
    boolean isPublished;

    /**
     * The current status of the post (PUB_PENDING, DEPUB_PENDING, etc.).
     */
    PostPubStatus postPubStatus;

    /**
     * Indicates whether the post is currently archived.
     */
    Boolean isArchived;

    private PostDTO(Long id, String queueIdent, ContentObject postTitle, ContentObject postDesc,
                    List<ContentObject> postContents, PostITunes postITunes, String postUrl,
                    List<PostUrl> postUrls, String postComment, String postRights,
                    List<PostPerson> contributors, List<PostPerson> authors, List<String> postCategories,
                    Date publishTimestamp, Date expirationTimestamp, List<PostEnclosure> enclosures,
                    Date lastUpdatedTimestamp, boolean isPublished, PostPubStatus postPubStatus, Boolean isArchived) {
        this.id = id;
        this.queueIdent = queueIdent;
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
        this.postPubStatus = postPubStatus;
        this.isArchived = isArchived;
    }

    /**
     * Static factory method to create a Post data transfer object from the supplied parameters.
     */
    @SuppressWarnings("WeakerAccess")
    public static PostDTO from(Long id, String queueIdent, ContentObject postTitle, ContentObject postDesc,
                               List<ContentObject> postContents, PostITunes postITunes, String postUrl,
                               List<PostUrl> postUrls, String postComment, String postRights,
                               List<PostPerson> contributors, List<PostPerson> authors, List<String> postCategories,
                               Date publishTimestamp, Date expirationTimestamp, List<PostEnclosure> enclosures,
                               Date lastUpdatedTimestamp, Boolean isPublished, PostPubStatus postPubStatus,
                               Boolean isArchived) {
        return new PostDTO(id, queueIdent, postTitle, postDesc,
                postContents, postITunes, postUrl,
                postUrls, postComment, postRights,
                contributors, authors, postCategories,
                publishTimestamp, expirationTimestamp, enclosures,
                lastUpdatedTimestamp, isPublished, postPubStatus, isArchived);
    }

    public static PostDTO from(StagingPost stagingPost, String queueIdent) {
        return from(stagingPost.getId(),
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
                // Note: always include isPublished in the DTO
                isTrue(stagingPost.isPublished()),
                stagingPost.getPostPubStatus(),
                // Note: only return isArchived if it is true; otherwise exclude it from the DTO
                isTrue(stagingPost.isArchived()) ? true : null);
    }
}
