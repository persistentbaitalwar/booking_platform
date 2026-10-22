package com.persistent.booking_platform.service;

import static com.persistent.booking_platform.model.Requests.BookingRequest;
import static com.persistent.booking_platform.model.Requests.BulkBookingRequest;
import static com.persistent.booking_platform.model.Requests.BulkCancelRequest;
import static com.persistent.booking_platform.model.Responses.BookingResponse;
import static com.persistent.booking_platform.model.Responses.BulkItemResult;
import static com.persistent.booking_platform.model.Responses.BulkOperationResponse;

import com.persistent.booking_platform.exception.ApiException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bulk operations. Not itself
 *  transactional: in the default (partial) mode every item runs in its own
 * transaction through {@link BookingService}, so one failure never blocks the others. With allOrNothing the whole
 * batch runs in a single transaction and is rolled back if any item fails.
 */
@Service
public class BulkBookingService {
    private final BookingService bookingService;
    private final TransactionTemplate transactionTemplate;

    public BulkBookingService(BookingService bookingService, PlatformTransactionManager transactionManager) {
        this.bookingService = bookingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public BulkOperationResponse bulkBook(BulkBookingRequest request) {
        return run(request.bookings(), request.allOrNothing(), bookingService::book);
    }

    public BulkOperationResponse bulkCancel(BulkCancelRequest request) {
        return run(request.bookingIds(), request.allOrNothing(), bookingService::cancel);
    }

    private <T> BulkOperationResponse run(List<T> items, boolean allOrNothing, Function<T, BookingResponse> operation) {
        if (allOrNothing) {
            return transactionTemplate.execute(status -> {
                List<BulkItemResult> results = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) {
                    try {
                        results.add(success(i, operation.apply(items.get(i))));
                    } catch (ApiException ex) {
                        throw new ApiException(ex.getStatus(), ex.getCode(),
                                "Item " + (i + 1) + " failed, nothing was changed: " + ex.getMessage());
                    }
                }
                return new BulkOperationResponse(results, results.size(), 0);
            });
        }
        List<BulkItemResult> results = new ArrayList<>();
        int succeeded = 0;
        for (int i = 0; i < items.size(); i++) {
            try {
                results.add(success(i, operation.apply(items.get(i))));
                succeeded++;
            } catch (ApiException ex) {
                results.add(new BulkItemResult(i, false, null, null, ex.getCode(), ex.getMessage()));
            } catch (PessimisticLockingFailureException | OptimisticLockingFailureException ex) {
                results.add(new BulkItemResult(i, false, null, null, "CONCURRENT_MODIFICATION",
                        "Another request changed these seats at the same time. Please try this item again."));
            }
        }
        return new BulkOperationResponse(results, succeeded, results.size() - succeeded);
    }

    private BulkItemResult success(int index, BookingResponse response) {
        return new BulkItemResult(index, true, response.bookingId(), response.status(), null, null);
    }
}
