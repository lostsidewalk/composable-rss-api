package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
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

    private static final QueueCredential TEST_QUEUE_CREDENTIAL = QueueCredential.from("me", 1L, "testUsername", "testPassword");

    @Test
    public void test_addQueueCredential() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/queues/1/credentials")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_QUEUE_CREDENTIAL))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Added credential to queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).addCredential("me", 1L, "testUsername", "testPassword");
    }

    private static final List<QueueCredential> TEST_QUEUE_CREDENTIALS = singletonList(TEST_QUEUE_CREDENTIAL);

    @Test
    public void test_getQueueCredentials() throws Exception {
        when(this.queueCredentialsService.findByQueueId("me", 1L)).thenReturn(TEST_QUEUE_CREDENTIALS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/queues/1/credentials")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("[{\"queueId\":1,\"username\":\"me\",\"basicUsername\":\"testUsername\",\"basicPassword\":\"testPassword\"}]", JsonArray.class), GSON.fromJson(responseContent, JsonArray.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    public void test_updatePassword() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/queues/1/credentials/customer")
                        .servletPath("/queues/1/credentials/customer")
                        .content("testPassword")
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Updated credential on queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).updatePassword("me", 1L, "customer", "testPassword");
    }

    @Test
    public void test_deleteQueueCredential() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/queues/1/credentials/customer")
                        .servletPath("/queues/1/credentials/customer")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(TEST_QUEUE_CREDENTIAL))
                        .header(API_KEY_HEADER_NAME, "testApiKey")
                        .header(API_SECRET_HEADER_NAME, "testApiSecret")
                )
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"message\":\"Deleted credential from queue Id 1\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.queueCredentialsService).deleteQueueCredential("me", 1L, "customer");
    }
}
