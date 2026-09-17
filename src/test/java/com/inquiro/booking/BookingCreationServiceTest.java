package com.inquiro.booking;

import com.inquiro.availability.AvailabilityResult;
import com.inquiro.availability.AvailabilityStatus;
import com.inquiro.availability.BookingAvailabilityService;
import com.inquiro.business.BusinessAccount;
import com.inquiro.business.BusinessAccountRepository;
import com.inquiro.business.BusinessKnowledge;
import com.inquiro.business.BusinessProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCreationServiceTest {

    @Mock BusinessAccountRepository businessRepository;
    @Mock BookingJpaRepository bookingRepository;
    @Mock BookingAvailabilityService availabilityService;

    private BookingCreationService service;
    private BusinessAccount business;

    @BeforeEach
    void setUp() {
        service = new BookingCreationService(
                businessRepository,
                bookingRepository,
                availabilityService
        );
        business = new BusinessAccount(
                "biz_001",
                "Test Business",
                new BusinessProfile(
                        "Test Business",
                        "RESTAURANT",
                        "Test",
                        List.of(),
                        BusinessKnowledge.empty()
                )
        );
    }

    @Test
    void createsConfirmedBookingWhenAvailabilityIsConfirmed() {
        when(businessRepository.findByBusinessIdForUpdate("biz_001"))
                .thenReturn(business);
        when(availabilityService.check(
                eq("biz_001"), eq("TABLE_RESERVATION"), anyMap(), eq(business.profile())))
                .thenReturn(new AvailabilityResult(
                        AvailabilityStatus.CONFIRMED, "Available"));
        when(bookingRepository.save(any(BookingEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BookingEntity booking = service.create(
                "biz_001",
                "TABLE_RESERVATION",
                Map.of("date", "2026-09-20", "time", "19:00"),
                "Gayan",
                "+94770000000"
        );

        assertNotNull(booking.getBookingId());
        assertTrue(booking.getBookingId().startsWith("booking_"));
        assertEquals("biz_001", booking.getBusinessId());
        assertEquals(LocalDate.of(2026, 9, 20), booking.getBookingDate());
        assertEquals(LocalTime.of(19, 0), booking.getStartTime());
        assertEquals(LocalTime.of(20, 0), booking.getEndTime());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        verify(bookingRepository).save(any(BookingEntity.class));
    }

    @Test
    void doesNotCreateWhenSlotIsUnavailable() {
        when(businessRepository.findByBusinessIdForUpdate("biz_001"))
                .thenReturn(business);
        when(availabilityService.check(
                eq("biz_001"), eq("TABLE_RESERVATION"), anyMap(), eq(business.profile())))
                .thenReturn(new AvailabilityResult(
                        AvailabilityStatus.UNAVAILABLE, "Requested time is already booked"));

        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.create(
                        "biz_001",
                        "TABLE_RESERVATION",
                        Map.of("date", "2026-09-20", "time", "19:00"),
                        "Gayan",
                        "+94770000000"
                )
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownAvailability() {
        when(businessRepository.findByBusinessIdForUpdate("biz_001"))
                .thenReturn(business);
        when(availabilityService.check(
                eq("biz_001"), eq("TABLE_RESERVATION"), anyMap(), eq(business.profile())))
                .thenReturn(new AvailabilityResult(
                        AvailabilityStatus.UNKNOWN, "Availability cannot be confirmed"));

        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.create(
                        "biz_001",
                        "TABLE_RESERVATION",
                        Map.of("date", "2026-09-20", "time", "19:00"),
                        "Gayan",
                        "+94770000000"
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsMissingCustomerName() {
        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.create(
                        "biz_001",
                        "TABLE_RESERVATION",
                        Map.of("date", "2026-09-20", "time", "19:00"),
                        " ",
                        "+94770000000"
                )
        );

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(businessRepository, availabilityService, bookingRepository);
    }

    @Test
    void rejectsMissingBusiness() {
        when(businessRepository.findByBusinessIdForUpdate("biz_001"))
                .thenReturn(null);

        var exception = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.create(
                        "biz_001",
                        "TABLE_RESERVATION",
                        Map.of("date", "2026-09-20", "time", "19:00"),
                        "Gayan",
                        "+94770000000"
                )
        );

        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(availabilityService, bookingRepository);
    }
}
