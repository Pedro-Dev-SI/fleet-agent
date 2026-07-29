package com.br.langchain4j.rental.api;

import java.time.LocalDateTime;

public record ReservationResponse(
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
