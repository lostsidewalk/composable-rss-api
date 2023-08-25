package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.lostsidewalk.buffy.app.model.request.PostConfigRequest;
import com.lostsidewalk.buffy.app.model.request.PostStatusUpdateRequest;
import com.lostsidewalk.buffy.app.model.response.DeployResponse;
import com.lostsidewalk.buffy.app.model.response.PostDTO;
import com.lostsidewalk.buffy.post.*;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.rometools.modules.itunes.EntryInformationImpl;
import com.rometools.modules.itunes.ITunes;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.DEPUB_PENDING;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.PUB_PENDING;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PostController.class)
public class PostControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .create();

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    protected static final ContentObject TEST_POST_TITLE = ContentObject.from("text", "testPostTitle");

    protected static final ContentObject TEST_POST_DESCRIPTION = ContentObject.from("text", "testPostDescription");

    protected static final ContentObject TEST_POST_CONTENT = ContentObject.from("text", "testPostContent");

    protected static final PostMedia TEST_POST_MEDIA;
    static {
        MediaEntryModuleImpl testMediaEntryModule = new MediaEntryModuleImpl();
        Metadata metadata = new Metadata();
        testMediaEntryModule.setMetadata(metadata);
        TEST_POST_MEDIA = PostMedia.from(testMediaEntryModule);
    }

    protected static final PostITunes TEST_POST_ITUNES;
    static {
        ITunes testITunes = new EntryInformationImpl();
        testITunes.setKeywords(new String[] { "test"});
        TEST_POST_ITUNES = PostITunes.from(testITunes);
    }

    protected static final PostUrl TEST_POST_URL = new PostUrl();
    static {
        TEST_POST_URL.setTitle("testUrlTitle");
        TEST_POST_URL.setRel("testUrlRel");
        TEST_POST_URL.setHref("testUrlHref");
        TEST_POST_URL.setHreflang("testUrlHreflang");
        TEST_POST_URL.setType("testUrlType");
    }

    protected static final PostPerson TEST_POST_CONTRIBUTOR = new PostPerson();
    static {
        TEST_POST_CONTRIBUTOR.setName("testContributorName");
        TEST_POST_CONTRIBUTOR.setUri("testContributorUri");
        TEST_POST_CONTRIBUTOR.setEmail("testContributorEmail");
    }

    protected static final PostPerson TEST_POST_AUTHOR = new PostPerson();
    static {
        TEST_POST_AUTHOR.setName("testAuthorName");
        TEST_POST_AUTHOR.setUri("testAuthorUri");
        TEST_POST_AUTHOR.setEmail("testAuthorEmail");
    }

    protected static final PostEnclosure TEST_POST_ENCLOSURE = new PostEnclosure();
    static {
        TEST_POST_ENCLOSURE.setType("testEnclosureType");
        TEST_POST_ENCLOSURE.setUrl("testEnclosureUrl");
        TEST_POST_ENCLOSURE.setLength(4821L);
    }

    private static final Date YESTERDAY = new Date(10_000_000L);

    private static final Date NOW = new Date(15_000_000L);

    private static final Date THIRTY_DAYS_FROM_NOW = new Date(50_000_000L);

    private static final StagingPost TEST_STAGING_POST = StagingPost.from(
            "testImporterId",
            1L,
            "testImporterDesc",
            2L,
            TEST_POST_TITLE,
            TEST_POST_DESCRIPTION,
            List.of(TEST_POST_CONTENT),
            TEST_POST_MEDIA,
            TEST_POST_ITUNES,
            "testPostUrl",
            List.of(TEST_POST_URL),
            "testPostImgUrl",
            null, // import timestamp
            "testPostHash",
            "me",
            "testPostComment",
            "testPostRights",
            List.of(TEST_POST_CONTRIBUTOR),
            List.of(TEST_POST_AUTHOR),
            List.of("testPostCategory"),
            YESTERDAY, // publish timestamp
            THIRTY_DAYS_FROM_NOW, // expiration timestamp
            List.of(TEST_POST_ENCLOSURE),
            NOW // last updated timestamp
    );
    static {
        TEST_STAGING_POST.setId(1L);
        TEST_STAGING_POST.setPostPubStatus(PUB_PENDING);
    }

    private static final List<StagingPost> TEST_STAGING_POSTS = List.of(
            TEST_STAGING_POST
    );

    private static final StagingPost TEST_NON_DEPLOYED_STAGING_POST = StagingPost.from(
            "testImporterId",
            1L,
            "testImporterDesc",
            2L,
            TEST_POST_TITLE,
            TEST_POST_DESCRIPTION,
            List.of(TEST_POST_CONTENT),
            TEST_POST_MEDIA,
            TEST_POST_ITUNES,
            "testPostUrl",
            List.of(TEST_POST_URL),
            "testPostImgUrl",
            null, // import timestamp
            "testPostHash",
            "me",
            "testPostComment",
            "testPostRights",
            List.of(TEST_POST_CONTRIBUTOR),
            List.of(TEST_POST_AUTHOR),
            List.of("testPostCategory"),
            null, // publish timestamp
            null, // expiration timestamp
            List.of(TEST_POST_ENCLOSURE),
            null // last updated timestamp
    );
    static {
        TEST_NON_DEPLOYED_STAGING_POST.setId(1L);
    }

    private static final PostConfigRequest TEST_POST_CONFIG_REQUEST = new PostConfigRequest();
    static {
        TEST_POST_CONFIG_REQUEST.setPostTitle(TEST_STAGING_POST.getPostTitle());
        TEST_POST_CONFIG_REQUEST.setPostDesc(TEST_STAGING_POST.getPostDesc());
        TEST_POST_CONFIG_REQUEST.setPostContents(TEST_STAGING_POST.getPostContents());
        TEST_POST_CONFIG_REQUEST.setPostITunes(TEST_STAGING_POST.getPostITunes());
        TEST_POST_CONFIG_REQUEST.setPostUrl(TEST_STAGING_POST.getPostUrl());
        TEST_POST_CONFIG_REQUEST.setPostUrls(TEST_STAGING_POST.getPostUrls());
        TEST_POST_CONFIG_REQUEST.setPostComment(TEST_STAGING_POST.getPostComment());
        TEST_POST_CONFIG_REQUEST.setPostRights(TEST_STAGING_POST.getPostRights());
        TEST_POST_CONFIG_REQUEST.setContributors(TEST_STAGING_POST.getContributors());
        TEST_POST_CONFIG_REQUEST.setAuthors(TEST_STAGING_POST.getAuthors());
        TEST_POST_CONFIG_REQUEST.setPostCategories(TEST_STAGING_POST.getPostCategories());
        TEST_POST_CONFIG_REQUEST.setExpirationTimestamp(TEST_STAGING_POST.getExpirationTimestamp());
        TEST_POST_CONFIG_REQUEST.setEnclosures(TEST_STAGING_POST.getEnclosures());
    }

    private static final List<PostConfigRequest> TEST_POST_CONFIG_REQUESTS = List.of(TEST_POST_CONFIG_REQUEST);

    private static final PostDTO TEST_POST_UPDATE_RESULT = PostDTO.from(
            1L,
            1L,
            TEST_POST_CONFIG_REQUEST.getPostTitle(),
            TEST_POST_CONFIG_REQUEST.getPostDesc(),
            TEST_POST_CONFIG_REQUEST.getPostContents(),
            TEST_POST_CONFIG_REQUEST.getPostITunes(),
            TEST_POST_CONFIG_REQUEST.getPostUrl(),
            TEST_POST_CONFIG_REQUEST.getPostUrls(),
            TEST_POST_CONFIG_REQUEST.getPostComment(),
            TEST_POST_CONFIG_REQUEST.getPostRights(),
            TEST_POST_CONFIG_REQUEST.getContributors(),
            TEST_POST_CONFIG_REQUEST.getAuthors(),
            TEST_POST_CONFIG_REQUEST.getPostCategories(),
            YESTERDAY,
            TEST_POST_CONFIG_REQUEST.getExpirationTimestamp(),
            TEST_POST_CONFIG_REQUEST.getEnclosures(),
            NOW,
            false
    );

    @Test
    void test_createPosts() throws Exception {
        when(this.stagingPostService.createPost("me", 1L, TEST_POST_CONFIG_REQUEST)).thenReturn(1L);
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/posts/1")
                        .servletPath("/posts/1")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONFIG_REQUESTS))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("[{\"id\":1,\"queueId\":1,\"postTitle\":{\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"type\":\"text\",\"value\":\"testPostContent\"}],\"postITunes\":{\"keywords\":[\"test\"],\"block\":false,\"explicit\":false,\"closeCaptioned\":false},\"postUrl\":\"testPostUrl\",\"postUrls\":[{\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"importTimestamp\":null,\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"testPostCategory\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-01T13:53:20.000+00:00\",\"enclosures\":[{\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false}]", JsonArray.class),
                            GSON.fromJson(responseContent, JsonArray.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPosts() throws Exception {
        when(this.stagingPostService.getStagingPosts("me", List.of(1L))).thenReturn(TEST_STAGING_POSTS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("[{\"id\":1,\"queueId\":1,\"postTitle\":{\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"type\":\"text\",\"value\":\"testPostContent\"}],\"postITunes\":{\"keywords\":[\"test\"],\"block\":false,\"explicit\":false,\"closeCaptioned\":false},\"postUrl\":\"testPostUrl\",\"postUrls\":[{\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"importTimestamp\":null,\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"testPostCategory\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-01T13:53:20.000+00:00\",\"enclosures\":[{\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false}]", JsonArray.class),
                            GSON.fromJson(responseContent, JsonArray.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostStatus() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/status")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("PUB_PENDING", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostQueueId_text() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/queue")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("1", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostQueueId_json() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/queue")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("1", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostTitle() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/title")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    ContentObject postTitle = GSON.fromJson(responseContent, ContentObject.class);
                    assertEquals(TEST_POST_TITLE, postTitle);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostDesc() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/desc")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    ContentObject postDesc = GSON.fromJson(responseContent, ContentObject.class);
                    assertEquals(TEST_POST_DESCRIPTION, postDesc);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostITunes() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/itunes")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_ITUNES, GSON.fromJson(responseContent, PostITunes.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostComment_text() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/comment")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testPostComment", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostComment_json() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/comment")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testPostComment", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostRights_text() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/rights")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testPostRights", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPostRights_json() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/rights")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testPostRights", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    private static final SimpleDateFormat ISO_8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Test
    void test_getExpirationTimestamp_text() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/expiration")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isNotBlank(responseContent));
                    Date expirationDate = null;
                    try {
                        expirationDate = ISO_8601_TIMESTAMP_FORMAT.parse(responseContent);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                    assertEquals(expirationDate, THIRTY_DAYS_FROM_NOW);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getExpirationTimestamp_json() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/expiration")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(THIRTY_DAYS_FROM_NOW, GSON.fromJson(responseContent, Date.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getExpirationTimestamp_json_nonDeployed() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_NON_DEPLOYED_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/expiration")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isEmpty(GSON.fromJson(responseContent, String.class)));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPublishedTimestamp_text() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/published")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isNotBlank(responseContent));
                    Date published = null;
                    try {
                        published = ISO_8601_TIMESTAMP_FORMAT.parse(responseContent);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                    assertEquals(published, YESTERDAY);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPublishedTimestamp_json() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/published")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(YESTERDAY, GSON.fromJson(responseContent, Date.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getPublishedTimestamp_json_nonDeployed() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_NON_DEPLOYED_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/published")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isEmpty(GSON.fromJson(responseContent, String.class)));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getLastUpdatedTimestamp_text() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/updated")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isNotBlank(responseContent));
                    Date lastUpdated = null;
                    try {
                        lastUpdated = ISO_8601_TIMESTAMP_FORMAT.parse(responseContent);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                    assertEquals(lastUpdated, NOW);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getLastUpdatedTimestamp_json() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/updated")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(NOW, GSON.fromJson(responseContent, Date.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getLastUpdatedTimestamp_json_nonDeployed() throws Exception {
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_NON_DEPLOYED_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/posts/1/updated")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isEmpty(GSON.fromJson(responseContent, String.class)));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePost() throws Exception {
        when(this.stagingPostService.updatePost("me", 1L, TEST_POST_CONFIG_REQUEST)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1")
                        .servletPath("/posts/1")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONFIG_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    PostDTO updatedPost = GSON.fromJson(responseContent, PostDTO.class);
                    assertEquals(
                            TEST_POST_UPDATE_RESULT,
                            updatedPost
                    );
                })
                .andExpect(status().isOk());
    }

    private static final PostStatusUpdateRequest TEST_POST_STATUS_UPDATE_REQUEST = new PostStatusUpdateRequest();
    static {
        TEST_POST_STATUS_UPDATE_REQUEST.setNewStatus(PUB_PENDING.name());
    }

    private static final PubResult TEST_PUB_RESULT = PubResult.from("testPublisher", emptyList(), NOW);

    private static final List<PubResult> TEST_PUB_RESULTS = List.of(TEST_PUB_RESULT);

    private static final DeployResponse TEST_DEPLOY_RESPONSE = DeployResponse.from(TEST_PUB_RESULTS);

    @Test
    void test_updatePostStatus() throws Exception {
        when(this.stagingPostService.updatePostPubStatus("me", 1L, PUB_PENDING)).thenReturn(TEST_PUB_RESULTS);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/status")
                        .servletPath("/posts/1/status")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_STATUS_UPDATE_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_DEPLOY_RESPONSE, GSON.fromJson(responseContent, DeployResponse.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostTitle() throws Exception {
        when(this.stagingPostService.updatePostTitle("me", 1L, TEST_POST_TITLE)).thenReturn(TEST_POST_TITLE);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/title")
                        .servletPath("/posts/1/title")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_TITLE))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_TITLE, GSON.fromJson(responseContent, ContentObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostDesc() throws Exception {
        when(this.stagingPostService.updatePostDesc("me", 1L, TEST_POST_DESCRIPTION)).thenReturn(TEST_POST_DESCRIPTION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/desc")
                        .servletPath("/posts/1/desc")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_DESCRIPTION))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_DESCRIPTION, GSON.fromJson(responseContent, ContentObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostITunes() throws Exception {
        when(this.stagingPostService.updatePostITunes("me", 1L, TEST_POST_ITUNES)).thenReturn(TEST_POST_ITUNES);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/itunes")
                        .servletPath("/posts/1/itunes")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_ITUNES))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_ITUNES, GSON.fromJson(responseContent, PostITunes.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostComment_text() throws Exception {
        when(this.stagingPostService.updatePostComment("me", 1L, "testComment")).thenReturn("testComment");
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/comment")
                        .servletPath("/posts/1/comment")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testComment")
                        .accept(TEXT_PLAIN_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testComment", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostComment_json() throws Exception {
        when(this.stagingPostService.updatePostComment("me", 1L, "testComment")).thenReturn("testComment");
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/comment")
                        .servletPath("/posts/1/comment")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson("testComment"))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testComment", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostRights_text() throws Exception {
        when(this.stagingPostService.updatePostRights("me", 1L, "testRights")).thenReturn("testRights");
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/rights")
                        .servletPath("/posts/1/rights")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testRights")
                        .accept(TEXT_PLAIN_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testRights", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostRights_json() throws Exception {
        when(this.stagingPostService.updatePostRights("me", 1L, "testRights")).thenReturn("testRights");
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/rights")
                        .servletPath("/posts/1/rights")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson("testRights"))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testRights", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateExpirationTimestamp_text() throws Exception {
        when(this.stagingPostService.updateExpirationTimestamp("me", 1L, THIRTY_DAYS_FROM_NOW)).thenReturn(THIRTY_DAYS_FROM_NOW);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/expiration")
                        .servletPath("/posts/1/expiration")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content(ISO_8601_TIMESTAMP_FORMAT.format(THIRTY_DAYS_FROM_NOW))
                        .accept(TEXT_PLAIN_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(THIRTY_DAYS_FROM_NOW, ISO_8601_TIMESTAMP_FORMAT.parse(responseContent));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateExpirationTimestamp_json() throws Exception {
        when(this.stagingPostService.updateExpirationTimestamp("me", 1L, THIRTY_DAYS_FROM_NOW)).thenReturn(THIRTY_DAYS_FROM_NOW);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/expiration")
                        .servletPath("/posts/1/expiration")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(ISO_8601_TIMESTAMP_FORMAT.format(THIRTY_DAYS_FROM_NOW))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(THIRTY_DAYS_FROM_NOW, GSON.fromJson(responseContent, Date.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateExpirationTimestamp_json_invalidTimestamp() throws Exception {
        when(this.stagingPostService.updateExpirationTimestamp("me", 1L, THIRTY_DAYS_FROM_NOW)).thenReturn(THIRTY_DAYS_FROM_NOW);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/posts/1/expiration")
                        .servletPath("/posts/1/expiration")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content("aaaaa")
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                ).andExpect(status().isBadRequest());
    }

    @Test
    void test_deletePost() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/1")
                        .servletPath("/posts/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"message\":\"Deleted post Id 1\"}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePostPubStatus("me", 1L, DEPUB_PENDING);
        verify(stagingPostService).deletePost("me", 1L);
    }

    @Test
    void test_deletePostITunes() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/1/itunes")
                        .servletPath("/posts/1/itunes")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"message\":\"Deleted iTunes descriptor from post Id 1\"}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearPostITunes("me", 1L);
    }

    @Test
    void test_deletePostComment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/1/comment")
                        .servletPath("/posts/1/comment")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"message\":\"Deleted comment string from post Id 1\"}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearPostComment("me", 1L);
    }

    @Test
    void test_deletePostRights() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/1/rights")
                        .servletPath("/posts/1/rights")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"message\":\"Deleted rights string from post Id 1\"}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearPostRights("me", 1L);
    }

    @Test
    void test_deleteExpirationTimestamp() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/posts/1/expiration")
                        .servletPath("/posts/1/expiration")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"message\":\"Deleted expiration timestamp string from post Id 1\"}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearExpirationTimestamp("me", 1L);
    }
}
