package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.lostsidewalk.buffy.app.model.AppToken;
import com.lostsidewalk.buffy.app.model.request.NewPasswordRequest;
import com.lostsidewalk.buffy.app.model.request.PasswordResetRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.model.TokenType.PW_AUTH;
import static com.lostsidewalk.buffy.app.model.TokenType.PW_RESET;
import static com.lostsidewalk.buffy.auth.AuthProvider.LOCAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = PasswordResetController.class)
class PasswordResetControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new Gson();

    private static final AppToken TEST_PW_RESET_TOKEN = new AppToken("testPwResetToken", 60);

    @Test
    void testInitPasswordReset() throws Exception {
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(authService.generateAuthToken("me")).thenReturn(new AppToken("testToken", 60));
        PasswordResetRequest testPwResetRequest = new PasswordResetRequest("me", "me@localhost");
        when(authService.initPasswordReset(testPwResetRequest)).thenReturn(TEST_PW_RESET_TOKEN);
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/pw_reset")
                        .servletPath("/pw_reset")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(testPwResetRequest))
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        verify(authService).requireAuthProvider("me", LOCAL);
        verify(mailService).sendPasswordResetEmail("me", TEST_PW_RESET_TOKEN);
    }

    @Test
    void testContinuePasswordReset() throws Exception {
        when(tokenService.instanceFor(PW_RESET, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/pw_reset/testToken")
                        .servletPath("/pw_reset/testToken")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().is3xxRedirection());
        verify(authService).continuePasswordReset(eq("me"), any(HttpServletResponse.class));
    }

    private static final NewPasswordRequest TEST_NEW_PASSWORD_REQUEST = new NewPasswordRequest();
    static {
        TEST_NEW_PASSWORD_REQUEST.setNewPassword("testPassword");
        TEST_NEW_PASSWORD_REQUEST.setNewPasswordConfirmed("testPassword");
    }

    @Test
    void testFinalizePasswordReset() throws Exception {
        when(authService.getTokenCookieFromRequest(eq(PW_AUTH), any())).thenReturn("testToken");
        when(tokenService.instanceFor(PW_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(authService.requirePwResetAuthClaim("me")).thenReturn("testAuthClaim");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        mockMvc.perform(MockMvcRequestBuilders
                .put("/pw_update")
                .servletPath("/pw_update")
                .contentType(APPLICATION_JSON_VALUE)
                .content(GSON.toJson(TEST_NEW_PASSWORD_REQUEST))
                .header("Authorization", "Bearer testToken"))
                .andExpect(status().isOk());
        verify(authService).finalizePwResetAuthClaim("me");
        verify(authService).updatePassword("me", "testPassword");
    }
}
