package com.lostsidewalk.buffy.app.v1.post;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.post.StagingPost;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.PUB_PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PostStatusController.class)
class PostStatusControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .create();

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    //
    //
    //

    private static final StagingPost TEST_STAGING_POST = StagingPost.from(
            "testImporterId",
            1L,
            "testImporterDesc",
            2L,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null, // import timestamp
            null,
            "me",
            null,
            null,
            null,
            null,
            null,
            null, // publish timestamp
            null, // expiration timestamp
            null,
            null, // last updated timestamp,
            null, // created
            null // last modified
    );
    static {
        TEST_STAGING_POST.setId(1L);
    }

    @Test
    void test_getPostStatus() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/status")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("UNPUBLISHED", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    private static final PostStatusUpdateRequest TEST_POST_STATUS_UPDATE_REQUEST = new PostStatusUpdateRequest();
    static {
        TEST_POST_STATUS_UPDATE_REQUEST.setNewStatus(PUB_PENDING.name());
    }

    @Test
    void test_updatePostStatus() throws Exception {
        when(stagingPostService.updatePostPubStatus("me", 1L, PUB_PENDING)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/status")
                        .servletPath("/v1/posts/1/status")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_STATUS_UPDATE_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"id\":1,\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchPostStatus() throws Exception {
        when(stagingPostService.updatePostPubStatus("me", 1L, PUB_PENDING)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/status")
                        .servletPath("/v1/posts/1/status")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_STATUS_UPDATE_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"id\":1,\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
    }
}
