package io.github.pedrodevsi.fleetagent.rental.api;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(
    UUID reservationId,
    String status,
    String carModel,
    String carCategory,
    String carPlate,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String customerName,
    String customerDocument,
    String customerPhone
) {
}
