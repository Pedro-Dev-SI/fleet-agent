package com.br.langchain4j.rental.api;

public record AvailableCarResponse(
        String model,
        String category,
        String status
) {
}
