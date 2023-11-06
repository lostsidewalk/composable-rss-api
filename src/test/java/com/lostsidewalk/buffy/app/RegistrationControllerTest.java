package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.model.request.RegistrationRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static com.lostsidewalk.buffy.app.model.TokenType.VERIFICATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RegistrationController.class)
class RegistrationControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    @BeforeEach
    final void test_setup() throws Exception {
        when(tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    @Test
    final void test_register() throws Exception {
        RegistrationRequest testRegistrationRequest = new RegistrationRequest("testUsername", "me@localhost", "testPassword");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/register")
                        .servletPath("/register")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(testRegistrationRequest))
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"username\":\"testUsername\",\"password\":\"testPassword\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());

    }

    @Test
    final void test_verify() throws Exception {
        when(tokenService.instanceFor(VERIFICATION, "testToken")).thenReturn(TEST_JWT_UTIL);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/verify/testToken")
                        .servletPath("/verify/testToken")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().is3xxRedirection());
        verify(userService).markAsVerified("me");
        verify(authService).finalizeVerificationClaim("me");
    }

    @Test
    final void test_deregister() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/deregister")
                        .servletPath("/deregister")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }
}
