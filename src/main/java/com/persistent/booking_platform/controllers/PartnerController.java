package com.persistent.booking_platform.controllers;

import static com.persistent.booking_platform.model.Requests.*;
import static com.persistent.booking_platform.model.Responses.*;


import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
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

import com.persistent.booking_platform.service.PartnerService;
import com.persistent.booking_platform.service.ShowImportService;
import com.persistent.booking_platform.service.TicketService;

/** B2B API for theatre partners. */
@RestController
@RequestMapping("/api/v1/partner")
public class PartnerController {
    private final PartnerService partnerService;
    private final ShowImportService showImportService;
    private final TicketService ticketService;

    public PartnerController(PartnerService partnerService, ShowImportService showImportService,
                             TicketService ticketService) {
        this.partnerService = partnerService;
        this.showImportService = showImportService;
        this.ticketService = ticketService;
    }

    /** Bulk schedule upload for theatres that run their own scheduling system. */
    @PostMapping("/shows/import")
    public ImportResponse importShows(@Valid @RequestBody ShowImportRequest request) {
        return showImportService.importShows(request);
    }

    /** Entrance check: scan the QR code and send its text here. */
    @GetMapping("/tickets/verify")
    public TicketVerification verifyTicket(@RequestParam("payload") String payload) {
        return ticketService.verify(payload);
    }

    @PostMapping("/theatres")
    @ResponseStatus(HttpStatus.CREATED)
    public TheatreResponse onboardTheatre(@Valid @RequestBody TheatreRequest request) {
        return partnerService.onboardTheatre(request);
    }

    @PostMapping("/theatres/{theatreId}/screens")
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse addScreen(@PathVariable Long theatreId, @Valid @RequestBody ScreenRequest request) {
        return partnerService.addScreen(theatreId, request);
    }

    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(@Valid @RequestBody ShowRequest request) {
        return partnerService.createShow(request);
    }

    @PutMapping("/shows/{showId}")
    public ShowResponse updateShow(@PathVariable Long showId, @Valid @RequestBody UpdateShowRequest request) {
        return partnerService.updateShow(showId, request);
    }

    @DeleteMapping("/shows/{showId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShow(@PathVariable Long showId) {
        partnerService.deleteShow(showId);
    }

    @PutMapping("/shows/{showId}/seats")
    public List<SeatResponse> updateInventory(@PathVariable Long showId,
                                              @Valid @RequestBody SeatInventoryRequest request) {
        return partnerService.updateInventory(showId, request);
    }
}
