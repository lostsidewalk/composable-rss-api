package com.lostsidewalk.buffy.app.v1.contributor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.PostPersonConfigRequest;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostPerson;
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

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ContributorController.class)
class ContributorControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
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
    void test_addPostContributor() throws Exception {
        when(stagingPostService.addContributor("me", 1L, TEST_POST_CONTRIBUTOR_REQUEST)).thenReturn("testContributor");
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/posts/1/contributors")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUEST))
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"tetDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"contributors\":[{\"ident\":\"2\",\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isCreated());
        verify(stagingPostService).addContributor("me", 1L, TEST_POST_CONTRIBUTOR_REQUEST);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleIdent", "testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("tetDescIdent", "testDescType", "testDescValue");

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
    void test_getPostContributors() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
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
    void test_getPostContributor() throws Exception {
        when(stagingPostService.findContributorByIdent("me", 1L, "1")).thenReturn(TEST_POST_CONTRIBUTOR);
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
    void test_updatePostContributor() throws Exception {
        when(stagingPostService.updateContributor("me", 1L, "2", TEST_POST_CONTRIBUTOR_REQUEST, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/contributors/2")
                        .servletPath("/v1/posts/1/contributors/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUEST))
                        .header("Authorization", "Bearer testToken")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"tetDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"contributors\":[{\"ident\":\"2\",\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updateContributor("me", 1L, "2", TEST_POST_CONTRIBUTOR_REQUEST, false);
    }

    @Test
    void test_updatePostContributors() throws Exception {
        when(stagingPostService.updateContributors("me", 1L, TEST_POST_CONTRIBUTOR_REQUESTS, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/contributors")
                        .servletPath("/v1/posts/1/contributors")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUESTS))
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"tetDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"contributors\":[{\"ident\":\"2\",\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updateContributors("me", 1L, TEST_POST_CONTRIBUTOR_REQUESTS, false);
    }

    @Test
    void test_patchPostContributor() throws Exception {
        when(stagingPostService.updateContributor("me", 1L, "2", TEST_POST_CONTRIBUTOR_REQUEST, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/contributors/2")
                        .servletPath("/v1/posts/1/contributors/2")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUEST))
                        .header("Authorization", "Bearer testToken")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"tetDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"contributors\":[{\"ident\":\"2\",\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updateContributor("me", 1L, "2", TEST_POST_CONTRIBUTOR_REQUEST, true);
    }

    @Test
    void test_patchPostContributors() throws Exception {
        when(stagingPostService.updateContributors("me", 1L, TEST_POST_CONTRIBUTOR_REQUESTS, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/contributors")
                        .servletPath("/v1/posts/1/contributors")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONTRIBUTOR_REQUESTS))
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"tetDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"contributors\":[{\"ident\":\"2\",\"name\":\"me\",\"email\":\"me@localhost\",\"uri\":\"https://localhost\"}],\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updateContributors("me", 1L, TEST_POST_CONTRIBUTOR_REQUESTS, true);
    }

    @Test
    void test_deletePostContributors() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setContributors(null);
        when(stagingPostService.deleteContributors("me", 1L)).thenReturn(updatedPost);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/contributors")
                        .servletPath("/v1/posts/1/contributors")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"tetDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).deleteContributors("me", 1L);
    }

    @Test
    void test_deletePostContributor() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setContributors(null);
        when(stagingPostService.deleteContributor("me", 1L, "2")).thenReturn(updatedPost);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/contributors/2")
                        .servletPath("/v1/posts/1/contributors/2")
                        .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"tetDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).deleteContributor("me", 1L, "2");
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
