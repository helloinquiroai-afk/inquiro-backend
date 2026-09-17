package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.booking.BookingEntity;
import com.inquiro.booking.BookingJpaRepository;
import com.inquiro.booking.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingAvailabilityServiceTest {

    private static final String BUSINESS_ID = "biz_001";
    private static final String SERVICE = "TABLE_RESERVATION";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 21);
    private static final LocalTime START = LocalTime.of(10, 0);
    private static final LocalTime END = LocalTime.of(11, 0);

    @Mock
    private BookingJpaRepository bookingRepository;

    @Mock
    private BusinessScheduleAvailabilitySource scheduleSource;

    @Test
    void confirmsSlotWhenBusinessIsOpenAndNoBookingConflicts() {
        BookingAvailabilityService service = newService();
        givenBusinessIsOpen();
        when(conflicts()).thenReturn(List.<BookingEntity>of());

        AvailabilityResult result = service.check(
                BUSINESS_ID,
                SERVICE,
                fields(),
                profile()
        );

        assertEquals(AvailabilityStatus.CONFIRMED, result.status());
        verify(bookingRepository).findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(BUSINESS_ID),
                eq(DATE),
                eq(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)),
                eq(END),
                eq(START)
        );
    }

    @Test
    void rejectsSlotWhenBookingConflicts() {
        BookingAvailabilityService service = newService();
        givenBusinessIsOpen();
        when(conflicts()).thenReturn(List.<BookingEntity>of(booking()));

        AvailabilityResult result = service.check(
                BUSINESS_ID,
                SERVICE,
                fields(),
                profile()
        );

        assertEquals(AvailabilityStatus.UNAVAILABLE, result.status());
    }

    @Test
    void cancelledBookingDoesNotBlockSlot() {
        BookingAvailabilityService service = newService();
        givenBusinessIsOpen();
        when(conflicts()).thenReturn(List.<BookingEntity>of());

        AvailabilityResult result = service.check(
                BUSINESS_ID,
                SERVICE,
                fields(),
                profile()
        );

        assertEquals(AvailabilityStatus.CONFIRMED, result.status());
        verify(bookingRepository).findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                eq(BUSINESS_ID),
                eq(DATE),
                eq(List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)),
                eq(END),
                eq(START)
        );
    }

    @Test
    void returnsUnavailableWhenBusinessIsClosed() {
        BookingAvailabilityService service = newService();
        when(scheduleSource.check(any(), any(), any()))
                .thenReturn(new AvailabilityResult(
                        AvailabilityStatus.UNAVAILABLE,
                        "The business is closed."
                ));

        AvailabilityResult result = service.check(
                BUSINESS_ID,
                SERVICE,
                fields(),
                profile()
        );

        assertEquals(AvailabilityStatus.UNAVAILABLE, result.status());
        verify(bookingRepository, never())
                .findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), any(), any(), any()
                );
    }

    @Test
    void returnsUnknownWhenDateOrTimeIsMissing() {
        BookingAvailabilityService service = newService();
        givenBusinessIsOpen();

        AvailabilityResult result = service.check(
                BUSINESS_ID,
                SERVICE,
                Map.of("date", DATE.toString()),
                profile()
        );

        assertEquals(AvailabilityStatus.UNKNOWN, result.status());
        verify(bookingRepository, never())
                .findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), any(), any(), any()
                );
    }

    @Test
    void rejectsInvalidTimeRange() {
        BookingAvailabilityService service = newService();
        givenBusinessIsOpen();

        AvailabilityResult result = service.check(
                BUSINESS_ID,
                SERVICE,
                Map.of(
                        "date", DATE.toString(),
                        "time", "11:00",
                        "endTime", "10:00"
                ),
                profile()
        );

        assertEquals(AvailabilityStatus.UNKNOWN, result.status());
        verify(bookingRepository, never())
                .findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), any(), any(), any()
                );
    }

    private BookingAvailabilityService newService() {
        return new BookingAvailabilityService(bookingRepository, scheduleSource);
    }

    private BusinessProfile profile() {
        return new BusinessProfile(
                "Test Business",
                "RESTAURANT",
                "Test",
                List.of(),
                null
        );
    }

    private Map<String, Object> fields() {
        return Map.of(
                "date", DATE.toString(),
                "time", "10:00",
                "endTime", "11:00"
        );
    }

    private void givenBusinessIsOpen() {
        when(scheduleSource.check(any(), any(), any()))
                .thenReturn(new AvailabilityResult(
                        AvailabilityStatus.CONFIRMED,
                        "Business is open."
                ));
    }

    private org.mockito.stubbing.OngoingStubbing<List<BookingEntity>> conflicts() {
        return when(bookingRepository
                .findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), any(), any(), any()
                ));
    }

    private BookingEntity booking() {
        return new BookingEntity(
                "booking_test",
                BUSINESS_ID,
                SERVICE,
                DATE,
                LocalTime.of(10, 30),
                LocalTime.of(11, 30),
                "Customer",
                "0770000000",
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );
    }
}
