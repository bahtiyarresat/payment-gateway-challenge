package com.checkout.payment.gateway.controller;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "acquiring-bank.base-url=http://bank.test" // avoid hitting real docker simulator during tests
})
@AutoConfigureMockMvc
class PaymentGatewayProcessingTest {

  @Autowired
  private MockMvc mvc;
  @Autowired private RestTemplate restTemplate;

  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    server = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void whenValidPaymentAndBankAuthorizes_thenReturnAuthorizedAndStoreMaskedDetails() throws Exception {
    server.expect(requestTo("http://bank.test/payments"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("{" +
            "\"card_number\":\"2222405343248877\"," +
            "\"expiry_date\":\"04/2030\"," +
            "\"currency\":\"GBP\"," +
            "\"amount\":100," +
            "\"cvv\":\"123\"" +
            "}"))
        .andRespond(withSuccess(
            "{\"authorized\":true,\"authorization_code\":\"abc\"}",
            MediaType.APPLICATION_JSON));

    mvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{" +
                "\"card_number\":\"2222405343248877\"," +
                "\"expiry_month\":4," +
                "\"expiry_year\":2030," +
                "\"currency\":\"GBP\"," +
                "\"amount\":100," +
                "\"cvv\":\"123\"" +
                "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.expiry_month").value(4))
        .andExpect(jsonPath("$.expiry_year").value(2030))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.id").exists());

    server.verify();
  }

  @Test
  void whenInvalidPayment_thenRejectedWithoutCallingBank() throws Exception {
    mvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{" +
                "\"card_number\":\"12\"," +
                "\"expiry_month\":13," +
                "\"expiry_year\":2000," +
                "\"currency\":\"AAA\"," +
                "\"amount\":0," +
                "\"cvv\":\"x\"" +
                "}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.message").value("Rejected"))
        .andExpect(jsonPath("$.errors").isArray());
  }

  @Test
  void whenBankUnavailable_then503Returned() throws Exception {
    server.expect(requestTo("http://bank.test/payments"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    mvc.perform(post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{" +
                "\"card_number\":\"2222405343248870\"," +
                "\"expiry_month\":4," +
                "\"expiry_year\":2030," +
                "\"currency\":\"GBP\"," +
                "\"amount\":100," +
                "\"cvv\":\"123\"" +
                "}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("BANK_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("Acquiring bank unavailable"));

    server.verify();
  }
}

