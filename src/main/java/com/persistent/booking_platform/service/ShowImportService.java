package com.persistent.booking_platform.service;

import static com.persistent.booking_platform.model.Requests.ShowImportRequest;
import static com.persistent.booking_platform.model.Requests.ShowRequest;
import static com.persistent.booking_platform.model.Responses.ImportItemResult;
import static com.persistent.booking_platform.model.Responses.ImportResponse;
import static com.persistent.booking_platform.model.Responses.ShowResponse;

import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.persistent.booking_platform.exception.ApiException;

/**
 * Entry point for theatres that already run their own IT system: their scheduler pushes a day's or week's shows
 * in one call. Each show is created in its own transaction, so one clash does not reject the whole file.
 */
@Service
public class ShowImportService {
    private final PartnerService partnerService;

    public ShowImportService(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    public ImportResponse importShows(ShowImportRequest request) {
        List<ImportItemResult> results = new ArrayList<>();
        int imported = 0;
        List<ShowRequest> shows = request.shows();
        for (int i = 0; i < shows.size(); i++) {
            try {
                ShowResponse created = partnerService.createShow(shows.get(i));
                results.add(new ImportItemResult(i, true, created.id(), null, null));
                imported++;
            } catch (ApiException ex) {
                results.add(new ImportItemResult(i, false, null, ex.getCode(), ex.getMessage()));
            } catch (DataAccessException ex) {
                results.add(new ImportItemResult(i, false, null, "CONCURRENT_MODIFICATION",
                        "The screen was being changed at the same time. Please send this show again."));
            }
        }
        return new ImportResponse(results, imported, results.size() - imported);
    }
}
