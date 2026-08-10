package io.github.pedrodevsi.fleetagent.rental.api;

import java.util.UUID;

public record ReservationCancelledResponse(
        Boolean success,
        UUID reservationId,
        String status,
        String message
) {
}
