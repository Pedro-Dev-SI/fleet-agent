package io.github.pedrodevsi.fleetagent.customer.api;

import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank
        String name,
        @NotBlank
        String document,
        @NotBlank
        String email,
        @NotBlank
        String phone,
        @NotBlank
        String type
) {
}
