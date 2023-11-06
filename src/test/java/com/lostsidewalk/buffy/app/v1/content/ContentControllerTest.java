package com.lostsidewalk.buffy.app.v1.content;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.ContentObjectConfigRequest;
import com.lostsidewalk.buffy.post.ContentObject;
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
@WebMvcTest(controllers = ContentController.class)
class ContentControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final ContentObjectConfigRequest TEST_POST_CONTENT_REQUEST = new ContentObjectConfigRequest();
    static {
        TEST_POST_CONTENT_REQUEST.setType("testContentType");
        TEST_POST_CONTENT_REQUEST.setValue("testContentValue");
    }

    private static final List<ContentObjectConfigRequest> TEST_POST_CONTENT_REQUESTS = List.of(TEST_POST_CONTENT_REQUEST);

    @Test
    void test_addPostContent() throws Exception {
        when(stagingPostService.addContent("me", 1L, TEST_POST_CONTENT_REQUEST)).thenReturn("testContent");
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/posts/1/content")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTENT_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postContents\":[{\"ident\":\"2\",\"type\":\"testContentType\",\"value\":\"testContentValue\"}],\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isCreated());
        verify(stagingPostService).addContent("me", 1L, TEST_POST_CONTENT_REQUEST);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleIdent", "testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescIdent", "testDescType", "testDescValue");

    private static final ContentObject TEST_POST_CONTENT = new ContentObject();
    static {
        TEST_POST_CONTENT.setIdent("2");
        TEST_POST_CONTENT.setType("testContentType");
        TEST_POST_CONTENT.setValue("testContentValue");
    }

    private static final Date NOW = new Date();

    private static final List<ContentObject> TEST_POST_CONTENTS = singletonList(TEST_POST_CONTENT);

    private static final StagingPost TEST_STAGING_POST = StagingPost.from(
            "testImporterId",
            1L,
            "testImporterDesc",
            2L,
            TEST_POST_TITLE,
            TEST_POST_DESC,
            TEST_POST_CONTENTS,
            null,
            null,
            "testPostUrl",
            null,
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
    void test_getPostContents() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/content")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[{\"ident\":\"2\",\"type\":\"testContentType\",\"value\":\"testContentValue\"}]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostContent() throws Exception {
        when(stagingPostService.findContentByIdent("me", 1L, "1")).thenReturn(TEST_POST_CONTENT);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/content/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"ident\":\"2\",\"type\":\"testContentType\",\"value\":\"testContentValue\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostContent() throws Exception {
        when(stagingPostService.updateContent("me", 1L, "2", TEST_POST_CONTENT_REQUEST, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/content/2")
                        .servletPath("/v1/posts/1/content/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTENT_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postContents\":[{\"ident\":\"2\",\"type\":\"testContentType\",\"value\":\"testContentValue\"}],\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updateContent("me", 1L, "2", TEST_POST_CONTENT_REQUEST, false);
    }

    @Test
    void test_updatePostContents() throws Exception {
        when(stagingPostService.updateContents("me", 1L, TEST_POST_CONTENT_REQUESTS, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/content")
                        .servletPath("/v1/posts/1/content")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTENT_REQUESTS))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postContents\":[{\"ident\":\"2\",\"type\":\"testContentType\",\"value\":\"testContentValue\"}],\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updateContents("me", 1L, TEST_POST_CONTENT_REQUESTS, false);
    }

    @Test
    void test_patchPostContent() throws Exception {
        when(stagingPostService.updateContent("me", 1L, "2", TEST_POST_CONTENT_REQUEST, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/content/2")
                        .servletPath("/v1/posts/1/content/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTENT_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postContents\":[{\"ident\":\"2\",\"type\":\"testContentType\",\"value\":\"testContentValue\"}],\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updateContent("me", 1L, "2", TEST_POST_CONTENT_REQUEST, true);
    }

    @Test
    void test_patchPostContents() throws Exception {
        when(stagingPostService.updateContents("me", 1L, TEST_POST_CONTENT_REQUESTS, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/content")
                        .servletPath("/v1/posts/1/content")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTENT_REQUESTS))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postContents\":[{\"ident\":\"2\",\"type\":\"testContentType\",\"value\":\"testContentValue\"}],\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updateContents("me", 1L, TEST_POST_CONTENT_REQUESTS, true);
    }

    @Test
    void test_deletePostContent() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostContents(null);
        when(stagingPostService.deletePostContent("me", 1L, "2")).thenReturn(updatedPost);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/content/2")
                        .servletPath("/v1/posts/1/content/2")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).deletePostContent("me", 1L, "2");
    }

    @Test
    void test_deletePostContents() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostContents(null);
        when(stagingPostService.deletePostContents("me", 1L)).thenReturn(updatedPost);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/content")
                        .servletPath("/v1/posts/1/content")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).deletePostContents("me", 1L);
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
