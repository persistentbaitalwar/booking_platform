package com.persistent.booking_platform.web;

import static com.persistent.booking_platform.dto.Responses.BookingResponse;
import static com.persistent.booking_platform.dto.Responses.PaymentWebhookResponse;

import com.persistent.booking_platform.payment.PaymentService;
import com.persistent.booking_platform.security.AccessGuard;
import com.persistent.booking_platform.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final AccessGuard guard;

    public PaymentController(PaymentService paymentService, AccessGuard guard) {
        this.paymentService = paymentService;
        this.guard = guard;
    }

    @Operation(summary = "Payment provider callback (signed with HMAC-SHA256 in the X-Signature header)")
    @PostMapping("/webhook")
    public PaymentWebhookResponse webhook(@RequestBody String body,
                                          @RequestHeader(value = "X-Signature", required = false) String signature) {
        return new PaymentWebhookResponse(paymentService.handleWebhook(body, signature).name());
    }

    @Operation(summary = "Demo payment gateway for local use: pays a booking that is waiting for payment")
    @PostMapping("/simulator/{bookingId}/pay")
    public BookingResponse simulatePayment(@PathVariable Long bookingId, @AuthenticationPrincipal Jwt jwt) {
        guard.requireBookingAccess(CurrentUser.from(jwt), bookingId);
        return paymentService.simulatePayment(bookingId);
    }
}
