package com.lostsidewalk.buffy.app.v1.content;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.ContentObjectConfigRequest;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.StagingPost;
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

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ContentController.class)
public class ContentControllerTest extends BaseWebControllerTest {

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
    public void test_addPostContent() throws Exception {
        when(this.stagingPostService.addContent("me", 1L, TEST_POST_CONTENT_REQUEST)).thenReturn("testContent");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/posts/1/content")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTENT_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Added content 'testContent' to post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isCreated());
        verify(this.stagingPostService).addContent("me", 1L, TEST_POST_CONTENT_REQUEST);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescType", "testDescValue");

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
    public void test_getPostContents() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
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
    public void test_getPostContent() throws Exception {
        when(this.stagingPostService.findContentByIdent("me", 1L, "1")).thenReturn(TEST_POST_CONTENT);
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
    public void test_updatePostContent() throws Exception {
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
                    assertEquals(GSON.fromJson("{\"message\":\"Updated content '2' on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateContent("me", 1L, "2", TEST_POST_CONTENT_REQUEST, false);
    }

    @Test
    public void test_updatePostContents() throws Exception {
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
                    assertEquals(GSON.fromJson("{\"message\":\"Updated contents on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateContents("me", 1L, TEST_POST_CONTENT_REQUESTS, false);
    }

    @Test
    public void test_patchPostContent() throws Exception {
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
                    assertEquals(GSON.fromJson("{\"message\":\"Updated content '2' on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateContent("me", 1L, "2", TEST_POST_CONTENT_REQUEST, true);
    }

    @Test
    public void test_patchPostContents() throws Exception {
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
                    assertEquals(GSON.fromJson("{\"message\":\"Updated contents on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateContents("me", 1L, TEST_POST_CONTENT_REQUESTS, true);
    }

    @Test
    public void test_deletePostContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/content/2")
                        .servletPath("/v1/posts/1/content/2")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted content '2' from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deleteContent("me", 1L, "2");
    }

    @Test
    public void test_deletePostContents() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/content")
                        .servletPath("/v1/posts/1/content")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted contents from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deletePostContents("me", 1L);
    }
}
