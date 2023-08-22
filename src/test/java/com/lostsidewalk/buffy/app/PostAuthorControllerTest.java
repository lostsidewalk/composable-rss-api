package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PostAuthorController.class)
public class PostAuthorControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final PostPerson TEST_POST_AUTHOR = new PostPerson();
    static {
        TEST_POST_AUTHOR.setName("me");
        TEST_POST_AUTHOR.setEmail("me@localhost");
        TEST_POST_AUTHOR.setUri("https://localhost");
    }

    @Test
    public void test_addPostAuthor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/posts/1/authors")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_POST_AUTHOR))
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Added author to post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).addAuthor("me", 1L, TEST_POST_AUTHOR);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescType", "testDescValue");

    private static final List<PostPerson> TEST_POST_AUTHORS = singletonList(TEST_POST_AUTHOR);

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
            null,
            TEST_POST_AUTHORS,
            null,
            null,
            null,
            null,
            null
    );

    @Test
    public void test_getPostAuthors() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/authors")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[{\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_updatePostAuthor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/authors/2")
                        .servletPath("/posts/1/authors/2")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_POST_AUTHOR))
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated author on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updatePostAuthor("me", 1L, 2, TEST_POST_AUTHOR);
    }

    @Test
    public void test_deletePostAuthor() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/1/authors/2")
                        .servletPath("/posts/1/authors/2")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_POST_AUTHOR))
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted author from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deletePostAuthor("me", 1L, 2);
    }
}
