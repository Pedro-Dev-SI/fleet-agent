package com.br.langchain4j.customer.api;

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
