package com.lostsidewalk.buffy.app.v1.credentials;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.BaseWebControllerTest;
import com.lostsidewalk.buffy.queue.QueueCredential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_KEY_HEADER_NAME;
import static com.lostsidewalk.buffy.app.auth.AuthTokenFilter.API_SECRET_HEADER_NAME;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = QueueCredentialsController.class)
public class QueueCredentialControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(authService.findUserByApiKey("testApiKey")).thenReturn(TEST_API_USER);
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY_OBJ);
        when(apiUserService.loadUserByUsername("me")).thenReturn(TEST_API_USER_DETAILS);
    }

    private static final Gson GSON = new Gson();

    private static final QueueCredential TEST_QUEUE_CREDENTIAL = new QueueCredential();
    static {
        TEST_QUEUE_CREDENTIAL.setId(1L);
        TEST_QUEUE_CREDENTIAL.setUsername("me");
        TEST_QUEUE_CREDENTIAL.setQueueId(1L);
        TEST_QUEUE_CREDENTIAL.setBasicUsername("testUsername");
        TEST_QUEUE_CREDENTIAL.setBasicPassword("testPassword");
    }

    @Test
    public void test_addQueueCredential() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueCredentialsService.addCredential("me", 1L, "testUsername", "testPassword")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/queues/1/credentials")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_QUEUE_CREDENTIAL))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Added credential Id 1 to queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isCreated());
        verify(this.queueCredentialsService).addCredential("me", 1L, "testUsername", "testPassword");
    }

    private static final List<QueueCredential> TEST_QUEUE_CREDENTIALS = singletonList(TEST_QUEUE_CREDENTIAL);

    @Test
    public void test_getQueueCredentials() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueCredentialsService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_CREDENTIALS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/credentials")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[{\"id\":1,\"queueId\":1,\"username\":\"me\",\"basicUsername\":\"testUsername\",\"basicPassword\":\"testPassword\"}]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_getQueueCredential() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        when(this.queueCredentialsService.findById("me", 1L, 1L)).thenReturn(TEST_QUEUE_CREDENTIAL);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/v1/queues/1/credentials/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"id\":1,\"queueId\":1,\"username\":\"me\",\"basicUsername\":\"testUsername\",\"basicPassword\":\"testPassword\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_updateQueueCredential_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/credentials/1")
                        .servletPath("/v1/queues/1/credentials/1")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testPassword")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated password for credential Id 1 on queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).updatePassword("me", 1L, 1L, "testPassword");
    }

    @Test
    public void test_updateQueueCredential_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/credentials/1")
                        .servletPath("/v1/queues/1/credentials/1")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson("testPassword"))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated password for credential Id 1 on queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).updatePassword("me", 1L, 1L, GSON.toJson("testPassword"));
    }

    @Test
    public void test_patchQueueCredential_text() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/v1/queues/1/credentials/1")
                        .servletPath("/v1/queues/1/credentials/1")
                        .contentType(TEXT_PLAIN_VALUE)
                        .content("testPassword")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated password for credential Id 1 on queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).updatePassword("me", 1L, 1L, "testPassword");
    }

    @Test
    public void test_patchQueueCredential_json() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/v1/queues/1/credentials/1")
                        .servletPath("/v1/queues/1/credentials/1")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson("testPassword"))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated password for credential Id 1 on queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).updatePassword("me", 1L, 1L, GSON.toJson("testPassword"));
    }

    @Test
    public void test_deleteQueueCredentials() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/credentials")
                        .servletPath("/v1/queues/1/credentials")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted all credentials from queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).deleteQueueCredentials("me", 1L);
    }

    @Test
    public void test_deleteQueueCredential() throws Exception {
        when(this.queueDefinitionService.resolveQueueId("me", "1")).thenReturn(1L);
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/v1/queues/1/credentials/1")
                        .servletPath("/v1/queues/1/credentials/1")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted credential with Id 1 from queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).deleteQueueCredential("me", 1L, 1L);
    }
}