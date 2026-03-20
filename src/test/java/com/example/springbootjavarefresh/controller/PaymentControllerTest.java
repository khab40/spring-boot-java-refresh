package com.example.springbootjavarefresh.controller;

import com.example.springbootjavarefresh.entity.PaymentTransaction;
import com.example.springbootjavarefresh.entity.PaymentTransactionStatus;
import com.example.springbootjavarefresh.security.JwtAuthenticationFilter;
import com.example.springbootjavarefresh.service.PaymentService;
import com.example.springbootjavarefresh.service.PaymentWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private PaymentWebhookService paymentWebhookService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void shouldCreateCheckoutRequest() throws Exception {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(99L);
        transaction.setStatus(PaymentTransactionStatus.PENDING);
        when(paymentService.initiateCheckout(any())).thenReturn(transaction);

        mockMvc.perform(post("/api/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productId": 2,
                                  "successUrl": "https://example.com/success",
                                  "cancelUrl": "https://example.com/cancel"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(99L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldGetPaymentStatus() throws Exception {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setId(99L);
        transaction.setStatus(PaymentTransactionStatus.CHECKOUT_CREATED);
        transaction.setCheckoutUrl("https://stripe.test/checkout");
        when(paymentService.getTransactionById(99L)).thenReturn(Optional.of(transaction));
        when(paymentService.refreshTransactionStatusFromStripe(transaction)).thenReturn(transaction);

        mockMvc.perform(get("/api/payments/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKOUT_CREATED"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://stripe.test/checkout"));
    }

    @Test
    void shouldReturnNotFoundForUnknownPayment() throws Exception {
        when(paymentService.getTransactionById(101L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payments/101"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAcceptWebhookRequest() throws Exception {
        doNothing().when(paymentWebhookService).handleWebhook(any(), eq("t=1,v1=test"));

        mockMvc.perform(post("/api/payments/webhook")
                        .header("Stripe-Signature", "t=1,v1=test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "checkout.session.completed",
                                  "data": {
                                    "object": {
                                      "id": "cs_test_123",
                                      "clientReferenceId": "99"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().string("Webhook processed"));
    }
}
