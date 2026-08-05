package com.br.langchain4j.rental.api.event;

import java.util.UUID;

public record ReservationCancelledEvent(
        UUID reservationId,
        UUID customerId,
        UUID carId
) {
}
