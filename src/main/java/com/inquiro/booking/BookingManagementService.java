package com.inquiro.booking;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingManagementService {

    private final BookingJpaRepository bookingRepository;

    public List<BookingEntity> list(String businessId, LocalDate date) {
        return bookingRepository.findAll().stream()
                .filter(booking -> businessId.equals(booking.getBusinessId()))
                .filter(booking -> date == null || date.equals(booking.getBookingDate()))
                .sorted(Comparator
                        .comparing(BookingEntity::getBookingDate)
                        .thenComparing(BookingEntity::getStartTime))
                .toList();
    }

    public BookingEntity get(String businessId, String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        requireBusiness(booking, businessId);
        return booking;
    }

    @Transactional
    public BookingEntity cancel(String businessId, String bookingId) {
        BookingEntity booking = get(businessId, bookingId);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    private void requireBusiness(BookingEntity booking, String businessId) {
        if (!businessId.equals(booking.getBusinessId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Booking not found");
        }
    }
}
