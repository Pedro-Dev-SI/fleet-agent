package com.br.langchain4j.rental.api;

public record ReservationCreatedResponse(
        Boolean success,
        ReservationResponse reservation,
        String message
) {
}
