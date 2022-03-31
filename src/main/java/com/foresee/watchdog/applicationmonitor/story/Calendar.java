package com.foresee.watchdog.applicationmonitor.story;

import com.foresee.api_automation.tests.microservices.calendar_service.DateRangeControllerTest;
import com.foresee.watchdog.applicationmonitor.service.Mornitor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor

public class Calendar {
    private final Mornitor mornitor;

    @PostConstruct
    public void postConstruct() {
        DateRangeControllerTest test = new DateRangeControllerTest();
        mornitor.register("calendar_last_7_days", () -> {
            try {
                test.testGETLookupDateRange200FiscalCalendarLast7Days_16071810();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
        mornitor.register("calendar_last_month", () -> {
            try {
                test.testGETLookupDateRange200FiscalCalendarLastMonth_16071807();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
    }
}
