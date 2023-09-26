package com.lostsidewalk.buffy.app.model.v1.request;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.post.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * A request model for configuring a post.
 *
 * A queue may contain any number of posts. An post may represent a "story" -- much like a story in a newspaper or
 * magazine; if so its description is a synopsis of the story, and the link points to the full story. A post may also
 * be complete in itself, if so, the description contains the text (entity-encoded HTML is allowed; see examples),
 * and the link and title may be omitted. All elements of a post are optional, however at least one of title or
 * description must be present.
 */
@Data
@NoArgsConstructor
@JsonInclude(NON_ABSENT)
public class PostConfigRequest {

    /**
     * The title of the post.
     * <p>
     * Example: Venice Film Festival Tries to Quit Sinking
     */
    @NotNull(message = "{post.config.error.post-title-is-null}")
    @Valid
    private ContentObjectConfigRequest postTitle;

    /**
     * A synopsis of the post.
     * <p>
     * Example: Some of the most heated chatter at the Venice Film Festival this week was about the way that the arrival of the stars at the Palazzo del Cinema was being staged.
     */
    @NotNull(message = "{post.config.error.post-desc-is-null}")
    @Valid
    private ContentObjectConfigRequest postDesc;

    /**
     * A list of ContentObject objects (the post content)
     */
    @Valid
    private List<ContentObjectConfigRequest> postContents;

    /**
     * The post ITunes descriptor
     */
    @Valid
    private PostITunes postITunes;

    /**
     * The (primary) post URL
     */
    @Size(max = 1024, message = "{post.config.error.url-too-long}")
    private String postUrl;

    /**
     * A list of alternate post URLs
     */
    @Valid
    private List<PostUrlConfigRequest> postUrls;

    /**
     * A URL of a page for comments relating to the post.
     * <p>
     * Example: http://ekzemplo.com/entry/4403/comments
     */
    @SuppressWarnings("JavadocLinkAsPlainText")
    @Size(max = 2048, message = "{post.config.error.comment-too-long}")
    private String postComment;

    /**
     * The rights associated with the post.
     */
    @Size(max = 1024, message = "{post.config.error.rights-too-long}")
    private String postRights;

    /**
     * The list of contributors to the post.
     */
    @Valid
    private List<PostPersonConfigRequest> contributors;

    /**
     * The list of authors of the post.
     */
    @Valid
    private List<PostPersonConfigRequest> authors;

    /**
     * The list of categories associated with the post.
     * <p>
     * Example: Grateful Dead
     */
    private List<String> postCategories;

    /**
     * The timestamp of when the post will expire.
     */
    private Date expirationTimestamp;

    /**
     * A list of media objects that are attached to the post.
     */
    @Valid
    private List<PostEnclosureConfigRequest> enclosures;
}
