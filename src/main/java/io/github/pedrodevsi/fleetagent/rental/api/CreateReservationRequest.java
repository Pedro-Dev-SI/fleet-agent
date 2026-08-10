package io.github.pedrodevsi.fleetagent.rental.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateReservationRequest(
        @NotNull
        UUID sessionId,
        @NotBlank
        String document,
        @NotNull
        LocalDateTime startDate,
        @NotNull
        LocalDateTime finishDate,
        @NotBlank
        String carModel
) {
}
