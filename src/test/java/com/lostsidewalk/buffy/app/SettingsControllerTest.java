package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.FrameworkConfig;
import com.lostsidewalk.buffy.app.model.request.SettingsUpdateRequest;
import com.lostsidewalk.buffy.app.model.request.UpdateSubscriptionRequest;
import com.lostsidewalk.buffy.app.model.response.SettingsResponse;
import com.lostsidewalk.buffy.app.model.response.StripeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static com.lostsidewalk.buffy.app.model.request.SubscriptionStatus.ACTIVE;
import static com.lostsidewalk.buffy.app.model.request.SubscriptionStatus.CANCELED;
import static com.lostsidewalk.buffy.auth.AuthProvider.LOCAL;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = SettingsController.class)
public class SettingsControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    private static final SettingsResponse TEST_SETTINGS_RESPONSE = SettingsResponse.from(
            "me",
            "testEmailAddress",
            LOCAL,
            "testAuthProviderProfileImgUrl",
            "testAuthProviderUsername",
            new FrameworkConfig()
    );

    private static final Gson GSON = new Gson();

    @Test
    void test_getSettings() throws Exception {
        when(this.settingsService.getFrameworkConfig("me")).thenReturn(TEST_SETTINGS_RESPONSE);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/settings")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"username\":\"me\",\"emailAddress\":\"testEmailAddress\",\"authProvider\":\"LOCAL\",\"authProviderProfileImgUrl\":\"testAuthProviderProfileImgUrl\",\"authProviderUsername\":\"testAuthProviderUsername\",\"frameworkConfig\":{\"userId\":null,\"notifications\":{},\"display\":{}},\"subscription\":null}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
    }

    private static final FrameworkConfig TEST_FRAMEWORK_CONFIG = new FrameworkConfig();
    static {
        TEST_FRAMEWORK_CONFIG.setUserId(1L);
        //
        TEST_FRAMEWORK_CONFIG.setDisplay(emptyMap());
        TEST_FRAMEWORK_CONFIG.setNotifications(emptyMap());
    }

    private static final SettingsUpdateRequest TEST_SETTINGS_UPDATE_REQUEST = new SettingsUpdateRequest();
    static {
        TEST_SETTINGS_UPDATE_REQUEST.setEmailAddress("me@localhost");
        TEST_SETTINGS_UPDATE_REQUEST.setFrameworkConfig(TEST_FRAMEWORK_CONFIG);
    }

    @Test
    void test_updateSettings() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/settings")
                        .header("Authorization", "Bearer testToken")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_SETTINGS_UPDATE_REQUEST)))
                .andExpect(status().isOk());
        verify(this.settingsService).updateFrameworkConfig("me", TEST_SETTINGS_UPDATE_REQUEST);
    }

    private static final StripeResponse TEST_STRIPE_RESPONSE = StripeResponse.from("testSessionId", "testSessionUrl");

    @Test
    void test_initCheckout() throws Exception {
        when(this.stripeOrderService.createCheckoutSession("me")).thenReturn(TEST_STRIPE_RESPONSE);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/order")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"sessionId\":\"testSessionId\",\"sessionUrl\":\"testSessionUrl\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(this.stripeOrderService).createCheckoutSession("me");
    }

    private static final UpdateSubscriptionRequest TEST_CANCEL_SUBSCRIPTION_REQUEST = new UpdateSubscriptionRequest();
    static {
        TEST_CANCEL_SUBSCRIPTION_REQUEST.setSubscriptionStatus(CANCELED);
    }

    @Test
    void test_updateSubscription_cancel() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/subscriptions")
                        .header("Authorization", "Bearer testToken")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_CANCEL_SUBSCRIPTION_REQUEST)))
                .andExpect(status().isOk());
        verify(this.stripeOrderService).cancelSubscription("me");
    }

    private static final UpdateSubscriptionRequest TEST_RESUME_SUBSCRIPTION_REQUEST = new UpdateSubscriptionRequest();
    static {
        TEST_RESUME_SUBSCRIPTION_REQUEST.setSubscriptionStatus(ACTIVE);
    }

    @Test
    void test_updateSubscription_resume() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .put("/subscriptions")
                        .header("Authorization", "Bearer testToken")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(TEST_RESUME_SUBSCRIPTION_REQUEST)))
                .andExpect(status().isOk());
        verify(this.stripeOrderService).resumeSubscription("me");
    }}
