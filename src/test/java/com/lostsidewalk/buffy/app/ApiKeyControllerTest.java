package com.lostsidewalk.buffy.app;

import com.lostsidewalk.buffy.auth.ApiKey;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ApiKeyController.class)
class ApiKeyControllerTest extends BaseWebControllerTest {

    @BeforeEach
    @SuppressWarnings("NestedMethodCall")
    final void test_setup() throws Exception {
        when(tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    private static final Long TEST_USER_ID = 1L;

    private static final String TEST_KEY_VALUE = "keyValue";

    private static final String TEST_SECRET_VALUE = "secretValue";

    private static final ApiKey TEST_API_KEY = ApiKey.from(TEST_USER_ID, TEST_KEY_VALUE, TEST_SECRET_VALUE);

    @Test
    final void testInitApiKeyRecovery() throws Exception {
        when(authService.requireApiKey("me")).thenReturn(TEST_API_KEY);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/send_key")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals("Ok", responseContent);
                })
                .andExpect(status().isOk());
        verify(mailService).sendApiKeyRecoveryEmail(eq("me"), eq(TEST_API_KEY));
    }
}
