package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.model.request.RegistrationRequest;
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
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RegistrationController.class)
public class RegistrationControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
            .create();

    @BeforeEach
    void test_setup() throws Exception {
        when(this.tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(this.authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(this.userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    @Test
    public void test_register() throws Exception {
        RegistrationRequest testRegistrationRequest = new RegistrationRequest("testUsername", "me@localhost", "testPassword");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/register")
                        .servletPath("/register")
                        .contentType(APPLICATION_JSON)
                        .content(GSON.toJson(testRegistrationRequest))
                        .accept(APPLICATION_JSON))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"username\":\"testUsername\",\"password\":\"testPassword\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());

    }

    @Test
    public void test_verify() throws Exception {
        when(this.tokenService.instanceFor(VERIFICATION, "testToken")).thenReturn(TEST_JWT_UTIL);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/verify/testToken")
                        .servletPath("/verify/testToken")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(status().is3xxRedirection());
        verify(this.userService).markAsVerified("me");
        verify(this.authService).finalizeVerificationClaim("me");
    }

    @Test
    public void test_deregister() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/deregister")
                        .servletPath("/deregister")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
