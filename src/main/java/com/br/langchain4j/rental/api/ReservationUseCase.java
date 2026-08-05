package com.br.langchain4j.rental.api;

public interface ReservationUseCase {

    ReservationCreatedResponse findByCustomerDocument(String document);

    ReservationCreatedResponse createReservation(CreateReservationRequest request);

    ReservationCancelledResponse cancelReservation(CancelReservationRequest request);

}
