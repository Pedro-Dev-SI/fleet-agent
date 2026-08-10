package io.github.pedrodevsi.fleetagent.rental.api.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationCreatedEvent(
        UUID reservationId,
        UUID customerId,
        UUID carId,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
