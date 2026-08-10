package io.github.pedrodevsi.fleetagent.rental.api;

public record ReservationCreatedResponse(
        Boolean success,
        ReservationResponse reservation,
        String message
) {
}
