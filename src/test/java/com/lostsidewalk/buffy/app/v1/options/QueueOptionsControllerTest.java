package com.lostsidewalk.buffy.app.v1.options;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.app.model.v1.Atom10Config;
import com.lostsidewalk.buffy.app.model.v1.RSS20Config;
import com.lostsidewalk.buffy.app.model.v1.request.ExportConfigRequest;
import com.lostsidewalk.buffy.app.model.v1.response.ExportConfigDTO;
import com.lostsidewalk.buffy.app.v1.queue.QueueController;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.Serializable;
import java.util.Date;

import static com.google.common.collect.ImmutableMap.of;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static com.lostsidewalk.buffy.publisher.Publisher.PubResult.from;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = QueueController.class)
public class QueueOptionsControllerTest extends BaseWebControllerTest {

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

    @Test
    public void test_getExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/options")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_getAtomExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/options/atomConfig")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_getRssExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/options/rssConfig")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_updateExportOptions() throws Exception {
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
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/options")
                        .servletPath("/v1/queues/1/options")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_EXPORT_CONFIG))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateExportConfig(eq("me"), eq(1L), any(ExportConfigRequest.class), eq(false));
    }

    @Test
    public void test_updateAtomExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                ExportConfigDTO.from(TEST_ATOM_CONFIG, null),
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/options/atomConfig")
                        .servletPath("/v1/queues/1/options/atomConfig")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_ATOM_CONFIG))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateAtomExportConfig(eq("me"), eq(1L), any(Atom10Config.class), eq(false));
    }

    @Test
    public void test_updateRssExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                ExportConfigDTO.from(null, TEST_RSS_CONFIG),
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/options/rssConfig")
                        .servletPath("/v1/queues/1/options/rssConfig")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_RSS_CONFIG))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateRssExportConfig(eq("me"), eq(1L), any(RSS20Config.class), eq(false));
    }

    @Test
    public void test_patchExportOptions() throws Exception {
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
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/options")
                        .servletPath("/v1/queues/1/options")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_EXPORT_CONFIG))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"},\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateExportConfig(eq("me"), eq(1L), any(ExportConfigRequest.class), eq(true));
    }

    @Test
    public void test_patchAtomExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                ExportConfigDTO.from(TEST_ATOM_CONFIG, null),
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/options/atomConfig")
                        .servletPath("/v1/queues/1/options/atomConfig")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_ATOM_CONFIG))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateAtomExportConfig(eq("me"), eq(1L), eq(TEST_ATOM_CONFIG), eq(true));
    }

    @Test
    public void test_patchRssExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                ExportConfigDTO.from(null, TEST_RSS_CONFIG),
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/options/rssConfig")
                        .servletPath("/v1/queues/1/options/rssConfig")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_RSS_CONFIG))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).updateRssExportConfig(eq("me"), eq(1L), any(RSS20Config.class), eq(true));
    }

    private static final Date NOW = new Date(15_000_000L);

    @Test
    public void test_deleteExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
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
                false));
        when(this.postPublisher.publishFeed("me", 1L)).thenReturn(of("RSS_20", from("testUrl", emptyList(), NOW)));
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/options")
                        .servletPath("/v1/queues/1/options")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{\"RSS_20\":{\"timestamp\":\"1970-01-01T04:10:00.000+00:00\",\"publisherIdent\":\"RSS_20\",\"url\":\"testUrl\"}}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).clearExportConfig("me", 1L);
    }

    @Test
    public void test_deleteAtomExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                ExportConfigDTO.from(null, TEST_RSS_CONFIG),
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        when(this.postPublisher.publishFeed("me", 1L)).thenReturn(of("RSS_20", from("testUrl", emptyList(), NOW)));
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/options/atomConfig")
                        .servletPath("/v1/queues/1/options/atomConfig")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"rssConfig\":{\"managingEditor\":\"managingEditor\",\"webMaster\":\"webMaster\",\"categoryValue\":\"categoryValue\",\"categoryDomain\":\"categoryDomain\",\"docs\":\"docs\",\"cloudDomain\":\"cloudDomain\",\"cloudProtocol\":\"cloudProtocol\",\"cloudRegisterProcedure\":\"cloudRegisterProcedure\",\"cloudPort\":80,\"ttl\":60,\"rating\":\"rating\",\"textInputTitle\":\"textInputTitle\",\"textInputDescription\":\"textInputDescription\",\"textInputName\":\"textInputName\",\"textInputLink\":\"textInputLink\",\"skipHours\":\"skipHours\",\"skipDays\":\"skipDays\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{\"RSS_20\":{\"timestamp\":\"1970-01-01T04:10:00.000+00:00\",\"publisherIdent\":\"RSS_20\",\"url\":\"testUrl\"}}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).clearAtomExportConfig("me", 1L);
    }

    @Test
    public void test_deleteRssExportOptions() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(QueueDefinition.from(
                "testQueue",
                "Test Queue Title",
                "Test Queue Description",
                "Test Queue Feed Generator",
                "Test Queue Transport Identifier",
                "me",
                ExportConfigDTO.from(TEST_ATOM_CONFIG, null),
                "Test Queue Copyright",
                "en-US",
                "testQueueImageSource",
                false));
        when(this.postPublisher.publishFeed("me", 1L)).thenReturn(of("RSS_20", from("testUrl", emptyList(), NOW)));
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/options/rssConfig")
                        .servletPath("/v1/queues/1/options/rssConfig")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"queueDTO\":{\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"options\":{\"atomConfig\":{\"authorName\":\"testAuthorName\",\"authorEmail\":\"testAuthorEmail\",\"authorUri\":\"testAuthorUri\",\"contributorName\":\"testContributorName\",\"contributorEmail\":\"testContributorEmail\",\"contributorUri\":\"testContributorUri\",\"categoryTerm\":\"testCategoryTerm\",\"categoryLabel\":\"testCategoryLabel\",\"categoryScheme\":\"testCategoryScheme\"}},\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":\"testQueueImageSource\",\"isAuthenticated\":false},\"deployResponses\":{\"RSS_20\":{\"timestamp\":\"1970-01-01T04:10:00.000+00:00\",\"publisherIdent\":\"RSS_20\",\"url\":\"testUrl\"}}}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueDefinitionService).clearRssExportConfig("me", 1L);
    }
}
