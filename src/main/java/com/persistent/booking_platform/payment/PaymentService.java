package com.persistent.booking_platform.payment;

import static com.persistent.booking_platform.dto.Responses.BookingResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persistent.booking_platform.exception.ApiException;
import com.persistent.booking_platform.service.BookingService;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Provider-neutral payment callbacks. Any gateway adapter (Razorpay, Stripe, PayU) translates its own webhook into
 * this small JSON contract and signs the body with HMAC-SHA256 using the shared webhook secret:
 * {"eventId":"evt_1","bookingId":42,"paymentReference":"pay_9","status":"SUCCEEDED"} (status SUCCEEDED or FAILED).
 */
@Service
public class PaymentService {
    public record WebhookEvent(String eventId, Long bookingId, String paymentReference, String status) {
    }

    private final BookingService bookingService;
    private final PaymentProperties properties;
    private final ObjectMapper objectMapper;

    public PaymentService(BookingService bookingService, PaymentProperties properties, ObjectMapper objectMapper) {
        this.bookingService = bookingService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public PaymentOutcome handleWebhook(String rawBody, String signature) {
        if (signature == null || !MessageDigest.isEqual(sign(rawBody).getBytes(StandardCharsets.UTF_8),
                signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE", "The webhook signature is not valid");
        }
        WebhookEvent event;
        try {
            event = objectMapper.readValue(rawBody, WebhookEvent.class);
        } catch (JsonProcessingException ex) {
            throw ApiException.badRequest("MALFORMED_WEBHOOK", "The webhook body is not valid JSON");
        }
        if (event.bookingId() == null || event.status() == null) {
            throw ApiException.badRequest("MALFORMED_WEBHOOK", "The webhook needs a bookingId and a status");
        }
        switch (event.status()) {
            case "SUCCEEDED":
                return bookingService.confirmPayment(event.bookingId(), event.paymentReference());
            case "FAILED":
                return bookingService.failPayment(event.bookingId());
            default:
                throw ApiException.badRequest("MALFORMED_WEBHOOK", "Unknown payment status " + event.status());
        }
    }

    /** Local stand-in for the provider's checkout page: pays the booking as the gateway would report it. */
    public BookingResponse simulatePayment(Long bookingId) {
        if (!properties.isHoldMode() || !properties.simulatorEnabled()) {
            throw ApiException.notFound("SIMULATOR_DISABLED", "The demo payment gateway is not enabled");
        }
        bookingService.confirmPayment(bookingId, "SIM-PAY-" + UUID.randomUUID());
        return bookingService.getBooking(bookingId);
    }

    public String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 is not available", ex);
        }
    }
}
