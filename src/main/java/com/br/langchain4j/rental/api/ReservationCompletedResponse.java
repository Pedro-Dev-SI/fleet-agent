package com.br.langchain4j.rental.api;

public record ReservationCompletedResponse(
        Boolean success,
        ReservationResponse reservation,
        String message
) {
}
