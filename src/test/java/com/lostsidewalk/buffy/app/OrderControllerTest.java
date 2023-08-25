package com.lostsidewalk.buffy.app;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.stripe.model.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.lostsidewalk.buffy.app.OrderController.STRIPE_SIGNATURE_HEADER;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = OrderController.class)
public class OrderControllerTest extends BaseWebControllerTest {

    private static final Gson GSON = new Gson();

    @Test
    public void testStripeCallback() throws Exception {
        Event TEST_STRIPE_ORDER_EVENT = new Event();
        String testSigHeader = "testSigHeader";
        String testEventPayload = "{}";
        when(this.stripeOrderService.constructEvent(testSigHeader, testEventPayload)).thenReturn(TEST_STRIPE_ORDER_EVENT);
        JsonObject testStripePayload = new JsonObject();
        mockMvc.perform(MockMvcRequestBuilders
                .post("/stripe")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(GSON.toJson(testStripePayload))
                        .header(STRIPE_SIGNATURE_HEADER, testSigHeader)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
        verify(this.stripeOrderService).constructEvent(eq(testSigHeader), eq(testEventPayload));
    }
}
