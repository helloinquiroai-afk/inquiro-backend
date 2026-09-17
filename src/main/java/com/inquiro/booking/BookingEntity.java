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
                )
        }
)
public class BookingEntity {

    @Id
    @Column(
            name = "booking_id",
            nullable = false,
            updatable = false
    )
    private String bookingId;

    @Column(
            name = "business_id",
            nullable = false,
            updatable = false
    )
    private String businessId;

    @Column(
            name = "service",
            nullable = false
    )
    private String service;

    @Column(
            name = "booking_date",
            nullable = false
    )
    private LocalDate bookingDate;

    @Column(
            name = "start_time",
            nullable = false
    )
    private LocalTime startTime;

    @Column(
            name = "end_time",
            nullable = false
    )
    private LocalTime endTime;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false
    )
    private BookingStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    protected BookingEntity() {
        // JPA
    }

    public BookingEntity(
            String bookingId,
            String businessId,
            String service,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime,
            String customerName,
            String customerPhone,
            BookingStatus status,
            LocalDateTime createdAt) {

        this.bookingId = bookingId;
        this.businessId = businessId;
        this.service = service;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getService() {
        return service;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
