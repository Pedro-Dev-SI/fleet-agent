package com.br.langchain4j.rental.api;

import java.util.UUID;

public record ReservationCancelledResponse(
        Boolean success,
        UUID reservationId,
        String status,
        String message
) {
}
