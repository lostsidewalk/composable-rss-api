package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.model.request.QueueAuthUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.app.model.request.QueueStatusUpdateRequest;
import com.lostsidewalk.buffy.queue.QueueDefinition;
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
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
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
            null,
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
            null,
            "Test Queue Copyright",
            "en-US",
            null,
            false);
    static {
        TEST_DEPLOYED_QUEUE_DEFINITION.setId(1L);
        TEST_DEPLOYED_QUEUE_DEFINITION.setLastDeployed(YESTERDAY);
    }

    private static final List<QueueDefinition> TEST_DEPLOYED_QUEUE_DEFINITIONS = List.of(TEST_DEPLOYED_QUEUE_DEFINITION);

    private static final QueueConfigRequest TEST_QUEUE_CONFIG_REQUEST = QueueConfigRequest.from(
            "testIdent",
            "testTitle",
            "testDescription",
            "testGenerator",
            null,
            "testCopyright",
            "testLanguage",
            null);

    private static final List<QueueConfigRequest> TEST_QUEUE_CONFIG_REQUESTS = List.of(TEST_QUEUE_CONFIG_REQUEST);

    @Test
    void test_createQueue() throws Exception {
        when(this.queueDefinitionService.createQueue("me", TEST_QUEUE_CONFIG_REQUEST)).thenReturn(1L);
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/queues")
                        .servletPath("/queues")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_QUEUE_CONFIG_REQUESTS))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("[{\"id\":1,\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"exportConfig\":null,\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":null,\"lastDeployed\":null,\"isAuthenticated\":false,\"enabled\":false}]", JsonArray.class),
                            GSON.fromJson(responseContent, JsonArray.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueues() throws Exception {
        when(this.queueDefinitionService.findByUser("me")).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITIONS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("[{\"id\":1,\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"exportConfig\":null,\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":null,\"lastDeployed\":\"1970-01-01T02:46:40.000+00:00\",\"isAuthenticated\":false,\"enabled\":false}]", JsonArray.class),
                            GSON.fromJson(responseContent, JsonArray.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueStatus() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/status")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("ENABLED", String.class),
                            GSON.fromJson(responseContent, String.class)
                    );
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueIdent_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/ident")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("testQueue", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_getQueueTitle_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/title")
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
    void test_getQueueDescription_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/desc")
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
    void test_getQueueGenerator_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/generator")
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
    void test_getQueueTransport_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/transport")
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
    void test_getQueueCopyright_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/copyright")
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
    void test_getQueueLanguage_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/language")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("en-US", responseContent);
                })
                .andExpect(status().isOk());
    }

    private static final SimpleDateFormat ISO_8601_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    @Test
    void test_getQueueDeployedTimestamp_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/deployed")
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
    void test_getQueueAuthRequiremen_textt() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/auth")
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
    void test_getQueueImageSource_text() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/imgsrc")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(TEXT_PLAIN_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("", responseContent);
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_updateQueue() throws Exception {
        when(this.queueDefinitionService.updateQueue("me", 1L, TEST_QUEUE_CONFIG_REQUEST)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                .put("/queues/1")
                .servletPath("/queues/1")
                .contentType(APPLICATION_JSON)
                .content(GSON.toJson(TEST_QUEUE_CONFIG_REQUEST))
                .header(API_KEY_HEADER_NAME, "testApiKey")
                .header(API_SECRET_HEADER_NAME, "testApiSecret")
                ).andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"id\":1,\"ident\":\"testQueue\",\"title\":\"Test Queue Title\",\"description\":\"Test Queue Description\",\"generator\":\"Test Queue Feed Generator\",\"transportIdent\":\"Test Queue Transport Identifier\",\"exportConfig\":null,\"copyright\":\"Test Queue Copyright\",\"language\":\"en-US\",\"queueImgSrc\":null,\"lastDeployed\":\"1970-01-01T02:46:40.000+00:00\",\"isAuthenticated\":false,\"enabled\":false}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    private static final QueueStatusUpdateRequest TEST_QUEUE_STATUS_UPDATE_REQUEST = new QueueStatusUpdateRequest();
    static {
        TEST_QUEUE_STATUS_UPDATE_REQUEST.setNewStatus("DISABLED");
    }

    @Test
    void test_updateQueueStatus() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/status")
                        .servletPath("/queues/1/status")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_QUEUE_STATUS_UPDATE_REQUEST))
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
        verify(this.queueDefinitionService).updateQueueStatus("me", 1L, TEST_QUEUE_STATUS_UPDATE_REQUEST);
    }

    @Test
    void test_updateQueueIdent() throws Exception {
//        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        when(this.queueDefinitionService.updateQueueIdent("me", 1L, "newIdent")).thenReturn("newIdent");
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/ident")
                        .servletPath("/queues/1/ident")
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
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/title")
                        .servletPath("/queues/1/title")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newTitle")
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
        verify(this.queueDefinitionService).updateQueueTitle("me", 1L, "newTitle");
    }

    @Test
    void test_updateQueueDescription() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/desc")
                        .servletPath("/queues/1/desc")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newDescription")
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
        verify(this.queueDefinitionService).updateQueueDescription("me", 1L, "newDescription");
    }

    @Test
    void test_updateQueueGenerator() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/generator")
                        .servletPath("/queues/1/generator")
                        .content("newGenerator")
                        .contentType(TEXT_PLAIN_VALUE)
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
        verify(this.queueDefinitionService).updateQueueGenerator("me", 1L, "newGenerator");
    }

    @Test
    void test_updateQueueCopyright() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/copyright")
                        .servletPath("/queues/1/copyright")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newCopyright")
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
        verify(this.queueDefinitionService).updateQueueCopyright("me", 1L, "newCopyright");
    }

    @Test
    void test_updateQueueLanguage() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/language")
                        .servletPath("/queues/1/language")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("newLanguage")
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
        verify(this.queueDefinitionService).updateQueueLanguage("me", 1L, "newLanguage");
    }

    private static final QueueAuthUpdateRequest TEST_QUEUE_AUTH_UPDATE_REQUEST = new QueueAuthUpdateRequest();
    static {
        TEST_QUEUE_AUTH_UPDATE_REQUEST.setIsRequired(true);
    }

    @Test
    void test_updateQueueAuthRequirement() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/auth")
                        .servletPath("/queues/1/auth")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_QUEUE_AUTH_UPDATE_REQUEST))
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
        verify(this.queueDefinitionService).updateQueueAuthenticationRequirement("me", 1L, true);
    }

    @Test
    void test_updateQueueImageSource() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_DEPLOYED_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/imgsrc")
                        .servletPath("/queues/1/imgsrc")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testImageSource")
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
        verify(this.queueDefinitionService).updateQueueImageSource("me", 1L, "testImageSource");
    }

    @Test
    void test_deleteQueue() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/queues/1")
                        .servletPath("/queues/1")
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
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/queues/1/title")
                        .servletPath("/queues/1/title")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"message\":\"Deleted title from queue Id 1\"}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueTitle("me", 1L);
    }

    @Test
    void test_deleteQueueDescription() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/queues/1/desc")
                        .servletPath("/queues/1/desc")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"message\":\"Deleted description from queue Id 1\"}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueDescription("me", 1L);
    }

    @Test
    void test_deleteQueueGenerator() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/queues/1/generator")
                        .servletPath("/queues/1/generator")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"message\":\"Deleted generator from queue Id 1\"}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueGenerator("me", 1L);
    }

    @Test
    void test_deleteQueueCopyright() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/queues/1/copyright")
                        .servletPath("/queues/1/copyright")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"message\":\"Deleted copyright from queue Id 1\"}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueCopyright("me", 1L);
    }

    @Test
    void test_deleteQueueImageSource() throws Exception {
        when(this.queueDefinitionService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/queues/1/imgsrc")
                        .servletPath("/queues/1/imgsrc")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"message\":\"Deleted queue image from queue Id 1\"}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(queueDefinitionService).clearQueueImageSource("me", 1L);
    }
}
