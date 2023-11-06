package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lostsidewalk.buffy.app.auth.CookieBuilder;
import com.lostsidewalk.buffy.app.model.AppToken;
import com.lostsidewalk.buffy.app.model.request.LoginRequest;
import com.lostsidewalk.buffy.app.token.TokenService.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.auth.AuthProvider.LOCAL;
import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH_REFRESH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = AuthenticationController.class)
class AuthenticationControllerTest extends BaseWebControllerTest {

    @Test
    void testCurrentUser() throws Exception {
        when(authService.getTokenCookieFromRequest(eq(APP_AUTH_REFRESH), any())).thenReturn("testCookieValue");
        JwtUtil mockJwtUtil = mock(JwtUtil.class);
        when(tokenService.instanceFor(APP_AUTH_REFRESH, "testCookieValue")).thenReturn(mockJwtUtil);
        when(mockJwtUtil.extractUsername()).thenReturn("me");
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(mockJwtUtil.extractValidationClaim()).thenReturn("b4223bd3427db93956acaadf9e425dd259bfb11dac44234604c819dbbf75e180");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        when(authService.generateAuthToken("me")).thenReturn(new AppToken("testToken", 60));
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/currentuser")
                        .servletPath("/currentuser")
                        .cookie(new CookieBuilder("composable-rss-app_auth-token","testTokenValue").build())
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"authToken\":\"testToken\",\"username\":\"me\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(mockJwtUtil).requireNonExpired();
        verify(authService).addTokenCookieToResponse(eq(APP_AUTH_REFRESH), eq("me"), eq("testAuthClaim"), any(HttpServletResponse.class));
    }

    private static final Gson GSON = new Gson();

    @Test
    void testAuthenticate() throws Exception {
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(authService.generateAuthToken("me")).thenReturn(new AppToken("testToken", 60));
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        LoginRequest testLoginRequest = new LoginRequest("me", "testPassword");
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/authenticate")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(testLoginRequest))
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    assertEquals(GSON.fromJson("{\"authToken\":\"testToken\",\"username\":\"me\"}", JsonObject.class), GSON.fromJson(responseContent, JsonObject.class));
                })
                .andExpect(status().isOk());
        verify(authService).requireAuthProvider("me", LOCAL);
        verify(authService).addTokenCookieToResponse(eq(APP_AUTH_REFRESH), eq("me"), eq("testAuthClaim"), any());
    }

    @Test
    void testDeauthenticate() throws Exception {
        JwtUtil mockJwtUtil = mock(JwtUtil.class);
        when(tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(mockJwtUtil);
        when(mockJwtUtil.extractUsername()).thenReturn("me");
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(mockJwtUtil.extractValidationClaim()).thenReturn("b4223bd3427db93956acaadf9e425dd259bfb11dac44234604c819dbbf75e180");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/deauthenticate")
                        .header("Authorization", "Bearer testToken")
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        verify(mockJwtUtil).requireNonExpired();
        verify(authService).requireAuthClaim("me");
    }
}
