ALTER TABLE reservation
ADD CONSTRAINT chk_reservation_valid_period
CHECK (end_date > start_date);

ALTER TABLE car
ADD CONSTRAINT uq_car_model UNIQUE (model);

ALTER TABLE car
ADD CONSTRAINT uq_car_plate UNIQUE (plate);

CREATE UNIQUE INDEX uq_reservation_active_car
ON reservation (car_id)
WHERE status IN ('CREATED', 'CONFIRMED');

CREATE INDEX idx_reservation_customer_active_start
ON reservation (customer_id, start_date DESC)
WHERE status IN ('CREATED', 'CONFIRMED');

CREATE INDEX idx_reservation_active_end_date
ON reservation (end_date)
WHERE status IN ('CREATED', 'CONFIRMED');
