package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(EventProcessingException ex) {
    LOG.warn("Payment not found", ex);
    return new ResponseEntity<>(
        new ErrorResponse("PAYMENT_NOT_FOUND", "Payment not found"),
        HttpStatus.NOT_FOUND
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.toList());

    // Add global errors (e.g. cross-field constraints like expiry validation)
    errors.addAll(ex.getBindingResult().getGlobalErrors().stream()
        .map(err -> err.getObjectName() + ": " + err.getDefaultMessage())
        .collect(Collectors.toList()));

    return new ResponseEntity<>(
        new ErrorResponse("VALIDATION_ERROR", "Rejected", errors),
        HttpStatus.BAD_REQUEST
    );
  }

  @ExceptionHandler(AcquiringBankUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleBankUnavailable(AcquiringBankUnavailableException ex) {
    LOG.warn("Acquiring bank unavailable", ex);
    return new ResponseEntity<>(
        new ErrorResponse("BANK_UNAVAILABLE", "Acquiring bank unavailable"),
        HttpStatus.SERVICE_UNAVAILABLE
    );
  }
}
