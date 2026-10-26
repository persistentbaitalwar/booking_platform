package com.persistent.booking_platform.payment;

import com.persistent.booking_platform.service.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Frees the seats of bookings whose payment did not arrive in time. */
@Component
public class HoldExpiryJob {
    private static final Logger LOG = LoggerFactory.getLogger(HoldExpiryJob.class);
    private static final int BATCH = 100;

    private final BookingService bookingService;

    public HoldExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.payments.expiry-check-ms}")
    public void releaseExpiredHolds() {
        for (Long id : bookingService.findExpiredHoldIds(BATCH)) {
            try {
                if (bookingService.expireHold(id)) {
                    LOG.info("Released the seats of booking {} because payment was not completed in time", id);
                }
            } catch (RuntimeException ex) {
                LOG.warn("Could not expire the hold of booking {}: {}", id, ex.getMessage());
            }
        }
    }
}
