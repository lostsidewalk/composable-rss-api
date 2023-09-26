package com.lostsidewalk.buffy.app.v1.enclosure;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.PostEnclosureConfigRequest;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostEnclosure;
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
@WebMvcTest(controllers = EnclosureController.class)
public class EnclosureControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final PostEnclosureConfigRequest TEST_POST_ENCLOSURE_REQUEST = new PostEnclosureConfigRequest();
    static {
        TEST_POST_ENCLOSURE_REQUEST.setLength(64L);
        TEST_POST_ENCLOSURE_REQUEST.setUrl("https://localhost");
        TEST_POST_ENCLOSURE_REQUEST.setType("testEnclosureType");
    }

    private static final List<PostEnclosureConfigRequest> TEST_POST_ENCLOSURES_REQUEST = List.of(TEST_POST_ENCLOSURE_REQUEST);

    @Test
    public void test_addPostEnclosure() throws Exception {
        when(this.stagingPostService.addEnclosure("me", 1L, TEST_POST_ENCLOSURE_REQUEST)).thenReturn("testEnclosure");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/posts/1/enclosures")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_ENCLOSURE_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Added enclosure 'testEnclosure' to post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isCreated());
        verify(this.stagingPostService).addEnclosure("me", 1L, TEST_POST_ENCLOSURE_REQUEST);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescType", "testDescValue");

    private static final PostEnclosure TEST_POST_ENCLOSURE = new PostEnclosure();
    static {
        TEST_POST_ENCLOSURE.setIdent("2");
        TEST_POST_ENCLOSURE.setType("testEnclosureType");
        TEST_POST_ENCLOSURE.setUrl("https://localhost");
        TEST_POST_ENCLOSURE.setLength(64L);
    }

    private static final Date NOW = new Date();

    private static final List<PostEnclosure> TEST_POST_ENCLOSURES = singletonList(TEST_POST_ENCLOSURE);

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
            TEST_POST_ENCLOSURES,
            null,
            NOW, // created
            null
    );

    @Test
    public void test_getPostEnclosures() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/enclosures")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[{\"ident\":\"2\",\"url\":\"https://localhost\",\"type\":\"testEnclosureType\",\"length\":64}]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_getPostEnclosure() throws Exception {
        when(this.stagingPostService.findEnclosureByIdent("me", 1L, "1")).thenReturn(TEST_POST_ENCLOSURE);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/enclosures/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"ident\":\"2\",\"url\":\"https://localhost\",\"type\":\"testEnclosureType\",\"length\":64}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_updatePostEnclosures() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/enclosures")
                        .servletPath("/v1/posts/1/enclosures")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_ENCLOSURES_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated enclosures on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateEnclosures("me", 1L, TEST_POST_ENCLOSURES_REQUEST, false);
    }

    @Test
    public void test_updatePostEnclosure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/enclosures/2")
                        .servletPath("/v1/posts/1/enclosures/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_ENCLOSURE_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated enclosure '2' on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateEnclosure("me", 1L, "2", TEST_POST_ENCLOSURE_REQUEST, false);
    }

    @Test
    public void test_patchPostEnclosures() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/enclosures")
                        .servletPath("/v1/posts/1/enclosures")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_ENCLOSURES_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated enclosures on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateEnclosures("me", 1L, TEST_POST_ENCLOSURES_REQUEST, true);
    }

    @Test
    public void test_patchPostEnclosure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/enclosures/2")
                        .servletPath("/v1/posts/1/enclosures/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_ENCLOSURE_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated enclosure '2' on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateEnclosure("me", 1L, "2", TEST_POST_ENCLOSURE_REQUEST, true);
    }

    @Test
    public void test_deletePostEnclosures() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/enclosures")
                        .servletPath("/v1/posts/1/enclosures")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted all enclosures from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deleteEnclosures("me", 1L);
    }

    @Test
    public void test_deletePostEnclosure() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/enclosures/2")
                        .servletPath("/v1/posts/1/enclosures/2")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted enclosure '2' from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deleteEnclosure("me", 1L, "2");
    }
}
