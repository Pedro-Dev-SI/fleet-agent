package com.br.langchain4j.rental.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelReservationRequest(
        @NotNull UUID reservationId,
        @NotBlank String document
) {
}
