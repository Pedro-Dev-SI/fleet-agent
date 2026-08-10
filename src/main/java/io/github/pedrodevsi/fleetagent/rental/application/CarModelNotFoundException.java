package io.github.pedrodevsi.fleetagent.rental.application;

public class CarModelNotFoundException extends RuntimeException {

    public CarModelNotFoundException(String message) {
        super(message);
    }
}
