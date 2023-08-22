package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.post.ContentObject;
import com.lostsidewalk.buffy.post.PostMedia;
import com.lostsidewalk.buffy.post.StagingPost;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.MediaGroup;
import com.rometools.modules.mediarss.types.Metadata;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PostMediaController.class)
public class PostMediaControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final Metadata TEST_MEDIA_ENTRY_METADATA = new Metadata();
    static {
        // TODO: additional initialization here if necessary
    }

    private static final MediaEntryModuleImpl TEST_MEDIA_ENTRY_MODULE = new MediaEntryModuleImpl();
    static {
        TEST_MEDIA_ENTRY_MODULE.setMetadata(TEST_MEDIA_ENTRY_METADATA);
        TEST_MEDIA_ENTRY_MODULE.setMediaContents(new MediaContent[] {});
        TEST_MEDIA_ENTRY_MODULE.setMediaGroups(new MediaGroup[] {});
    }

    private static final PostMedia TEST_POST_MEDIA;
    static {
        TEST_POST_MEDIA = PostMedia.from(TEST_MEDIA_ENTRY_MODULE);
    }

    private static final ContentObject TEST_POST_TITLE = ContentObject.from("testTitleType", "testTitleValue");

    private static final ContentObject TEST_POST_DESC = ContentObject.from("testDescType", "testDescValue");

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
            new Date(),
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
            null
    );

    @Test
    public void test_getPostMedia() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/media")
                        .servletPath("/posts/1/media")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"postMediaMetadata\":{}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_updatePostMedia() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/media")
                        .servletPath("/posts/1/media")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_POST_MEDIA))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated media on post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).updatePostMedia(eq("me"), eq(1L), any(PostMedia.class));
    }

    @Test
    public void test_deletePostMedia() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/1/media")
                        .servletPath("/posts/1/media")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted media from post Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).clearPostMedia("me", 1L);
    }
}
