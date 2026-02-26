package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.ValidExpiryDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@ValidExpiryDate
public class PostPaymentRequest implements Serializable {

  @NotBlank(message = "Card number is required")
  @Size(min = 14, max = 19, message = "Card number must be between 14 and 19 digits")
  @Pattern(regexp = "^[0-9]+$", message = "Card number must contain only numeric characters")
  @JsonProperty("card_number")
  private String cardNumber;

  @NotNull(message = "Expiry month is required")
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @NotNull(message = "Expiry year is required")
  @Min(value = 2000, message = "Expiry year must be 2000 or later")
  @JsonProperty("expiry_year")
  private Integer expiryYear;

  @NotBlank(message = "Currency is required")
  @Size(min = 3, max = 3, message = "Currency must be 3 characters")
  @Pattern(regexp = "^(GBP|USD|EUR)$", message = "Currency must be one of: GBP, USD, EUR")
  private String currency;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be a positive integer")
  private Integer amount;

  @NotBlank(message = "CVV is required")
  @Size(min = 3, max = 4, message = "CVV must be 3 or 4 characters")
  @Pattern(regexp = "^[0-9]+$", message = "CVV must contain only numeric characters")
  private String cvv;

  public String getCardNumber() { return cardNumber; }
  public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

  public Integer getExpiryMonth() { return expiryMonth; }
  public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }

  public Integer getExpiryYear() { return expiryYear; }
  public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }

  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }

  public Integer getAmount() { return amount; }
  public void setAmount(Integer amount) { this.amount = amount; }

  public String getCvv() { return cvv; }
  public void setCvv(String cvv) { this.cvv = cvv; }

  public String lastFourDigits() {
    if (cardNumber == null || cardNumber.length() < 4) return cardNumber;
    return cardNumber.substring(cardNumber.length() - 4);
  }

  @Override
  public String toString() {
    // Never log full card number
    String masked = cardNumber != null
        ? "*".repeat(cardNumber.length() - 4) + lastFourDigits()
        : null;
    return "PostPaymentRequest{"
        + "cardNumber='" + masked + '\''
        + ", expiryMonth=" + expiryMonth
        + ", expiryYear=" + expiryYear
        + ", currency='" + currency + '\''
        + ", amount=" + amount
        + '}';
  }
}
