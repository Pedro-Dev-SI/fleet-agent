package io.github.pedrodevsi.fleetagent.rental.api;

public record AvailableCarResponse(
        String model,
        String category,
        String status
) {
}
