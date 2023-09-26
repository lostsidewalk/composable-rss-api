package com.lostsidewalk.buffy.app.v1.queue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import com.lostsidewalk.buffy.app.model.v1.request.*;
import com.lostsidewalk.buffy.app.model.v1.response.ExportConfigDTO;
import com.lostsidewalk.buffy.post.*;
import com.lostsidewalk.buffy.publisher.Publisher.PubResult;
import com.lostsidewalk.buffy.queue.QueueDefinition;
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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static com.lostsidewalk.buffy.post.StagingPost.PostPubStatus.PUB_PENDING;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = QueueController.class)
public class QueueControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Atom10Config TEST_ATOM_CONFIG = Atom10Config.from(
            "testAuthorName", "testAuthorEmail", "testAuthorUri",
            "testContributorName", "testContributorEmail", "testContributorUri",
            "testCategoryTerm", "testCategoryLabel", "testCategoryScheme"
    );

    private static final RSS20Config TEST_RSS_CONFIG = RSS20Config.from(
            "managingEditor", "webMaster", "categoryValue",
            "categoryDomain", "docs", "cloudDomain", "cloudProtocol",
            "cloudRegisterProcedure", 80, 60, "rating", "textInputTitle",
            "textInputDescription", "textInputName", "textInputLink",
            "skipHours", "skipDays");

    private static final Serializable TEST_EXPORT_CONFIG = ExportConfigDTO.from(
            TEST_ATOM_CONFIG,
            TEST_RSS_CONFIG
    );

    private static final QueueDefinition TEST_QUEUE_DEFINITION = QueueDefinition.from(
            "testQueue",
            "Test Queue Title",
            "Test Queue Description",
            "Test Queue Feed Generator",
            "Test Queue Transport Identifier",
            "me",
            TEST_EXPORT_CONFIG,
            "Test Queue Copyright",
            "en-US",
            "testQueueImageSource",
            false);
    static {
        TEST_QUEUE_DEFINITION.setId(1L);
    }

    private static final Date YESTERDAY = new Date(10_000_000L);

    private static final QueueDefinition TEST_DEPLOYED_QUEUE_DEFINITION = QueueDefinition.from(
            "testQueue",
            "Test Queue Title",
            "Test Queue Description",
            "Test Queue Feed Generator",
            "Test Queue Transport Identifier",
            "me",
            TEST_EXPORT_CONFIG,
            "Test Queue Copyright",
            "en-US",
            null,
            false);
    static {
        TEST_DEPLOYED_QUEUE_DEFINITION.setId(1L);
        TEST_DEPLOYED_QUEUE_DEFINITION.setLastDeployed(YESTERDAY);
    }

    private static final List<QueueDefinition> TEST_DEPLOYED_QUEUE_DEFINITIONS = List.of(TEST_DEPLOYED_QUEUE_DEFINITION);

    //
    //
    //

    private static final ExportConfigRequest TEST_EXPORT_CONFIG_REQUEST = ExportConfigRequest.from(
            TEST_ATOM_CONFIG, TEST_RSS_CONFIG
    );

    private static final QueueConfigRequest TEST_QUEUE_CONFIG_REQUEST = QueueConfigRequest.from(
            "testIdent",
            "testTitle",
            "testDescription",
            "testGenerator",
            TEST_EXPORT_CONFIG_REQUEST,
            "testCopyright",
            "testLanguage",
            null);

    private static final Date NOW = new Date(15_000_000L);

    private static final PubResult TEST_PUB_RESULT = PubResult.from("testPubUrl", emptyList(), NOW);

    private static final Map<String, PubResult> TEST_PUB_RESULTS = Map.of("RSS_20", TEST_PUB_RESULT);

    @Test
    void test_createQueues() throws Exception {
        when(this.queueDefinitionService.createQueue("me", TEST_QUEUE_CONFIG_REQUEST)).thenReturn(1L);
        when(this.postPublisher.publishFeed("me", 1L)).thenReturn(TEST_PUB_RESULTS);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/queues")
                        .servletPath("/v1/queues")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_QUEUE_CONFIG_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{\"RSS_20\":{\"timestamp\":\"1970-01-01T04:10:00.000+00:00\",\"publisherIdent\":\"RSS_20\",\"url\":\"testPubUrl\"}}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class)
                    );
                })
                .andExpect(status().isCreated());
    }

    @Test
    void test_getQueues() throws Exception {
        when(this.queueDefinitionService.findByUser("me")).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITIONS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("[{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"lastDeployed\":\"1970-01-01T02:46:40.000+00:00\",\"isAuthenticated\":false}]", JsonArray.class),
                            GSON.fromJson(responseContent, JsonArray.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueById() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"lastDeployed\":\"1970-01-01T02:46:40.000+00:00\",\"isAuthenticated\":false}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueTitle_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/title")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Title", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueTitle_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/title")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Title", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueDescription_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/description")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Description", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueDescription_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/description")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Description", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueGenerator_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/generator")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Feed Generator", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueGenerator_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/generator")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Feed Generator", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueTransport_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/transport")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Transport Identifier", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueTransport_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/transport")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Transport Identifier", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueCopyright_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/copyright")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE)
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Copyright", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueCopyright_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/copyright")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE)
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Test Queue Copyright", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueLanguage_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/language")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("en-US", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueLanguage_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/language")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("en-US", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    private static final SimpleDateFormat ISO_8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Test
    void test_getQueueDeployedTimestamp_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/deployed")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertTrue(isNotBlank(responseContent));
                    Date deployedTimestamp = null;
                    try {
                        deployedTimestamp = ISO_8601_TIMESTAMP_FORMAT.parse(responseContent);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                    assertEquals(deployedTimestamp, YESTERDAY);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueDeployedTimestamp_json_deployed() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/deployed")
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
    void test_getQueueDeployedTimestamp_json_nonDeployed() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/deployed")
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
    void test_getQueueAuthRequirement_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/auth")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("false", Boolean.class),
                            GSON.fromJson(responseContent, Boolean.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueAuthRequirement_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/auth")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("false", Boolean.class),
                            GSON.fromJson(responseContent, Boolean.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueImageSource_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/imgsrc")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testQueueImageSource", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueImageSource_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/imgsrc")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testQueueImageSource", GSON.fromJson(responseContent, String.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateQueue() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.updateQueue("me", 1L, TEST_QUEUE_CONFIG_REQUEST, false)).thenReturn(QueueDefinition.from(
                "testIdent",
                "testTitle",
                "testDescription",
                "testGenerator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "testCopyright",
                "testLanguage",
                null,
                false));
        mockMvc.perform(MockMvcRequestBuilders
                .put("/v1/queues/1")
                .servletPath("/v1/queues/1")
                .contentType(APPLICATION_JSON_VALUE)
                .content(GSON.toJson(TEST_QUEUE_CONFIG_REQUEST))
                .header(API_KEY_HEADER_NAME, "testApiKey")
                .header(API_SECRET_HEADER_NAME, "testApiSecret")
                ).andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testIdent\",\"title\":\"testTitle\",\"description\":\"testDescription\",\"generator\":\"testGenerator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"testCopyright\",\"language\":\"testLanguage\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchQueue() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.updateQueue("me", 1L, TEST_QUEUE_CONFIG_REQUEST, true)).thenReturn(QueueDefinition.from(
                "testIdent",
                "testTitle",
                "testDescription",
                "testGenerator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "testCopyright",
                "testLanguage",
                null,
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1")
                        .servletPath("/v1/queues/1")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_QUEUE_CONFIG_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                ).andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testIdent\",\"title\":\"testTitle\",\"description\":\"testDescription\",\"generator\":\"testGenerator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"testCopyright\",\"language\":\"testLanguage\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateQueueIdent() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.updateQueueIdent("me", 1L, "newIdent")).thenReturn("newIdent");
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/ident")
                        .servletPath("/v1/queues/1/ident")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newIdent")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"message\":\"Successfully updated queue Id 1\"}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateQueueTitle() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/title")
                        .servletPath("/v1/queues/1/title")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newTitle")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"lastDeployed\":\"1970-01-01T02:46:40.000+00:00\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueTitle("me", 1L, "newTitle");
    }

    @Test
    void test_updateQueueDescription() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "newDescription",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/description")
                        .servletPath("/v1/queues/1/description")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newDescription")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"newDescription\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueDescription("me", 1L, "newDescription");
    }

    @Test
    void test_updateQueueGenerator() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "newGenerator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/generator")
                        .servletPath("/v1/queues/1/generator")
                        .content("newGenerator")
                        .contentType(TEXT_PLAIN_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"newGenerator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueGenerator("me", 1L, "newGenerator");
    }

    @Test
    void test_updateQueueCopyright() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "newCopyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/copyright")
                        .servletPath("/v1/queues/1/copyright")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newCopyright")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"newCopyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueCopyright("me", 1L, "newCopyright");
    }

    @Test
    void test_updateQueueLanguage() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "newLanguage",
                "newImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/language")
                        .servletPath("/v1/queues/1/language")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newLanguage")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"newLanguage\",\"queueImgSrc\":\"newImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueLanguage("me", 1L, "newLanguage");
    }

    private static final QueueAuthUpdateRequest TEST_QUEUE_AUTH_UPDATE_REQUEST = new QueueAuthUpdateRequest();
    static {
        TEST_QUEUE_AUTH_UPDATE_REQUEST.setIsRequired(true);
    }

    @Test
    void test_updateQueueAuthRequirement() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                true));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/auth")
                        .servletPath("/v1/queues/1/auth")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_QUEUE_AUTH_UPDATE_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":true},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueAuthenticationRequirement("me", 1L, true);
    }

    @Test
    void test_updateQueueImageSource() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "newImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/imgsrc")
                        .servletPath("/v1/queues/1/imgsrc")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testImageSource")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"newImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueImageSource("me", 1L, "testImageSource");
    }

    @Test
    void test_patchQueueIdent() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.updateQueueIdent("me", 1L, "newIdent")).thenReturn("newIdent");
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/ident")
                        .servletPath("/v1/queues/1/ident")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newIdent")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"message\":\"Successfully updated queue Id 1\"}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_patchQueueTitle() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "newTitle",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/title")
                        .servletPath("/v1/queues/1/title")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newTitle")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"newTitle\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueTitle("me", 1L, "newTitle");
    }

    @Test
    void test_patchQueueDescription() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "newDescription",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/description")
                        .servletPath("/v1/queues/1/description")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newDescription")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"newDescription\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueDescription("me", 1L, "newDescription");
    }

    @Test
    void test_patchQueueGenerator() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "newGenerator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/generator")
                        .servletPath("/v1/queues/1/generator")
                        .content("newGenerator")
                        .contentType(TEXT_PLAIN_VALUE)
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"newGenerator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueGenerator("me", 1L, "newGenerator");
    }

    @Test
    void test_patchQueueCopyright() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "newCopyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/copyright")
                        .servletPath("/v1/queues/1/copyright")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newCopyright")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"newCopyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueCopyright("me", 1L, "newCopyright");
    }

    @Test
    void test_patchQueueLanguage() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "newLanguage",
                "newImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/language")
                        .servletPath("/v1/queues/1/language")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newLanguage")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"newLanguage\",\"queueImgSrc\":\"newImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueLanguage("me", 1L, "newLanguage");
    }

    @Test
    void test_patchQueueAuthRequirement() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                true));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/auth")
                        .servletPath("/v1/queues/1/auth")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_QUEUE_AUTH_UPDATE_REQUEST))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":true},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueAuthenticationRequirement("me", 1L, true);
    }

    @Test
    void test_patchQueueImageSource() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "newImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/imgsrc")
                        .servletPath("/v1/queues/1/imgsrc")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newImageSource")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"newImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateQueueImageSource("me", 1L, "newImageSource");
    }

    @Test
    void test_deletePosts() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/posts")
                        .servletPath("/v1/queues/1/posts")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted posts from queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stagingPostService).deleteByQueueId("me", 1L);
    }

    @Test
    void test_deleteQueue() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1")
                        .servletPath("/v1/queues/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"message\":\"Deleted queue Id 1\"}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).deleteById("me", 1L);
    }

    @Test
    void test_deleteQueueTitle() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                null,
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/title")
                        .servletPath("/v1/queues/1/title")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueTitle("me", 1L);
    }

    @Test
    void test_deleteQueueDescription() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                null,
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/description")
                        .servletPath("/v1/queues/1/description")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueDescription("me", 1L);
    }

    @Test
    void test_deleteQueueGenerator() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                null,
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/generator")
                        .servletPath("/v1/queues/1/generator")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueGenerator("me", 1L);
    }

    @Test
    void test_deleteQueueCopyright() throws Exception {
//        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                null,
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/copyright")
                        .servletPath("/v1/queues/1/copyright")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueCopyright("me", 1L);
    }

    @Test
    void test_deleteQueueImageSource() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                TEST_EXPORT_CONFIG,
                "Test Queue Copyright",
                "en-US",
                null,
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/imgsrc")
                        .servletPath("/v1/queues/1/imgsrc")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueImageSource("me", 1L);
    }

    //
    //
    //

    protected static final ContentObject TEST_POST_TITLE = new ContentObject();
    static {
        TEST_POST_TITLE.setIdent("2");
        TEST_POST_TITLE.setType("text");
        TEST_POST_TITLE.setValue("testPostTitle");
    }

    protected static final ContentObject TEST_POST_DESCRIPTION = new ContentObject();
    static {
        TEST_POST_DESCRIPTION.setIdent("2");
        TEST_POST_DESCRIPTION.setType("text");
        TEST_POST_DESCRIPTION.setValue("testPostDescription");
    }

    protected static final ContentObject TEST_POST_CONTENT = new ContentObject();
    static {
        TEST_POST_CONTENT.setIdent("2");
        TEST_POST_CONTENT.setValue("testPostContent");
    }

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
        TEST_POST_URL.setIdent("2");
        TEST_POST_URL.setTitle("testUrlTitle");
        TEST_POST_URL.setType("testUrlType");
        TEST_POST_URL.setHref("testUrlHref");
        TEST_POST_URL.setHreflang("testUrlHreflang");
        TEST_POST_URL.setRel("testUrlRel");
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
        TEST_POST_ENCLOSURE.setIdent("2");
        TEST_POST_ENCLOSURE.setUrl("testEnclosureUrl");
        TEST_POST_ENCLOSURE.setType("testEnclosureType");
        TEST_POST_ENCLOSURE.setLength(4821L);
    }

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
            List.of("category"),
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
            List.of("category"),
            null, // publish timestamp
            null, // expiration timestamp
            List.of(TEST_POST_ENCLOSURE),
            null // last updated timestamp
    );
    static {
        TEST_NON_DEPLOYED_STAGING_POST.setId(1L);
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
        TEST_POST_CONFIG_REQUEST.setExpirationTimestamp(THIRTY_DAYS_FROM_NOW);
        TEST_POST_CONFIG_REQUEST.setEnclosures(TEST_ENCLOSURES_CONFIG_REQUEST);
    }

    private static final List<PostConfigRequest> TEST_POST_CONFIG_REQUESTS = List.of(TEST_POST_CONFIG_REQUEST);
    @Test
    void test_createPost() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.stagingPostService.createPost("me", 1L, TEST_POST_CONFIG_REQUEST)).thenReturn(1L);
        when(this.stagingPostService.findById("me", 1L)).thenReturn(TEST_STAGING_POST);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/queues/1/posts")
                        .servletPath("/v1/queues/1/posts")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_POST_CONFIG_REQUESTS))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("[{\"id\":1,\"queueIdent\":\"1\",\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postITunes\":{\"keywords\":[\"test\"],\"explicit\":false,\"block\":false,\"closeCaptioned\":false},\"postUrl\":\"testPostUrl\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"category\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-01T13:53:20.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false}]", JsonArray.class),
                            GSON.fromJson(responseContent, JsonArray.class)
                    );
                })
                .andExpect(status().isCreated());
    }

    @Test
    void test_getPosts() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.stagingPostService.getStagingPosts("me", List.of(1L))).thenReturn(TEST_STAGING_POSTS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/posts")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("[{\"id\":1,\"queueIdent\":\"1\",\"postTitle\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostTitle\"},\"postDesc\":{\"ident\":\"2\",\"type\":\"text\",\"value\":\"testPostDescription\"},\"postContents\":[{\"ident\":\"2\",\"value\":\"testPostContent\"}],\"postITunes\":{\"keywords\":[\"test\"],\"explicit\":false,\"block\":false,\"closeCaptioned\":false},\"postUrl\":\"testPostUrl\",\"postUrls\":[{\"ident\":\"2\",\"title\":\"testUrlTitle\",\"type\":\"testUrlType\",\"href\":\"testUrlHref\",\"hreflang\":\"testUrlHreflang\",\"rel\":\"testUrlRel\"}],\"postComment\":\"testPostComment\",\"postRights\":\"testPostRights\",\"contributors\":[{\"name\":\"testContributorName\",\"email\":\"testContributorEmail\",\"uri\":\"testContributorUri\"}],\"authors\":[{\"name\":\"testAuthorName\",\"email\":\"testAuthorEmail\",\"uri\":\"testAuthorUri\"}],\"postCategories\":[\"category\"],\"publishTimestamp\":\"1970-01-01T02:46:40.000+00:00\",\"expirationTimestamp\":\"1970-01-01T13:53:20.000+00:00\",\"enclosures\":[{\"ident\":\"2\",\"url\":\"testEnclosureUrl\",\"type\":\"testEnclosureType\",\"length\":4821}],\"lastUpdatedTimestamp\":\"1970-01-01T04:10:00.000+00:00\",\"published\":false}]", JsonArray.class),
                            GSON.fromJson(responseContent, JsonArray.class)
                    );
                })
                .andExpect(status().isOk());
    }
}
