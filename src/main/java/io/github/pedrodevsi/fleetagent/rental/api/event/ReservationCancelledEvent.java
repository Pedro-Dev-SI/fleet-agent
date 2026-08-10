package io.github.pedrodevsi.fleetagent.rental.api.event;

import java.util.UUID;

public record ReservationCancelledEvent(
        UUID reservationId,
        UUID customerId,
        UUID carId
) {
}
