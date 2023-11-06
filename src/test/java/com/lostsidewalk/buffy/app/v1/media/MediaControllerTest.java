package com.lostsidewalk.buffy.app.v1.media;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostMedia;
import com.lostsidewalk.buffy.post.StagingPost;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Date;

import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = MediaController.class)
class MediaControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final Metadata TEST_MEDIA_ENTRY_METADATA = new Metadata();
    static {
        // TODO: empty initializer
    }

    private static final MediaEntryModuleImpl TEST_MEDIA_ENTRY_MODULE = new MediaEntryModuleImpl();
    static {
        TEST_MEDIA_ENTRY_MODULE.setMetadata(TEST_MEDIA_ENTRY_METADATA);
//        TEST_MEDIA_ENTRY_MODULE.setMediaContents(new MediaContent[]);
//        TEST_MEDIA_ENTRY_MODULE.setMediaGroups(new MediaGroup[] {});
    }

    private static final PostMedia TEST_POST_MEDIA;
    static {
        TEST_POST_MEDIA = PostMedia.from(TEST_MEDIA_ENTRY_MODULE);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleIdent", "testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescIdent", "testDescType", "testDescValue");

    private static final Date NOW = new Date();

    private static final StagingPost TEST_STAGING_POST = StagingPost.from(
            "testImporterId",
            1L,
            "testImporterDesc",
            2L,
            TEST_POST_TITLE,
            TEST_POST_DESC,
            null,
            TEST_POST_MEDIA,
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
    void test_getPostMedia() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/media")
                        .servletPath("/v1/posts/1/media")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"postMediaMetadata\":{}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostMedia() throws Exception {
        when(stagingPostService.updatePostMedia(eq("me"), eq(1L), any(PostMedia.class), eq(false))).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/media")
                        .servletPath("/v1/posts/1/media")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_MEDIA))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePostMedia(eq("me"), eq(1L), any(PostMedia.class), eq(false));
    }

    @Test
    void test_patchPostMedia() throws Exception {
        when(stagingPostService.updatePostMedia(eq("me"), eq(1L), any(PostMedia.class), eq(true))).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/media")
                        .servletPath("/v1/posts/1/media")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_MEDIA))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePostMedia(eq("me"), eq(1L), any(PostMedia.class), eq(true));
    }

    @Test
    void test_deletePostMedia() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostMedia(null);
        when(stagingPostService.resolveQueueId("me", 1L)).thenReturn(1L);
        when(stagingPostService.findById("me", 1L)).thenReturn(updatedPost);
        when(queueDefinitionService.resolveQueueIdent("me", 1L)).thenReturn("1");
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/media")
                        .servletPath("/v1/posts/1/media")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"queueIdent\":\"1\",\"postTitle\":{\"ident\":\"testTitleIdent\",\"type\":\"testTitleType\",\"value\":\"testTitleValue\"},\"postDesc\":{\"ident\":\"testDescIdent\",\"type\":\"testDescType\",\"value\":\"testDescValue\"},\"postUrl\":\"testPostUrl\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearPostMedia("me", 1L);
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
