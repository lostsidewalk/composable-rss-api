package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.model.request.QueueConfigRequest;
import com.lostsidewalk.buffy.queue.QueueDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = QueueDefinitionController.class)
public class QueueDefinitionControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new Gson();

    private static final QueueDefinition TEST_FEED_DEFINITION = QueueDefinition.from(
            "testFeed",
            "Test Feed Title",
            "Test Feed Description",
            "Test Feed Generator",
            "Test Feed Transport Identifier",
            "me",
            null,
            "Test Feed Copyright",
            "en-US",
            null,
            false);
    static {
        TEST_FEED_DEFINITION.setId(1L);
    }

    private static final QueueConfigRequest TEST_FEED_CONFIG_REQUEST = QueueConfigRequest.from(
            "testIdent",
            "testTitle",
            "testDescription",
            "testGenerator",
            null,
            "testCopyright",
            "testLanguage",
            null);

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    @Test
    void test_getFeeds() {

    }

    @Test
    void test_toggleFeed() {

    }

    @Test
    void test_updateFeed() throws Exception {
        when(this.queueDefinitionDao.findByQueueId("me", 1L)).thenReturn(TEST_FEED_DEFINITION);
        mockMvc.perform(MockMvcRequestBuilders
                .put("/queues/1")
                .servletPath("/queues/1")
                .contentType(APPLICATION_JSON)
                .content(GSON.toJson(TEST_FEED_CONFIG_REQUEST))
                .header("Authorization", "Bearer testToken"))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(
                            GSON.fromJson("{\"queueDefinition\":{\"id\":1,\"ident\":\"testFeed\",\"title\":\"Test Feed Title\",\"description\":\"Test Feed Description\",\"generator\":\"Test Feed Generator\",\"transportIdent\":\"Test Feed Transport Identifier\",\"username\":\"me\",\"queueStatus\":\"ENABLED\",\"copyright\":\"Test Feed Copyright\",\"language\":\"en-US\",\"isAuthenticated\":false},\"queueImgSrc\":null}", JsonObject.class),
                            GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    @Test
    void test_createFeed() {

    }

    @Test
    void test_validateOpmlFiles() {

    }

    @Test
    void test_deleteFeedById() {

    }

    @Test
    void test_getQueries() {

    }
}
