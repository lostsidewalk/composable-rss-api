package com.lostsidewalk.buffy.app.v1.url;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.PostUrlConfigRequest;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostUrl;
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
@WebMvcTest(controllers = URLController.class)
public class URLControllerTest extends BaseWebControllerTest {

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

    private static final List<PostUrlConfigRequest> TEST_POST_URLS_REQUEST = List.of(TEST_POST_URL_REQUEST);

    @Test
    public void test_addPostUrl() throws Exception {
        when(this.stagingPostService.addPostUrl("me", 1L, TEST_POST_URL_REQUEST)).thenReturn("testUrl");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/posts/1/urls")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_URL_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Added URL 'testUrl' to post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isCreated());
        verify(this.stagingPostService).addPostUrl("me", 1L, TEST_POST_URL_REQUEST);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescType", "testDescValue");

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
    public void test_getPostUrls() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
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
    public void test_getPostUrl() throws Exception {
        when(this.stagingPostService.findUrlByIdent("me", 1L, "1")).thenReturn(TEST_POST_URL);
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
    public void test_updatePostUrl() throws Exception {
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
                    assertEquals(GSON.fromJson("{\"message\":\"Updated URL '2' on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updatePostUrl("me", 1L, "2", TEST_POST_URL_REQUEST, false);
    }

    @Test
    public void test_updatePostUrls() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/urls")
                        .servletPath("/v1/posts/1/urls")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_URLS_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated URLs on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updatePostUrls("me", 1L, TEST_POST_URLS_REQUEST, false);
    }

    @Test
    public void test_patchPostUrl() throws Exception {
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
                    assertEquals(GSON.fromJson("{\"message\":\"Updated URL '2' on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updatePostUrl("me", 1L, "2", TEST_POST_URL_REQUEST, true);
    }

    @Test
    public void test_patchPostUrls() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/urls")
                        .servletPath("/v1/posts/1/urls")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_URLS_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated URLs on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updatePostUrls("me", 1L, TEST_POST_URLS_REQUEST, true);
    }

    @Test
    public void test_deletePostUrls() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/urls")
                        .servletPath("/v1/posts/1/urls")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted URLs from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deletePostUrls("me", 1L);
    }

    @Test
    public void test_deletePostUrl() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/urls/2")
                        .servletPath("/v1/posts/1/urls/2")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted URL '2' from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deletePostUrl("me", 1L, "2");
    }
}
