package com.lostsidewalk.buffy.app.model.request;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.post.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * A request model for configuring a post.
 */
@Data
@NoArgsConstructor
@JsonInclude(NON_ABSENT)
public class PostConfigRequest {

    @NotNull(message = "{post.config.error.post-title-is-null}")
    @Valid
    private ContentObject postTitle;

    @NotNull(message = "{post.config.error.post-desc-is-null}")
    @Valid
    private ContentObject postDesc;

    @Valid
    private List<ContentObject> postContents;

    @Valid
    private PostITunes postITunes;

    @NotBlank(message = "{post.config.error.url-is-blank}")
    @Size(max = 1024, message = "{post.config.error.url-too-long}")
    private String postUrl;

    @Valid
    private List<PostUrl> postUrls;

    @Size(max = 2048, message = "{post.config.error.comment-too-long}")
    private String postComment;

    @Size(max = 1024, message = "{post.config.error.rights-too-long}")
    private String postRights;

    @Valid
    private List<PostPerson> contributors;

    @Valid
    private List<PostPerson> authors;

    private List<String> postCategories;

    private Date expirationTimestamp;

    @Valid
    private List<PostEnclosure> enclosures;
}
