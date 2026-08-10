package io.github.pedrodevsi.fleetagent.customer.api;

public record CustomerLookupResponse(
        boolean found,
        CustomerResponse customer,
        String message
) {
}
