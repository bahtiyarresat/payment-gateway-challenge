package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.bank.AcquiringBankClient;
import com.checkout.payment.gateway.bank.model.BankPaymentRequest;
import com.checkout.payment.gateway.bank.model.BankPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient acquiringBankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      AcquiringBankClient acquiringBankClient) {
    this.paymentsRepository = paymentsRepository;
    this.acquiringBankClient = acquiringBankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new EventProcessingException("Invalid ID"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.debug("Processing payment request {}", paymentRequest);

    BankPaymentRequest bankRequest = new BankPaymentRequest(
        paymentRequest.getCardNumber(),
        String.format("%02d/%d", paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear()),
        paymentRequest.getCurrency(),
        paymentRequest.getAmount(),
        paymentRequest.getCvv());

    BankPaymentResponse bankResponse = acquiringBankClient.submitPayment(bankRequest);
    boolean authorized = bankResponse != null && bankResponse.isAuthorized();

    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(authorized ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);
    response.setCardNumberLastFour(paymentRequest.lastFourDigits());
    response.setExpiryMonth(paymentRequest.getExpiryMonth());
    response.setExpiryYear(paymentRequest.getExpiryYear());
    response.setCurrency(paymentRequest.getCurrency());
    response.setAmount(paymentRequest.getAmount());

    paymentsRepository.add(response);
    return response;
  }
}
