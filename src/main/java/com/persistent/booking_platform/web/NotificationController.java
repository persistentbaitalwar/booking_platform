package com.persistent.booking_platform.web;

import static com.persistent.booking_platform.dto.Responses.NotificationResponse;

import com.persistent.booking_platform.security.AccessGuard;
import com.persistent.booking_platform.security.CurrentUser;
import com.persistent.booking_platform.service.TicketProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Notifications")
public class NotificationController {
    private final TicketProcessingService processingService;
    private final AccessGuard guard;

    public NotificationController(TicketProcessingService processingService, AccessGuard guard) {
        this.processingService = processingService;
        this.guard = guard;
    }

    @Operation(summary = "Latest messages created by the ticket event pipeline for a customer")
    @GetMapping("/customers/{customerId}/notifications")
    public List<NotificationResponse> notifications(@PathVariable Long customerId, @AuthenticationPrincipal Jwt jwt) {
        guard.requireCustomer(CurrentUser.from(jwt), customerId);
        return processingService.notificationsFor(customerId);
    }
}
