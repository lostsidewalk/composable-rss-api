package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.post.*;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PostContributorController.class)
public class PostContributorControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final PostPerson TEST_POST_CONTRIBUTOR = new PostPerson();
    static {
        TEST_POST_CONTRIBUTOR.setName("me");
        TEST_POST_CONTRIBUTOR.setEmail("me@localhost");
        TEST_POST_CONTRIBUTOR.setUri("https://localhost");
    }

    @Test
    public void test_addPostContributor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/posts/1/contributors")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Added contributor to post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).addContributor("me", 1L, TEST_POST_CONTRIBUTOR);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescType", "testDescValue");

    private static final List<PostPerson> TEST_POST_CONTRIBUTORS = singletonList(TEST_POST_CONTRIBUTOR);

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
            new Date(),
            "testPostHash",
            "me",
            null,
            null,
            TEST_POST_CONTRIBUTORS,
            null,
            null,
            null,
            null,
            null,
            null
    );

    @Test
    public void test_getPostContributors() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/contributors")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[{\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_updatePostContributor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/contributors/2")
                        .servletPath("/posts/1/contributors/2")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated contributor on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updatePostContributor("me", 1L, 2, TEST_POST_CONTRIBUTOR);
    }

    @Test
    public void test_deletePostContributor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/1/contributors/2")
                        .servletPath("/posts/1/contributors/2")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted contributor from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deletePostContributor("me", 1L, 2);
    }
}
