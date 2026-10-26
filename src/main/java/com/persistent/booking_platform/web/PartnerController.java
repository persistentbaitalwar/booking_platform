package com.persistent.booking_platform.web;

import static com.persistent.booking_platform.dto.Requests.*;
import static com.persistent.booking_platform.dto.Responses.*;

import com.persistent.booking_platform.security.AccessGuard;
import com.persistent.booking_platform.security.CurrentUser;
import com.persistent.booking_platform.service.PartnerService;
import com.persistent.booking_platform.service.ShowImportService;
import com.persistent.booking_platform.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** B2B API for theatre partners. A partner can only manage the theatres they onboarded (admins can manage all). */
@RestController
@RequestMapping("/api/v1/partner")
@Tag(name = "Partner")
public class PartnerController {
    private final PartnerService partnerService;
    private final ShowImportService showImportService;
    private final TicketService ticketService;
    private final AccessGuard guard;

    public PartnerController(PartnerService partnerService, ShowImportService showImportService,
                             TicketService ticketService, AccessGuard guard) {
        this.partnerService = partnerService;
        this.showImportService = showImportService;
        this.ticketService = ticketService;
        this.guard = guard;
    }

    @Operation(summary = "Import many shows from a theatre system in one call")
    @PostMapping("/shows/import")
    public ImportResponse importShows(@Valid @RequestBody ShowImportRequest request, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = CurrentUser.from(jwt);
        request.shows().forEach(s -> guard.requireScreenAccess(user, s.screenId()));
        return showImportService.importShows(request);
    }

    @Operation(summary = "Verify the text of a ticket QR code at the entrance (only for shows of your own theatres)")
    @GetMapping("/tickets/verify")
    public TicketVerification verifyTicket(@RequestParam("payload") String payload, @AuthenticationPrincipal Jwt jwt) {
        TicketVerification result = ticketService.verify(payload);
        if (result.booking() != null && !guard.canManageShow(CurrentUser.from(jwt), result.booking().showId())) {
            return new TicketVerification(false, "This ticket is not for one of your theatres", null);
        }
        return result;
    }

    @Operation(summary = "Onboard a theatre (you become its owner)")
    @PostMapping("/theatres")
    @ResponseStatus(HttpStatus.CREATED)
    public TheatreResponse onboardTheatre(@Valid @RequestBody TheatreRequest request, @AuthenticationPrincipal Jwt jwt) {
        return partnerService.onboardTheatre(request, CurrentUser.from(jwt).userId());
    }

    @Operation(summary = "Add a screen (seats are generated from rows and seats per row)")
    @PostMapping("/theatres/{theatreId}/screens")
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse addScreen(@PathVariable Long theatreId, @Valid @RequestBody ScreenRequest request,
                                    @AuthenticationPrincipal Jwt jwt) {
        guard.requireTheatreAccess(CurrentUser.from(jwt), theatreId);
        return partnerService.addScreen(theatreId, request);
    }

    @Operation(summary = "Create a show")
    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(@Valid @RequestBody ShowRequest request, @AuthenticationPrincipal Jwt jwt) {
        guard.requireScreenAccess(CurrentUser.from(jwt), request.screenId());
        return partnerService.createShow(request);
    }

    @Operation(summary = "Update the time or base price of a show")
    @PutMapping("/shows/{showId}")
    public ShowResponse updateShow(@PathVariable Long showId, @Valid @RequestBody UpdateShowRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        guard.requireShowAccess(CurrentUser.from(jwt), showId);
        return partnerService.updateShow(showId, request);
    }

    @Operation(summary = "Delete a show (bookings are cancelled and refunded)")
    @DeleteMapping("/shows/{showId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShow(@PathVariable Long showId, @AuthenticationPrincipal Jwt jwt) {
        guard.requireShowAccess(CurrentUser.from(jwt), showId);
        partnerService.deleteShow(showId);
    }

    @Operation(summary = "Allocate and update seat inventory (price, block, release, add seats)")
    @PutMapping("/shows/{showId}/seats")
    public List<SeatResponse> updateInventory(@PathVariable Long showId,
                                              @Valid @RequestBody SeatInventoryRequest request,
                                              @AuthenticationPrincipal Jwt jwt) {
        guard.requireShowAccess(CurrentUser.from(jwt), showId);
        return partnerService.updateInventory(showId, request);
    }
}
