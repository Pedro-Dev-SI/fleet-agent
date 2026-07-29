package com.br.langchain4j.rental.api;

public interface ReservationUseCase {

    ReservationCompletedResponse findByCustomerDocument(String document);

    ReservationCompletedResponse createReservation(CreateReservationRequest request);

}
