package io.github.pedrodevsi.fleetagent.rental.application;

public class RentalCategoryNotFoundException extends RuntimeException {

    public RentalCategoryNotFoundException(String message) {
        super(message);
    }
}
