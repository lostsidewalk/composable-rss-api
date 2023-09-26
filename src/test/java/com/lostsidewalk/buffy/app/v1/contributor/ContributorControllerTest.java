package com.lostsidewalk.buffy.app.v1.contributor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.PostPersonConfigRequest;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostPerson;
import com.lostsidewalk.buffy.post.StagingPost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ContributorController.class)
public class ContributorControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final PostPersonConfigRequest TEST_POST_CONTRIBUTOR_REQUEST = new PostPersonConfigRequest();
    static {
        TEST_POST_CONTRIBUTOR_REQUEST.setName("me");
        TEST_POST_CONTRIBUTOR_REQUEST.setEmail("me@localhost");
        TEST_POST_CONTRIBUTOR_REQUEST.setUri("https://localhost");
    }

    private static final List<PostPersonConfigRequest> TEST_POST_CONTRIBUTOR_REQUESTS = List.of(TEST_POST_CONTRIBUTOR_REQUEST);

    @Test
    public void test_addPostContributor() throws Exception {
        when(this.stagingPostService.addContributor("me", 1L, TEST_POST_CONTRIBUTOR_REQUEST)).thenReturn("testContributor");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/posts/1/contributors")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUEST))
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Added contributor 'testContributor' to post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isCreated());
        verify(this.stagingPostService).addContributor("me", 1L, TEST_POST_CONTRIBUTOR_REQUEST);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescType", "testDescValue");

    private static final PostPerson TEST_POST_CONTRIBUTOR = new PostPerson();
    static {
        TEST_POST_CONTRIBUTOR.setIdent("2");
        TEST_POST_CONTRIBUTOR.setName("me");
        TEST_POST_CONTRIBUTOR.setEmail("me@localhost");
        TEST_POST_CONTRIBUTOR.setUri("https://localhost");
    }

    private static final Date NOW = new Date();

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
            NOW, // import timestamp
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
            null,
            NOW, // created
            null
    );

    @Test
    public void test_getPostContributors() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/contributors")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[{\"ident\":\"2\",\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_getPostContributor() throws Exception {
        when(this.stagingPostService.findContributorByIdent("me", 1L, "1")).thenReturn(TEST_POST_CONTRIBUTOR);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/contributors/1")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"ident\":\"2\",\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_updatePostContributor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/contributors/2")
                        .servletPath("/v1/posts/1/contributors/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUEST))
                        .header("Authorization", "Bearer testToken")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated contributor '2' on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateContributor("me", 1L, "2", TEST_POST_CONTRIBUTOR_REQUEST, false);
    }

    @Test
    public void test_updatePostContributors() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/contributors")
                        .servletPath("/v1/posts/1/contributors")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUESTS))
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated contributors on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateContributors("me", 1L, TEST_POST_CONTRIBUTOR_REQUESTS, false);
    }

    @Test
    public void test_patchPostContributor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/contributors/2")
                        .servletPath("/v1/posts/1/contributors/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUEST))
                        .header("Authorization", "Bearer testToken")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated contributor '2' on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateContributor("me", 1L, "2", TEST_POST_CONTRIBUTOR_REQUEST, true);
    }

    @Test
    public void test_patchPostContributors() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/contributors")
                        .servletPath("/v1/posts/1/contributors")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUESTS))
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated contributors on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updateContributors("me", 1L, TEST_POST_CONTRIBUTOR_REQUESTS, true);
    }

    @Test
    public void test_deletePostContributors() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/contributors")
                        .servletPath("/v1/posts/1/contributors")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted contributors from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deleteContributors("me", 1L);
    }

    @Test
    public void test_deletePostContributor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/contributors/2")
                        .servletPath("/v1/posts/1/contributors/2")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted contributor '2' from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deleteContributor("me", 1L, "2");
    }
}
