# Payment Gateway — Java Implementation

A Spring Boot payment gateway that processes merchant payments via an acquiring bank simulator.

## Requirements

- JDK 17
- Docker (for the bank simulator)

## Running the Application

**1. Start the bank simulator:**
```bash
docker-compose up -d
```

**2. Run the application:**
```bash
./gradlew bootRun
```

The application starts on port **8090**. The bank simulator runs on port **8080**.

**3. Run the tests:**
```bash
./gradlew test
```

---

## API

### POST /payments — Process a payment

**Request:**
```json
{
  "card_number": "2222405343248877",
  "expiry_month": 4,
  "expiry_year": 2030,
  "currency": "GBP",
  "amount": 100,
  "cvv": "123"
}
```

**Responses:**

| Status | Meaning |
|--------|---------|
| `200 OK` | Payment processed — status is `Authorized` or `Declined` |
| `400 Bad Request` | Validation failed — payment **Rejected**, bank not called |
| `503 Service Unavailable` | Acquiring bank unreachable |

**200 Response example:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "Authorized",
  "card_number_last_four": "8877",
  "expiry_month": 4,
  "expiry_year": 2030,
  "currency": "GBP",
  "amount": 100
}
```

**400 Response example:**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Rejected",
  "errors": [
    "card_number: Card number must be between 14 and 19 digits",
    "currency: Currency must be one of: GBP, USD, EUR"
  ]
}
```

---

### GET /payments/{id} — Retrieve a payment

Returns a previously processed payment by its ID. The full card number is never stored — only the last 4 digits are returned.

**Response:** Same shape as the POST response above.

**200 Response example:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "Authorized",
  "card_number_last_four": "8877",
  "expiry_month": 4,
  "expiry_year": 2030,
  "currency": "GBP",
  "amount": 100
}
```

**404 Response:**
```json
{
  "code": "PAYMENT_NOT_FOUND",
  "message": "Payment not found"
}
```

---

## Validation Rules

| Field | Rules |
|-------|-------|
| `card_number` | Required, 14–19 digits, numeric only |
| `expiry_month` | Required, 1–12 |
| `expiry_year` | Required, ≥ 2000. Combined with month must be current month or future |
| `currency` | Required, exactly 3 chars, one of: `GBP`, `USD`, `EUR` |
| `amount` | Required, positive integer (minor currency unit, e.g. pence) |
| `cvv` | Required, 3–4 digits, numeric only |

---

## Design Decisions

**503 handling:** When the acquiring bank returns 503 or is unreachable, the gateway surfaces a `503 Service Unavailable` to the merchant rather than mapping it to `Declined`. This distinguishes a genuine bank refusal from a processing outage.

**Card masking:** The full PAN and CVV are never stored. Only the last 4 digits of the card number are persisted and returned.

**Expiry validity:** A card is considered valid through the end of its expiry month (i.e. a card expiring 03/2026 is valid during March 2026).

**Storage:** In-memory `ConcurrentHashMap` — sufficient per the exercise requirements and thread-safe for concurrent requests.

**Supported currencies:** GBP, USD, EUR.

---

## Testing

Three layers of tests:

**Unit tests** (`PaymentGatewayServiceTest`) — business logic in isolation using Mockito.
Covers: authorized payment, declined payment, bank unavailable (exception propagates, nothing saved),
payment not found, correct expiry date formatting sent to bank.

**Controller tests** (`PaymentGatewayControllerTest`) — slice test verifying GET endpoint
returns correct payment shape and 404 for unknown IDs.

**Integration tests** (`PaymentGatewayProcessingTest`) — full Spring context with
`MockRestServiceServer` intercepting the bank HTTP call. Verifies the exact request payload
sent to the bank (card number, `MM/YYYY` expiry format, currency, amount, cvv),
and all response scenarios: authorized, declined, rejected (validation), bank unavailable.

---

## API Documentation (Swagger)

Available at: **http://localhost:8090/swagger-ui/index.html**

---

## Project Structure

```
src/main/java/com/checkout/payment/gateway/
├── controller/         # REST endpoints
├── service/            # Business logic + bank client
├── bank/               # Acquiring bank HTTP client + DTOs
├── model/              # Request/response models
├── validation/         # Custom @ValidExpiryDate constraint
├── exception/          # Exception handlers
├── repository/         # In-memory payment store
└── enums/              # PaymentStatus enum
```