package com.lostsidewalk.buffy.app.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lostsidewalk.buffy.post.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private ContentObject postTitle;
    private ContentObject postDesc;
    private List<ContentObject> postContents;
    private PostITunes postITunes;
    private String postUrl;
    private List<PostUrl> postUrls;
    private String postComment;
    private String postRights;
    private List<PostPerson> contributors;
    private List<PostPerson> authors;
    private List<String> postCategories;
    private Date expirationTimestamp;
    private List<PostEnclosure> enclosures;
}
