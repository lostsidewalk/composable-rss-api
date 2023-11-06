package com.lostsidewalk.buffy.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.lostsidewalk.buffy.app.model.TokenType.APP_AUTH;
import static org.mockito.Mockito.when;


@Slf4j
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExportController.class)
class ExportControllerTest extends BaseWebControllerTest {

    @BeforeEach
    void test_setup() throws Exception {
        when(tokenService.instanceFor(APP_AUTH, "testToken")).thenReturn(TEST_JWT_UTIL);
        when(authService.requireAuthClaim("me")).thenReturn("testAuthClaim");
        when(userService.loadUserByUsername("me")).thenReturn(TEST_USER_DETAILS);
    }

    @Test
    void test_exportFeed() {}
}
