package com.checkout.payment.gateway.bank;

import com.checkout.payment.gateway.bank.model.BankPaymentRequest;
import com.checkout.payment.gateway.bank.model.BankPaymentResponse;
import com.checkout.payment.gateway.exception.AcquiringBankUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class AcquiringBankClient {

  private static final Logger LOG = LoggerFactory.getLogger(AcquiringBankClient.class);

  private final RestTemplate restTemplate;
  private final String baseUrl;

  public AcquiringBankClient(RestTemplate restTemplate,
      @Value("${acquiring-bank.base-url:http://localhost:8080}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl;
  }

  public BankPaymentResponse submitPayment(BankPaymentRequest request) {
    try {
      ResponseEntity<BankPaymentResponse> response = restTemplate.postForEntity(
          baseUrl + "/payments", request, BankPaymentResponse.class);
      return response.getBody();
    } catch (HttpStatusCodeException ex) {
      // The simulator uses 503 to represent bank issues.
      if (ex.getStatusCode().value() == 503) {
        throw new AcquiringBankUnavailableException("Acquiring bank unavailable", ex);
      }
      LOG.warn("Bank responded with status {} and body {}", ex.getStatusCode(), ex.getResponseBodyAsString());
      throw ex;
    } catch (ResourceAccessException ex) {
      throw new AcquiringBankUnavailableException("Could not reach acquiring bank", ex);
    }
  }
}

