package io.github.pedrodevsi.fleetagent.customer.api;

import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String name,
        String document,
        String email,
        String phone,
        String type
) {
}
