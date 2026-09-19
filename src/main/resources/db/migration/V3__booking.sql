CREATE TABLE IF NOT EXISTS booking (
    booking_id VARCHAR(128) PRIMARY KEY,
    business_id VARCHAR(128) NOT NULL,
    service VARCHAR(255) NOT NULL,
    booking_date DATE NOT NULL,
    check_out_date DATE,
    duration_nights INTEGER,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    customer_name VARCHAR(200),
    customer_phone VARCHAR(50),
    idempotency_key VARCHAR(200) UNIQUE,
    hold_expires_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_business
        FOREIGN KEY (business_id) REFERENCES business_account (business_id)
);

CREATE INDEX IF NOT EXISTS idx_booking_business_datetime
    ON booking (business_id, booking_date, start_time);

CREATE INDEX IF NOT EXISTS idx_booking_business_dates
    ON booking (business_id, booking_date, check_out_date);

CREATE INDEX IF NOT EXISTS idx_booking_idempotency
    ON booking (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_booking_business_status
    ON booking (business_id, status);


CREATE TABLE IF NOT EXISTS booking_inventory (
    inventory_id VARCHAR(128) PRIMARY KEY,
    business_id VARCHAR(128) NOT NULL,
    service VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_booking_inventory_business
        FOREIGN KEY (business_id) REFERENCES business_account (business_id),
    CONSTRAINT uq_booking_inventory_business_service
        UNIQUE (business_id, service),
    CONSTRAINT chk_booking_inventory_capacity
        CHECK (capacity > 0)
);

CREATE INDEX IF NOT EXISTS idx_booking_inventory_business
    ON booking_inventory (business_id);
