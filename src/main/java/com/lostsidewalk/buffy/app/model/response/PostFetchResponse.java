package com.lostsidewalk.buffy.app.model.response;

import com.lostsidewalk.buffy.post.StagingPost;
import lombok.Data;

import java.util.List;

@Data
public class PostFetchResponse {

    List<StagingPost> stagingPosts;

    private PostFetchResponse(List<StagingPost> stagingPosts) {
        this.stagingPosts = stagingPosts;
    }

    public static PostFetchResponse from(List<StagingPost> stagingPosts) {
        return new PostFetchResponse(stagingPosts);
    }
}
