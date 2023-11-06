package com.lostsidewalk.buffy.app.v1.url;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.PostUrlConfigRequest;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostUrl;
import com.lostsidewalk.buffy.post.StagingPost;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = URLController.class)
class URLControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final PostUrlConfigRequest TEST_POST_URL_REQUEST = new PostUrlConfigRequest();
    static {
        TEST_POST_URL_REQUEST.setHref("testHref");
        TEST_POST_URL_REQUEST.setHreflang("testHreflang");
        TEST_POST_URL_REQUEST.setType("testURLType");
        TEST_POST_URL_REQUEST.setRel("testRel");
        TEST_POST_URL_REQUEST.setTitle("testTitle");
    }

    private static final List<PostUrlConfigRequest> TEST_POST_URL_REQUESTS = List.of(TEST_POST_URL_REQUEST);

    @Test
    void test_addPostUrl() throws Exception {
        when(stagingPostService.addPostUrl("me", 1L, TEST_POST_URL_REQUEST)).thenReturn("testUrl");
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/posts/1/urls")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_URL_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testTitle\",\"type\":\"testURLType\",\"href\":\"testHref\",\"hreflang\":\"testHreflang\",\"rel\":\"testRel\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isCreated());
        verify(stagingPostService).addPostUrl("me", 1L, TEST_POST_URL_REQUEST);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleIdent", "testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescIdent", "testDescType", "testDescValue");

    private static final PostUrl TEST_POST_URL = new PostUrl();
    static {
        TEST_POST_URL.setIdent("2");
        TEST_POST_URL.setTitle("testTitle");
        TEST_POST_URL.setType("testURLType");
        TEST_POST_URL.setHref("testHref");
        TEST_POST_URL.setHreflang("testHreflang");
        TEST_POST_URL.setRel("testRel");
    }

    private static final Date NOW = new Date();

    private static final List<PostUrl> TEST_POST_URLS = singletonList(TEST_POST_URL);

    private static final StagingPost TEST_STAGING_POST = StagingPost.from(
            "testImporterId",
            1L,
            "testImporterDesc",
            2L,
            TEST_POST_TITLE,
            TEST_POST_DESC,
            null,
            null,
            null,
            TEST_POST_URL.getHref(),
            TEST_POST_URLS,
            null,
            null,
            NOW, // import timestamp
            "testPostHash",
            "me",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            NOW, // created
            null
    );

    @Test
    void test_getPostUrls() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/urls")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[{\"ident\":\"2\",\"title\":\"testTitle\",\"type\":\"testURLType\",\"href\":\"testHref\",\"hreflang\":\"testHreflang\",\"rel\":\"testRel\"}]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostUrl() throws Exception {
        when(stagingPostService.findUrlByIdent("me", 1L, "1")).thenReturn(TEST_POST_URL);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/urls/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"ident\":\"2\",\"title\":\"testTitle\",\"type\":\"testURLType\",\"href\":\"testHref\",\"hreflang\":\"testHreflang\",\"rel\":\"testRel\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostUrl() throws Exception {
        when(stagingPostService.updatePostUrl("me", 1L, "2", TEST_POST_URL_REQUEST, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/urls/2")
                        .servletPath("/v1/posts/1/urls/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_URL_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testTitle\",\"type\":\"testURLType\",\"href\":\"testHref\",\"hreflang\":\"testHreflang\",\"rel\":\"testRel\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePostUrl("me", 1L, "2", TEST_POST_URL_REQUEST, false);
    }

    @Test
    void test_updatePostUrls() throws Exception {
        when(stagingPostService.updatePostUrls("me", 1L, TEST_POST_URL_REQUESTS, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/urls")
                        .servletPath("/v1/posts/1/urls")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_URL_REQUESTS))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testTitle\",\"type\":\"testURLType\",\"href\":\"testHref\",\"hreflang\":\"testHreflang\",\"rel\":\"testRel\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePostUrls("me", 1L, TEST_POST_URL_REQUESTS, false);
    }

    @Test
    void test_patchPostUrl() throws Exception {
        when(stagingPostService.updatePostUrl("me", 1L, "2", TEST_POST_URL_REQUEST, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/urls/2")
                        .servletPath("/v1/posts/1/urls/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_URL_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testTitle\",\"type\":\"testURLType\",\"href\":\"testHref\",\"hreflang\":\"testHreflang\",\"rel\":\"testRel\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePostUrl("me", 1L, "2", TEST_POST_URL_REQUEST, true);
    }

    @Test
    void test_patchPostUrls() throws Exception {
        when(stagingPostService.updatePostUrls("me", 1L, TEST_POST_URL_REQUESTS, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/urls")
                        .servletPath("/v1/posts/1/urls")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_URL_REQUESTS))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testTitle\",\"type\":\"testURLType\",\"href\":\"testHref\",\"hreflang\":\"testHreflang\",\"rel\":\"testRel\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePostUrls("me", 1L, TEST_POST_URL_REQUESTS, true);
    }

    @Test
    void test_deletePostUrls() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostUrls(null);
        when(stagingPostService.deletePostUrls("me", 1L)).thenReturn(updatedPost);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/urls")
                        .servletPath("/v1/posts/1/urls")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testHref\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).deletePostUrls("me", 1L);
    }

    @Test
    void test_deletePostUrl() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostUrls(null);
        when(stagingPostService.deletePostUrl("me", 1L, "2")).thenReturn(updatedPost);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/urls/2")
                        .servletPath("/v1/posts/1/urls/2")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testHref\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).deletePostUrl("me", 1L, "2");
    }

    //
    //
    //

    private static StagingPost copyTestStagingPost() {
        StagingPost stagingPost = StagingPost.from(
                TEST_STAGING_POST.getImporterId(),
                TEST_STAGING_POST.getQueueId(),
                TEST_STAGING_POST.getImporterDesc(),
                TEST_STAGING_POST.getSubscriptionId(),
                TEST_STAGING_POST.getPostTitle(),
                TEST_STAGING_POST.getPostDesc(),
                TEST_STAGING_POST.getPostContents(),
                TEST_STAGING_POST.getPostMedia(),
                TEST_STAGING_POST.getPostITunes(),
                TEST_STAGING_POST.getPostUrl(),
                TEST_STAGING_POST.getPostUrls(),
                TEST_STAGING_POST.getPostImgUrl(),
                TEST_STAGING_POST.getPostImgTransportIdent(),
                TEST_STAGING_POST.getImportTimestamp(),
                TEST_STAGING_POST.getPostHash(),
                TEST_STAGING_POST.getUsername(),
                TEST_STAGING_POST.getPostComment(),
                TEST_STAGING_POST.getPostRights(),
                TEST_STAGING_POST.getContributors(),
                TEST_STAGING_POST.getAuthors(),
                TEST_STAGING_POST.getPostCategories(),
                TEST_STAGING_POST.getPublishTimestamp(),
                TEST_STAGING_POST.getExpirationTimestamp(),
                TEST_STAGING_POST.getEnclosures(),
                TEST_STAGING_POST.getLastUpdatedTimestamp(),
                TEST_STAGING_POST.getCreated(),
                TEST_STAGING_POST.getLastModified()
        );
        stagingPost.setId(TEST_STAGING_POST.getId());

        return stagingPost;
    }
}
