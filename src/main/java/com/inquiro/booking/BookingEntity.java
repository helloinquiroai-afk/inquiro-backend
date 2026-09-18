package com.inquiro.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "booking",
        indexes = {
                @Index(
                        name = "idx_booking_business_datetime",
                        columnList = "business_id,booking_date,start_time"
                ),
                @Index(
                        name = "idx_booking_business_dates",
                        columnList = "business_id,booking_date,check_out_date"
                )
        }
)
public class BookingEntity {

    @Id
    @Column(name = "booking_id", nullable = false, updatable = false)
    private String bookingId;

    @Column(name = "business_id", nullable = false, updatable = false)
    private String businessId;

    @Column(name = "service", nullable = false)
    private String service;

    /** Check-in date for hotel stays; booking date for point-in-time services. */
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    /** Check-out date for hotel stays. Null for point-in-time services. */
    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    /** Number of nights for ROOM_BOOKING. Null for other services. */
    @Column(name = "duration_nights")
    private Integer durationNights;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected BookingEntity() {}

    /** Existing constructor retained for non-hotel bookings and compatibility. */
    public BookingEntity(
            String bookingId, String businessId, String service,
            LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
            String customerName, String customerPhone,
            BookingStatus status, LocalDateTime createdAt) {
        this(bookingId, businessId, service, bookingDate, null, null,
                startTime, endTime, customerName, customerPhone, status, createdAt);
    }

    public BookingEntity(
            String bookingId, String businessId, String service,
            LocalDate bookingDate, LocalDate checkOutDate, Integer durationNights,
            LocalTime startTime, LocalTime endTime,
            String customerName, String customerPhone,
            BookingStatus status, LocalDateTime createdAt) {
        this.bookingId = bookingId;
        this.businessId = businessId;
        this.service = service;
        this.bookingDate = bookingDate;
        this.checkOutDate = checkOutDate;
        this.durationNights = durationNights;
        this.startTime = startTime;
        this.endTime = endTime;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getBookingId() { return bookingId; }
    public String getBusinessId() { return businessId; }
    public String getService() { return service; }
    public LocalDate getBookingDate() { return bookingDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public Integer getDurationNights() { return durationNights; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public BookingStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setStatus(BookingStatus status) { this.status = status; }
}
