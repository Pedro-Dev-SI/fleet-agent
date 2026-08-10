package io.github.pedrodevsi.fleetagent.rental.api;

public interface ReservationUseCase {

    ReservationCreatedResponse findByCustomerDocument(String document);

    ReservationCreatedResponse createReservation(CreateReservationRequest request);

    ReservationCancelledResponse cancelReservation(CancelReservationRequest request);

}
