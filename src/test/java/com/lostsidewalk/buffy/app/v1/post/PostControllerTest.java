package com.lostsidewalk.buffy.app.v1.post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.request.*;
import com.lostsidewalk.buffy.post.*;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import com.rometools.modules.itunes.EntryInformation;
import com.rometools.modules.itunes.EntryInformationImpl;
import com.rometools.modules.itunes.types.Duration;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.types.Metadata;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.PUB_PENDING;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PostController.class)
class PostControllerTest extends BaseWebControllerTest {

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

    private static final ContentObject TEST_POST_TITLE = new ContentObject();
    static {
        TEST_POST_TITLE.setIdent("2");
        TEST_POST_TITLE.setType("text");
        TEST_POST_TITLE.setValue("testPostTitle");
    }

    private static final ContentObject TEST_POST_DESCRIPTION = new ContentObject();
    static {
        TEST_POST_DESCRIPTION.setIdent("2");
        TEST_POST_DESCRIPTION.setType("text");
        TEST_POST_DESCRIPTION.setValue("testPostDescription");
    }

    private static final ContentObject TEST_POST_CONTENT = new ContentObject();
    static {
        TEST_POST_CONTENT.setIdent("2");
        TEST_POST_CONTENT.setValue("testPostContent");
    }

    private static final PostMedia TEST_POST_MEDIA;
    static {
        MediaEntryModuleImpl testMediaEntryModule = new MediaEntryModuleImpl();
        Metadata metadata = new Metadata();
        testMediaEntryModule.setMetadata(metadata);
        TEST_POST_MEDIA = PostMedia.from(testMediaEntryModule);
    }

    private static final PostITunes TEST_POST_ITUNES;
    static {
        EntryInformation testITunes = new EntryInformationImpl();
        testITunes.setAuthor("testAuthor");
        testITunes.setBlock(false);
        testITunes.setExplicit(true);
        testITunes.setExplicitNullable(true);
        //
        String imageUri = "http://localhost:666/testImageUri";
        testITunes.setImageUri(imageUri);
        URL imageUrl = null;
        try {
            imageUrl = new URL(imageUri);
        } catch (MalformedURLException e) {
            fail(e.getMessage());
        }
        testITunes.setImage(imageUrl);
        //
        testITunes.setKeywords(new String[] { "test"});
        testITunes.setSubtitle("testSubtitle");
        testITunes.setSummary("testSummary");
        Duration duration = new Duration(1000 * 60 * 60); // 1 hr
        testITunes.setClosedCaptioned(true);
        testITunes.setDuration(duration);
        testITunes.setEpisode(1);
        testITunes.setEpisodeType("FULL");
        testITunes.setOrder(1);
        testITunes.setSeason(1);
        testITunes.setTitle("testTitle");
        TEST_POST_ITUNES = PostITunes.from(testITunes);
    }

    private static final PostUrl TEST_POST_URL = new PostUrl();
    static {
        TEST_POST_URL.setIdent("2");
        TEST_POST_URL.setTitle("testUrlTitle");
        TEST_POST_URL.setType("testUrlType");
        TEST_POST_URL.setHref("testUrlHref");
        TEST_POST_URL.setHreflang("testUrlHreflang");
        TEST_POST_URL.setRel("testUrlRel");
    }

    private static final String TEST_POST_IMG_URL = "testPostImgUrl";

    private static final String TEST_POST_HASH = "testPostHash";

    private static final String TEST_POST_COMMENT = "testPostComment";

    private static final String TEST_POST_RIGHTS = "testPostRights";

    private static final PostPerson TEST_POST_CONTRIBUTOR = new PostPerson();
    static {
        TEST_POST_CONTRIBUTOR.setName("testContributorName");
        TEST_POST_CONTRIBUTOR.setUri("testContributorUri");
        TEST_POST_CONTRIBUTOR.setEmail("testContributorEmail");
    }

    private static final PostPerson TEST_POST_AUTHOR = new PostPerson();
    static {
        TEST_POST_AUTHOR.setName("testAuthorName");
        TEST_POST_AUTHOR.setUri("testAuthorUri");
        TEST_POST_AUTHOR.setEmail("testAuthorEmail");
    }

    private static final String TEST_POST_CATEGORY = "category";

    private static final PostEnclosure TEST_POST_ENCLOSURE = new PostEnclosure();
    static {
        TEST_POST_ENCLOSURE.setIdent("2");
        TEST_POST_ENCLOSURE.setUrl("testEnclosureUrl");
        TEST_POST_ENCLOSURE.setType("testEnclosureType");
        TEST_POST_ENCLOSURE.setLength(4821L);
    }

    private static final Instant YESTERDAY = Instant.ofEpochMilli(10_000_000L);

    private static final Instant NOW = Instant.ofEpochMilli(15_000_000L);

    private static final Instant THIRTY_DAYS_FROM_NOW = NOW.plus(30, DAYS);

    private static final QueueDefinition TEST_QUEUE_DEFINITION = QueueDefinition.from(
            "testQueue",
            "Test Queue Title",
            "Test Queue Description",
            "Test Queue Feed Generator",
            "Test Queue Transport Identifier",
            "me",
            null,
            "Test Queue Copyright",
            "en-US",
            "testQueueImageSource",
            false);
    static {
        TEST_QUEUE_DEFINITION.setId(1L);
    }

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
            TEST_POST_URL.getHref(),
            List.of(TEST_POST_URL),
            TEST_POST_IMG_URL,
            null,
            Date.from(YESTERDAY), // import timestamp
            TEST_POST_HASH,
            "me",
            TEST_POST_COMMENT,
            TEST_POST_RIGHTS,
            List.of(TEST_POST_CONTRIBUTOR),
            List.of(TEST_POST_AUTHOR),
            List.of(TEST_POST_CATEGORY),
            Date.from(YESTERDAY), // publish timestamp
            Date.from(THIRTY_DAYS_FROM_NOW), // expiration timestamp
            List.of(TEST_POST_ENCLOSURE),
            Date.from(NOW), // last updated timestamp,
            Date.from(YESTERDAY), // created
            null // last modified
    );
    static {
        TEST_STAGING_POST.setId(1L);
        TEST_STAGING_POST.setPostPubStatus(PUB_PENDING);
    }

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
            TEST_POST_URL.getHref(),
            List.of(TEST_POST_URL),
            TEST_POST_IMG_URL,
            null,
            Date.from(YESTERDAY), // import timestamp
            TEST_POST_HASH,
            "me",
            TEST_POST_COMMENT,
            TEST_POST_RIGHTS,
            List.of(TEST_POST_CONTRIBUTOR),
            List.of(TEST_POST_AUTHOR),
            List.of(TEST_POST_CATEGORY),
            null, // publish timestamp (never)
            null, // expiration timestamp
            List.of(TEST_POST_ENCLOSURE),
            null, // last updated timestamp (never)
            Date.from(YESTERDAY), // created
            null // last modified
    );
    static {
        TEST_NON_DEPLOYED_STAGING_POST.setId(1L);
    }

    @Test
    void test_getPost() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"id\":1,\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postITunes\":{\"author\":\"testAuthor\",\"imageUri\":\"http://localhost:666/testImageUri\",\"keywords\":[\"test\"],\"subTitle\":\"testSubtitle\",\"summary\":\"testSummary\",\"duration\":3600000,\"episode\":1,\"episodeType\":\"FULL\",\"order\":1,\"season\":1,\"title\":\"testTitle\",\"isBlock\":false,\"isExplicit\":true,\"isCloseCaptioned\":true},\"postUrl\":\"testUrlHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"category\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-31T04:10:00.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"postPubStatus\":\"PUB_PENDING\",\"published\":false}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_getPostQueueIdent_text() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        when(queueDefinitionService.resolveQueueIdent("me", 1L)).thenReturn("testQueue");
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/queue")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testQueue", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_getPostQueueIdent_json() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        when(queueDefinitionService.resolveQueueIdent("me", 1L)).thenReturn("testQueue");
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/queue")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testQueue", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_getPostTitle() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/title")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    ContentObject postTitle = GSON.fromJson(responseContent, ContentObject.class);
                    assertEquals(TEST_POST_TITLE, postTitle);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPostDescription() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/description")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    ContentObject postDesc = GSON.fromJson(responseContent, ContentObject.class);
                    assertEquals(TEST_POST_DESCRIPTION, postDesc);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPostITunes() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/itunes")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"author\":\"testAuthor\",\"imageUri\":\"http://localhost:666/testImageUri\",\"keywords\":[\"test\"],\"subTitle\":\"testSubtitle\",\"summary\":\"testSummary\",\"duration\":3600000,\"episode\":1,\"episodeType\":\"FULL\",\"order\":1,\"season\":1,\"title\":\"testTitle\",\"isBlock\":false,\"isExplicit\":true,\"isCloseCaptioned\":true}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPostComment_text() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/comment")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testPostComment", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPostComment_json() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/comment")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testPostComment", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPostRights_text() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/rights")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testPostRights", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPostRights_json() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/rights")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testPostRights", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPostCategories() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/categories")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[\"category\"]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    private static final DateTimeFormatter ISO_8601_TIMESTAMP_FORMATTER = ISO_INSTANT;

    @Test
    void test_getExpirationTimestamp_text() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/expiration")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isNotBlank(responseContent));
                    Instant instant = null;
                    try {
                        instant = Instant.from(ISO_8601_TIMESTAMP_FORMATTER.parse(responseContent));
                    } catch (DateTimeParseException e) {
                        fail(e.getMessage());
                    }
                    assertEquals(THIRTY_DAYS_FROM_NOW, instant);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getExpirationTimestamp_json() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/expiration")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    Instant parsedInstant = Instant.from(
                            ZonedDateTime.parse(GSON.fromJson(responseContent, String.class), ISO_OFFSET_DATE_TIME)
                    );
                    assertEquals(THIRTY_DAYS_FROM_NOW, parsedInstant);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getExpirationTimestamp_json_nonDeployed() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_NON_DEPLOYED_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/expiration")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isEmpty(GSON.fromJson(responseContent, String.class)));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPublishedTimestamp_text() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/published")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isNotBlank(responseContent));
                    Instant instant = null;
                    try {
                        instant = Instant.from(ISO_8601_TIMESTAMP_FORMATTER.parse(responseContent));
                    } catch (DateTimeParseException e) {
                        fail(e.getMessage());
                    }
                    assertEquals(YESTERDAY, instant);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPublishedTimestamp_json() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/published")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    Instant parsedInstant = Instant.from(
                            ZonedDateTime.parse(GSON.fromJson(responseContent, String.class), ISO_OFFSET_DATE_TIME)
                    );
                    assertEquals(YESTERDAY, parsedInstant);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getPublishedTimestamp_json_nonDeployed() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_NON_DEPLOYED_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/published")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isEmpty(GSON.fromJson(responseContent, String.class)));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getLastUpdatedTimestamp_text() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/updated")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isNotBlank(responseContent));
                    Instant instant = null;
                    try {
                        instant = Instant.from(ISO_8601_TIMESTAMP_FORMATTER.parse(responseContent));
                    } catch (DateTimeParseException e) {
                        fail(e.getMessage());
                    }
                    assertEquals(NOW, instant);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getLastUpdatedTimestamp_json() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/updated")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    Instant parsedInstant = Instant.from(
                            ZonedDateTime.parse(GSON.fromJson(responseContent, String.class), ISO_OFFSET_DATE_TIME)
                    );
                    assertEquals(NOW, parsedInstant);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    @Test
    void test_getLastUpdatedTimestamp_json_nonDeployed() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_NON_DEPLOYED_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/posts/1/updated")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isEmpty(GSON.fromJson(responseContent, String.class)));
                })
                .andExpect(status().isOk());
        verify(stagingPostService).findById("me", 1L);
    }

    //
    //
    //

    private static final ContentObjectConfigRequest TEST_POST_TITLE_CONFIG_REQUEST = new ContentObjectConfigRequest();
    static {
        // TODO: empty initializer
    }

    private static final ContentObjectConfigRequest TEST_POST_DESC_CONFIG_REQUEST = new ContentObjectConfigRequest();
    static {
        // TODO: empty initializer
    }

    private static final ContentObjectConfigRequest TEST_POST_CONTENT_CONFIG_REQUEST = new ContentObjectConfigRequest();
    static {
        // TODO: empty initializer
    }

    private static final List<ContentObjectConfigRequest> TEST_POST_CONTENTS_CONFIG_REQUEST = List.of(TEST_POST_CONTENT_CONFIG_REQUEST);

    private static final PostUrlConfigRequest TEST_POST_URL_CONFIG_REQUEST = new PostUrlConfigRequest();
    static {
        // TODO: empty initializer
    }

    private static final List<PostUrlConfigRequest> TEST_POST_URLS_CONFIG_REQUEST = List.of(TEST_POST_URL_CONFIG_REQUEST);

    private static final PostPersonConfigRequest TEST_AUTHOR_CONFIG_REQUEST = new PostPersonConfigRequest();
    static {
        // TODO: empty initializer
    }

    private static final List<PostPersonConfigRequest> TEST_AUTHORS_CONFIG_REQUEST = List.of(TEST_AUTHOR_CONFIG_REQUEST);

    private static final PostPersonConfigRequest TEST_CONTRIBUTOR_CONFIG_REQUEST = new PostPersonConfigRequest();
    static {
        // TODO: empty initializer
    }

    private static final List<PostPersonConfigRequest> TEST_CONTRIBUTORS_CONFIG_REQUEST = List.of(TEST_CONTRIBUTOR_CONFIG_REQUEST);

    private static final List<String> TEST_POST_CATEGORIES_CONFIG_REQUEST = List.of("testCategory");

    private static final PostEnclosureConfigRequest TEST_ENCLOSURE_CONFIG_REQUEST = new PostEnclosureConfigRequest();
    static {
        // TODO: empty initializer
    }

    private static final List<PostEnclosureConfigRequest> TEST_ENCLOSURES_CONFIG_REQUEST = List.of(TEST_ENCLOSURE_CONFIG_REQUEST);

    private static final PostConfigRequest TEST_POST_CONFIG_REQUEST = new PostConfigRequest();
    static {
        TEST_POST_CONFIG_REQUEST.setPostTitle(TEST_POST_TITLE_CONFIG_REQUEST);
        TEST_POST_CONFIG_REQUEST.setPostDesc(TEST_POST_DESC_CONFIG_REQUEST);
        TEST_POST_CONFIG_REQUEST.setPostContents(TEST_POST_CONTENTS_CONFIG_REQUEST);
        TEST_POST_CONFIG_REQUEST.setPostITunes(TEST_POST_ITUNES);
        TEST_POST_CONFIG_REQUEST.setPostUrl("");
        TEST_POST_CONFIG_REQUEST.setPostUrls(TEST_POST_URLS_CONFIG_REQUEST);
        TEST_POST_CONFIG_REQUEST.setPostComment("");
        TEST_POST_CONFIG_REQUEST.setPostRights("");
        TEST_POST_CONFIG_REQUEST.setContributors(TEST_CONTRIBUTORS_CONFIG_REQUEST);
        TEST_POST_CONFIG_REQUEST.setAuthors(TEST_AUTHORS_CONFIG_REQUEST);
        TEST_POST_CONFIG_REQUEST.setPostCategories(TEST_POST_CATEGORIES_CONFIG_REQUEST);
        TEST_POST_CONFIG_REQUEST.setExpirationTimestamp(Date.from(THIRTY_DAYS_FROM_NOW));
        TEST_POST_CONFIG_REQUEST.setEnclosures(TEST_ENCLOSURES_CONFIG_REQUEST);
    }

    private static final String TEST_POST_RESPONSE = "{\"postDTO\":{\"id\":1,\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postITunes\":{\"author\":\"testAuthor\",\"imageUri\":\"http://localhost:666/testImageUri\",\"keywords\":[\"test\"],\"subTitle\":\"testSubtitle\",\"summary\":\"testSummary\",\"duration\":3600000,\"episode\":1,\"episodeType\":\"FULL\",\"order\":1,\"season\":1,\"title\":\"testTitle\",\"isBlock\":false,\"isExplicit\":true,\"isCloseCaptioned\":true},\"postUrl\":\"testUrlHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"category\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-31T04:10:00.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"postPubStatus\":\"PUB_PENDING\",\"published\":false},\"deployed\":false}";

    @Test
    void test_updatePost() throws Exception {
        when(queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        when(stagingPostService.updatePost("me", 1L, TEST_POST_CONFIG_REQUEST, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1")
                        .servletPath("/v1/posts/1")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONFIG_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePost("me", 1L, TEST_POST_CONFIG_REQUEST, false);
        queueDefinitionService.isAutoDeploy("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_patchPost() throws Exception {
        when(queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        when(stagingPostService.updatePost("me", 1L, TEST_POST_CONFIG_REQUEST, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1")
                        .servletPath("/v1/posts/1")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONFIG_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson(TEST_POST_RESPONSE, JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class)
                    );
                })
                .andExpect(status().isOk());
        verify(stagingPostService).updatePost("me", 1L, TEST_POST_CONFIG_REQUEST, true);
        queueDefinitionService.isAutoDeploy("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_updatePostTitle() throws Exception {
        when(stagingPostService.updatePostTitle("me", 1L, TEST_POST_TITLE, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/title")
                        .servletPath("/v1/posts/1/title")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_TITLE))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostDescription() throws Exception {
        when(stagingPostService.updatePostDesc("me", 1L, TEST_POST_DESCRIPTION, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/description")
                        .servletPath("/v1/posts/1/description")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_DESCRIPTION))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostITunes() throws Exception {
        when(stagingPostService.updatePostITunes("me", 1L, TEST_POST_ITUNES, false)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/itunes")
                        .servletPath("/v1/posts/1/itunes")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_ITUNES))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostComment_text() throws Exception {
        when(stagingPostService.updatePostComment("me", 1L, TEST_POST_COMMENT)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/comment")
                        .servletPath("/v1/posts/1/comment")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content(TEST_POST_COMMENT)
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostComment_json() throws Exception {
        when(stagingPostService.updatePostComment("me", 1L, "testComment")).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/comment")
                        .servletPath("/v1/posts/1/comment")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson("testComment"))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostRights_text() throws Exception {
        when(stagingPostService.updatePostRights("me", 1L, "testRights")).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/rights")
                        .servletPath("/v1/posts/1/rights")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testRights")
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostRights_json() throws Exception {
        when(stagingPostService.updatePostRights("me", 1L, "testRights")).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/rights")
                        .servletPath("/v1/posts/1/rights")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson("testRights"))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updatePostCategories_json() throws Exception {
        when(stagingPostService.updatePostCategories("me", 1L, List.of("testCategory"))).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/categories")
                        .servletPath("/v1/posts/1/categories")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(List.of("testCategory")))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateExpirationTimestamp_json() throws Exception {
        when(stagingPostService.updateExpirationTimestamp("me", 1L, Date.from(THIRTY_DAYS_FROM_NOW))).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/expiration")
                        .servletPath("/v1/posts/1/expiration")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(ISO_8601_TIMESTAMP_FORMATTER.format(THIRTY_DAYS_FROM_NOW)))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateExpirationTimestamp_json_invalidTimestamp() throws Exception {
        when(stagingPostService.updateExpirationTimestamp("me", 1L, Date.from(THIRTY_DAYS_FROM_NOW))).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/posts/1/expiration")
                        .servletPath("/v1/posts/1/expiration")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content("aaaaa")
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                ).andExpect(status().isBadRequest());
    }

    @Test
    void test_patchPostTitle() throws Exception {
        when(stagingPostService.updatePostTitle("me", 1L, TEST_POST_TITLE, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/title")
                        .servletPath("/v1/posts/1/title")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_TITLE))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchPostDescription() throws Exception {
        when(stagingPostService.updatePostDesc("me", 1L, TEST_POST_DESCRIPTION, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/description")
                        .servletPath("/v1/posts/1/description")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_DESCRIPTION))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchPostITunes() throws Exception {
        when(stagingPostService.updatePostITunes("me", 1L, TEST_POST_ITUNES, true)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/itunes")
                        .servletPath("/v1/posts/1/itunes")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_ITUNES))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchPostComment_text() throws Exception {
        when(stagingPostService.updatePostComment("me", 1L, "testPostComment")).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/comment")
                        .servletPath("/v1/posts/1/comment")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testPostComment")
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchPostComment_json() throws Exception {
        when(stagingPostService.updatePostComment("me", 1L, "testComment")).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/comment")
                        .servletPath("/v1/posts/1/comment")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson("testComment"))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchPostRights_text() throws Exception {
        when(stagingPostService.updatePostRights("me", 1L, "testRights")).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/rights")
                        .servletPath("/v1/posts/1/rights")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testRights")
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchPostRights_json() throws Exception {
        when(stagingPostService.updatePostRights("me", 1L, "testRights")).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/rights")
                        .servletPath("/v1/posts/1/rights")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson("testRights"))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchPostCategories_json() throws Exception {
        when(stagingPostService.updatePostCategories("me", 1L, List.of("testCategory"))).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/categories")
                        .servletPath("/v1/posts/1/categories")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(List.of("testCategory")))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchExpirationTimestamp_json() throws Exception {
        when(stagingPostService.updateExpirationTimestamp("me", 1L, Date.from(THIRTY_DAYS_FROM_NOW))).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/posts/1/expiration")
                        .servletPath("/v1/posts/1/expiration")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(ISO_8601_TIMESTAMP_FORMATTER.format(THIRTY_DAYS_FROM_NOW)))
                        .accept(APPLICATION_JSON_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(TEST_POST_RESPONSE, responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchExpirationTimestamp_json_invalidTimestamp() throws Exception {
        when(stagingPostService.updateExpirationTimestamp("me", 1L, Date.from(THIRTY_DAYS_FROM_NOW))).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                .patch("/v1/posts/1/expiration")
                .servletPath("/v1/posts/1/expiration")
                .contentType(APPLICATION_JSON_VALUE)
                .content("aaaaa")
                .accept(APPLICATION_JSON_VALUE)
                .header(API_KEY_HEADER_NAME, "testApiKey")
                .header(API_SECRET_HEADER_NAME, "testApiSecret")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void test_deletePost() throws Exception {
        when(stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1")
                        .servletPath("/v1/posts/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"message\":\"Deleted post Id 1\"}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).deleteById("me", 1L);
    }

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

    @Test
    void test_deletePostITunes() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostITunes(null);
        when(queueDefinitionService.isAutoDeploy("me", 1L)).thenReturn(false);
        when(stagingPostService.findById("me", 1L)).thenReturn(updatedPost);
        when(queueDefinitionService.resolveQueueIdent("me", 1L)).thenReturn("1");
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/itunes")
                        .servletPath("/v1/posts/1/itunes")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"id\":1,\"queueIdent\":\"1\",\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postUrl\":\"testUrlHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"category\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-31T04:10:00.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearPostITunes("me", 1L);
        queueDefinitionService.isAutoDeploy("me", 1L);
        verify(stagingPostService).findById("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_deletePostComment() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostComment(null);
        when(queueDefinitionService.isAutoDeploy("me", 1L)).thenReturn(false);
        when(stagingPostService.findById("me", 1L)).thenReturn(updatedPost);
        when(queueDefinitionService.resolveQueueIdent("me", 1L)).thenReturn("1");
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/comment")
                        .servletPath("/v1/posts/1/comment")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                     assertEquals("{\"postDTO\":{\"id\":1,\"queueIdent\":\"1\",\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postITunes\":{\"author\":\"testAuthor\",\"imageUri\":\"http://localhost:666/testImageUri\",\"keywords\":[\"test\"],\"subTitle\":\"testSubtitle\",\"summary\":\"testSummary\",\"duration\":3600000,\"episode\":1,\"episodeType\":\"FULL\",\"order\":1,\"season\":1,\"title\":\"testTitle\",\"isBlock\":false,\"isExplicit\":true,\"isCloseCaptioned\":true},\"postUrl\":\"testUrlHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"category\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-31T04:10:00.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearPostComment("me", 1L);
        queueDefinitionService.isAutoDeploy("me", 1L);
        verify(stagingPostService).findById("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_deletePostRights() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostRights(null);
        when(queueDefinitionService.isAutoDeploy("me", 1L)).thenReturn(false);
        when(stagingPostService.findById("me", 1L)).thenReturn(updatedPost);
        when(queueDefinitionService.resolveQueueIdent("me", 1L)).thenReturn("1");
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/rights")
                        .servletPath("/v1/posts/1/rights")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"id\":1,\"queueIdent\":\"1\",\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postITunes\":{\"author\":\"testAuthor\",\"imageUri\":\"http://localhost:666/testImageUri\",\"keywords\":[\"test\"],\"subTitle\":\"testSubtitle\",\"summary\":\"testSummary\",\"duration\":3600000,\"episode\":1,\"episodeType\":\"FULL\",\"order\":1,\"season\":1,\"title\":\"testTitle\",\"isBlock\":false,\"isExplicit\":true,\"isCloseCaptioned\":true},\"postUrl\":\"testUrlHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"category\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-31T04:10:00.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearPostRights("me", 1L);
        queueDefinitionService.isAutoDeploy("me", 1L);
        verify(stagingPostService).findById("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_deletePostCategories() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setPostCategories(null);
        when(queueDefinitionService.isAutoDeploy("me", 1L)).thenReturn(false);
        when(stagingPostService.findById("me", 1L)).thenReturn(updatedPost);
        when(queueDefinitionService.resolveQueueIdent("me", 1L)).thenReturn("1");
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/categories")
                        .servletPath("/v1/posts/1/categories")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"id\":1,\"queueIdent\":\"1\",\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postITunes\":{\"author\":\"testAuthor\",\"imageUri\":\"http://localhost:666/testImageUri\",\"keywords\":[\"test\"],\"subTitle\":\"testSubtitle\",\"summary\":\"testSummary\",\"duration\":3600000,\"episode\":1,\"episodeType\":\"FULL\",\"order\":1,\"season\":1,\"title\":\"testTitle\",\"isBlock\":false,\"isExplicit\":true,\"isCloseCaptioned\":true},\"postUrl\":\"testUrlHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-31T04:10:00.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearPostCategories("me", 1L);
        queueDefinitionService.isAutoDeploy("me", 1L);
        verify(stagingPostService).findById("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }

    @Test
    void test_deleteExpirationTimestamp() throws Exception {
        StagingPost updatedPost = copyTestStagingPost();
        updatedPost.setExpirationTimestamp(null);
        when(queueDefinitionService.isAutoDeploy("me", 1L)).thenReturn(false);
        when(stagingPostService.findById("me", 1L)).thenReturn(updatedPost);
        when(queueDefinitionService.resolveQueueIdent("me", 1L)).thenReturn("1");
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/posts/1/expiration")
                        .servletPath("/v1/posts/1/expiration")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("{\"postDTO\":{\"id\":1,\"queueIdent\":\"1\",\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postITunes\":{\"author\":\"testAuthor\",\"imageUri\":\"http://localhost:666/testImageUri\",\"keywords\":[\"test\"],\"subTitle\":\"testSubtitle\",\"summary\":\"testSummary\",\"duration\":3600000,\"episode\":1,\"episodeType\":\"FULL\",\"order\":1,\"season\":1,\"title\":\"testTitle\",\"isBlock\":false,\"isExplicit\":true,\"isCloseCaptioned\":true},\"postUrl\":\"testUrlHref\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"category\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false},\"deployed\":false}", responseContent);
                })
                .andExpect(status().isOk());
        verify(stagingPostService).clearExpirationTimestamp("me", 1L);
        queueDefinitionService.isAutoDeploy("me", 1L);
        verify(stagingPostService).findById("me", 1L);
        verify(queueDefinitionService).resolveQueueIdent("me", 1L);
    }
}
