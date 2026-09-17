package com.inquiro.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingManagementServiceTest {

    @Mock BookingJpaRepository repository;

    private BookingManagementService service;

    @BeforeEach
    void setUp() {
        service = new BookingManagementService(repository);
    }

    private BookingEntity booking(
            String id,
            String businessId,
            LocalDate date,
            LocalTime start,
            BookingStatus status) {
        return new BookingEntity(
                id,
                businessId,
                "TABLE_RESERVATION",
                date,
                start,
                start.plusHours(1),
                "Gayan",
                "+94770000000",
                status,
                LocalDateTime.now()
        );
    }

    @Test
    void listsOnlyBusinessBookingsAndSortsByDateAndTime() {
        BookingEntity later = booking("b2", "biz_001", LocalDate.of(2026, 9, 20), LocalTime.of(20, 0), BookingStatus.CONFIRMED);
        BookingEntity earlier = booking("b1", "biz_001", LocalDate.of(2026, 9, 20), LocalTime.of(19, 0), BookingStatus.CONFIRMED);
        BookingEntity otherBusiness = booking("b3", "biz_002", LocalDate.of(2026, 9, 20), LocalTime.of(18, 0), BookingStatus.CONFIRMED);

        when(repository.findAll()).thenReturn(List.of(later, otherBusiness, earlier));

        List<BookingEntity> result = service.list("biz_001", LocalDate.of(2026, 9, 20));

        assertEquals(List.of(earlier, later), result);
    }

    @Test
    void getsBookingForOwningBusiness() {
        BookingEntity booking = booking("b1", "biz_001", LocalDate.of(2026, 9, 20), LocalTime.of(19, 0), BookingStatus.CONFIRMED);
        when(repository.findById("b1")).thenReturn(Optional.of(booking));

        assertSame(booking, service.get("biz_001", "b1"));
    }

    @Test
    void hidesBookingFromAnotherBusiness() {
        BookingEntity booking = booking("b1", "biz_002", LocalDate.of(2026, 9, 20), LocalTime.of(19, 0), BookingStatus.CONFIRMED);
        when(repository.findById("b1")).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.get("biz_001", "b1")
        );

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void cancelsConfirmedBooking() {
        BookingEntity booking = booking("b1", "biz_001", LocalDate.of(2026, 9, 20), LocalTime.of(19, 0), BookingStatus.CONFIRMED);
        when(repository.findById("b1")).thenReturn(Optional.of(booking));
        when(repository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingEntity result = service.cancel("biz_001", "b1");

        assertEquals(BookingStatus.CANCELLED, result.getStatus());
        verify(repository).save(booking);
    }

    @Test
    void rejectsAlreadyCancelledBooking() {
        BookingEntity booking = booking("b1", "biz_001", LocalDate.of(2026, 9, 20), LocalTime.of(19, 0), BookingStatus.CANCELLED);
        when(repository.findById("b1")).thenReturn(Optional.of(booking));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.cancel("biz_001", "b1")
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsMissingBooking() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.get("biz_001", "missing")
        );

        assertEquals(404, exception.getStatusCode().value());
    }
}
