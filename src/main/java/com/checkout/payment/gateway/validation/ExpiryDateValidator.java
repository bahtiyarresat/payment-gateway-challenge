package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

public class ExpiryDateValidator implements
    ConstraintValidator<ValidExpiryDate, PostPaymentRequest> {

  @Override
  public boolean isValid(PostPaymentRequest request, ConstraintValidatorContext context) {
    if (request == null) return true;
    Integer month = request.getExpiryMonth();
    Integer year = request.getExpiryYear();
    if (month == null || year == null) return true; // handled by @NotNull

    try {
      YearMonth expiry = YearMonth.of(year, month);
      YearMonth now = YearMonth.now();
      // Card is valid through the end of the expiry month
      boolean valid = expiry.isAfter(now) || expiry.equals(now);
      if (!valid) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "Expiry date must be in the future")
            .addConstraintViolation();
      }
      return valid;
    } catch (Exception e) {
      return false;
    }
  }
}