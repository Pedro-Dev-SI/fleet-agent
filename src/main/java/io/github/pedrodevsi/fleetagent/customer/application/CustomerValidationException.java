package io.github.pedrodevsi.fleetagent.customer.application;

public class CustomerValidationException extends RuntimeException {

    public CustomerValidationException(String message) {
        super(message);
    }
}
