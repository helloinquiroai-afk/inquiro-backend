package com.inquiro.availability;

import com.inquiro.business.BusinessProfile;
import com.inquiro.request.RequestActionType;
import com.inquiro.request.RequestDefinition;
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
        when(bookingRepository
                .findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of());

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
                eq(List.of(BookingStatus.PENDING, BookingStatus.HOLD, BookingStatus.CONFIRMED)),
                eq(END),
                eq(START)
        );
    }

    @Test
    void rejectsSlotWhenBookingConflicts() {
        BookingAvailabilityService service = newService();
        givenBusinessIsOpen();
        when(bookingRepository
                .findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of(booking()));

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
        when(bookingRepository
                .findByBusinessIdAndBookingDateAndStatusInAndStartTimeLessThanAndEndTimeGreaterThan(
                        any(), any(), any(), any(), any()
                ))
                .thenReturn(List.of());

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
                eq(List.of(BookingStatus.PENDING, BookingStatus.HOLD, BookingStatus.CONFIRMED)),
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


    @Test
    void confirmsHotelStayUsingCheckInDateAndDurationNights() {
        BookingAvailabilityService service = newService();
        givenBusinessIsOpen();
        when(bookingRepository.findByBusinessIdAndStatusIn(
                any(), any()
        )).thenReturn(List.of());

        AvailabilityResult result = service.check(
                BUSINESS_ID,
                "ROOM_BOOKING",
                Map.of(
                        "arrival", DATE.toString(),
                        "departure", DATE.plusDays(2).toString()
                ),
                hotelProfile()
        );

        assertEquals(AvailabilityStatus.CONFIRMED, result.status());
        verify(bookingRepository).findByBusinessIdAndStatusIn(
                eq(BUSINESS_ID),
                eq(List.of(BookingStatus.PENDING, BookingStatus.HOLD, BookingStatus.CONFIRMED))
        );
    }

    @Test
    void rejectsHotelStayWhenDateRangeOverlapsExistingBooking() {
        BookingAvailabilityService service = newService();
        givenBusinessIsOpen();
        BookingEntity existing = new BookingEntity(
                "booking_hotel",
                BUSINESS_ID,
                "ROOM_BOOKING",
                DATE.plusDays(2),
                DATE.plusDays(5),
                3,
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                "Existing",
                "0770000000",
                BookingStatus.CONFIRMED,
                LocalDateTime.now()
        );
        when(bookingRepository.findByBusinessIdAndStatusIn(any(), any()))
                .thenReturn(List.of(existing));

        AvailabilityResult result = service.check(
                BUSINESS_ID,
                "ROOM_BOOKING",
                Map.of(
                        "arrival", DATE.toString(),
                        "departure", DATE.plusDays(3).toString()
                ),
                hotelProfile()
        );

        assertEquals(AvailabilityStatus.UNAVAILABLE, result.status());
    }

    private BusinessProfile hotelProfile() {
        return new BusinessProfile(
                "Test Hotel",
                "HOSPITALITY",
                "Hotel",
                List.of(new RequestDefinition(
                        "ROOM_BOOKING",
                        "Book a room",
                        List.of("arrival", "departure"),
                        Map.of(),
                        RequestActionType.BOOKING,
                        List.of("arrival", "departure"),
                        "DATE_RANGE",
                        Map.of(
                                "startDate", "arrival",
                                "endDate", "departure"
                        )
                )),
                new com.inquiro.business.BusinessKnowledge(
                        "Hotel",
                        List.of("ROOM_BOOKING"),
                        List.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        "",
                        Map.of(
                                "MONDAY", "00:00-23:59",
                                "TUESDAY", "00:00-23:59",
                                "WEDNESDAY", "00:00-23:59",
                                "THURSDAY", "00:00-23:59",
                                "FRIDAY", "00:00-23:59",
                                "SATURDAY", "00:00-23:59",
                                "SUNDAY", "00:00-23:59"
                        ),
                        List.of(),
                        Map.of(),
                        Map.of(),
                        List.of(),
                        List.of(),
                        null
                )
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
