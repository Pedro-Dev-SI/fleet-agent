package com.br.langchain4j.customer.api;

public record CustomerLookupResponse(
        boolean found,
        CustomerResponse customer,
        String message
) {
}
