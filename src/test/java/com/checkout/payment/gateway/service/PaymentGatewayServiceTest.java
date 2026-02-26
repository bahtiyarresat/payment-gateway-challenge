package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.bank.AcquiringBankClient;
import com.checkout.payment.gateway.bank.model.BankPaymentRequest;
import com.checkout.payment.gateway.bank.model.BankPaymentResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquiringBankUnavailableException;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private AcquiringBankClient acquiringBankClient;

  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  @Captor
  private ArgumentCaptor<BankPaymentRequest> bankRequestCaptor;

  @Captor
  private ArgumentCaptor<PostPaymentResponse> savedPaymentCaptor;

  @Test
  void processPayment_whenBankAuthorizes_returnsAuthorizedAndStoresMaskedDetails() {
    PostPaymentRequest req = request("2222405343248877", 4, 2030, "GBP", 1050, "123");

    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("auth-code");
    when(acquiringBankClient.submitPayment(any())).thenReturn(bankResponse);

    PostPaymentResponse res = paymentGatewayService.processPayment(req);

    // status + masking
    assertThat(res.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(res.getCardNumberLastFour()).isEqualTo("8877");
    assertThat(res.getExpiryMonth()).isEqualTo(4);
    assertThat(res.getExpiryYear()).isEqualTo(2030);
    assertThat(res.getCurrency()).isEqualTo("GBP");
    assertThat(res.getAmount()).isEqualTo(1050);
    assertThat(res.getId()).isNotNull();

    // bank request mapping (incl. expiry formatting)
    verify(acquiringBankClient).submitPayment(bankRequestCaptor.capture());
    BankPaymentRequest bankReq = bankRequestCaptor.getValue();
    assertThat(bankReq.getCardNumber()).isEqualTo("2222405343248877");
    assertThat(bankReq.getExpiryDate()).isEqualTo("04/2030");
    assertThat(bankReq.getCurrency()).isEqualTo("GBP");
    assertThat(bankReq.getAmount()).isEqualTo(1050);
    assertThat(bankReq.getCvv()).isEqualTo("123");

    // stored payment matches response (gateway stores last4 only via response object)
    verify(paymentsRepository).add(savedPaymentCaptor.capture());
    PostPaymentResponse saved = savedPaymentCaptor.getValue();
    assertThat(saved.getCardNumberLastFour()).isEqualTo("8877");
    assertThat(saved.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
  }

  @Test
  void processPayment_whenBankDeclines_returnsDeclinedAndStoresPayment() {
    PostPaymentRequest req = request("2222405343248878", 12, 2031, "USD", 1, "1234");

    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(false);
    bankResponse.setAuthorizationCode("whatever");
    when(acquiringBankClient.submitPayment(any())).thenReturn(bankResponse);

    PostPaymentResponse res = paymentGatewayService.processPayment(req);

    assertThat(res.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(res.getCardNumberLastFour()).isEqualTo("8878");
    verify(paymentsRepository).add(any(PostPaymentResponse.class));
  }

  @Test
  void processPayment_whenBankUnavailable_propagatesException() {
    PostPaymentRequest req = request("2222405343248870", 1, 2030, "EUR", 100, "999");
    when(acquiringBankClient.submitPayment(any())).thenThrow(
        new AcquiringBankUnavailableException("down"));

    assertThatThrownBy(() -> paymentGatewayService.processPayment(req))
        .isInstanceOf(AcquiringBankUnavailableException.class);

    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void getPaymentById_whenFound_returnsPayment() {
    UUID id = UUID.randomUUID();
    PostPaymentResponse stored = new PostPaymentResponse();
    stored.setId(id);
    stored.setStatus(PaymentStatus.AUTHORIZED);
    stored.setCardNumberLastFour("1234");
    stored.setExpiryMonth(1);
    stored.setExpiryYear(2030);
    stored.setCurrency("GBP");
    stored.setAmount(100);

    when(paymentsRepository.get(id)).thenReturn(Optional.of(stored));

    PostPaymentResponse res = paymentGatewayService.getPaymentById(id);

    assertThat(res).isSameAs(stored);
    verify(paymentsRepository).get(id);
  }

  @Test
  void getPaymentById_whenMissing_throwsEventProcessingException() {
    UUID id = UUID.randomUUID();
    when(paymentsRepository.get(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentGatewayService.getPaymentById(id))
        .isInstanceOf(EventProcessingException.class);

    verify(paymentsRepository).get(id);
  }

  private static PostPaymentRequest request(String cardNumber, int expiryMonth, int expiryYear,
      String currency, int amount, String cvv) {
    PostPaymentRequest req = new PostPaymentRequest();
    req.setCardNumber(cardNumber);
    req.setExpiryMonth(expiryMonth);
    req.setExpiryYear(expiryYear);
    req.setCurrency(currency);
    req.setAmount(amount);
    req.setCvv(cvv);
    return req;
  }
}
