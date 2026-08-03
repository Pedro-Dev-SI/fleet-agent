package com.br.langchain4j.rental.application;

import com.br.langchain4j.customer.api.CustomerUseCase;
import com.br.langchain4j.customer.api.CustomerLookupResponse;
import com.br.langchain4j.customer.api.CustomerResponse;
import com.br.langchain4j.rental.api.ReservationUseCase;
import com.br.langchain4j.rental.api.event.ReservationCreatedEvent;
import com.br.langchain4j.rental.domain.Car;
import com.br.langchain4j.rental.domain.Reservation;
import com.br.langchain4j.rental.api.CreateReservationRequest;
import com.br.langchain4j.rental.api.ReservationCompletedResponse;
import com.br.langchain4j.rental.api.ReservationResponse;
import com.br.langchain4j.rental.domain.enums.StatusVeichleEnum;
import com.br.langchain4j.rental.repository.ReservationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService implements ReservationUseCase {


    private final ReservationRepository reservationRepository;
    private final CarService carService;
    private final CustomerUseCase customerUseCase;
    private final ApplicationEventPublisher eventPublisher;

    public ReservationService(
            ReservationRepository reservationRepository,
            CarService carService,
            CustomerUseCase customerUseCase, ApplicationEventPublisher eventPublisher
    ) {
        this.reservationRepository = reservationRepository;
        this.carService = carService;
        this.customerUseCase = customerUseCase;
        this.eventPublisher = eventPublisher;
    }

    public ReservationCompletedResponse findByCustomerDocument(String document) {

        if (document == null || document.isBlank()) {
            return new ReservationCompletedResponse(
                    false,
                    null,
                    "CPF do cliente é obrigatório para consultar a reserva"
            );
        }

        CustomerLookupResponse customerResponse = customerUseCase.findByDocument(document);

        if (!customerResponse.found() || customerResponse.customer() == null) {
            return new ReservationCompletedResponse(
                    false,
                    null,
                    customerResponse.message()
            );
        }

        Optional<Reservation> reservation = reservationRepository.findByCustomerId(customerResponse.customer().id());

        return reservation.map(value -> new ReservationCompletedResponse(
                true,
                toResponse(value),
                "Reserva encontrada"
        )).orElseGet(() -> new ReservationCompletedResponse(
                false,
                null,
                "Reserva não encontrada"
        ));

    }

    @Transactional
    public ReservationCompletedResponse createReservation(CreateReservationRequest reservationRequest) {

        if (isInvalidRequest(reservationRequest)) {
            return new ReservationCompletedResponse(
                    false,
                    null,
                    "Dados da reserva são obrigatórios"
            );
        }

        var reservationOp = findReservationBySessionIdAndCarModelOptional(reservationRequest.sessionId(), reservationRequest.carModel());

        if (reservationOp.isPresent()) {
            return new ReservationCompletedResponse(
                    true,
                    toResponse(reservationOp.get()),
                    "Reserva já existe para esta sessão e veículo"
            );
        }

        if (reservationRequest.finishDate().isBefore(reservationRequest.startDate())
                || reservationRequest.finishDate().isEqual(reservationRequest.startDate())) {
            return new ReservationCompletedResponse(
                    false,
                    null,
                    "Data de entrega deve ser posterior à data de retirada"
            );
        }

        CustomerLookupResponse customerLookup = customerUseCase.findByDocument(reservationRequest.document());

        if (!customerLookup.found()) {
            return new ReservationCompletedResponse(
                    false,
                    null,
                    customerLookup.message()
            );
        }

        CustomerResponse customer = customerLookup.customer();

        if (!carService.checkAvailabilityByCarModel(reservationRequest.carModel())) {
            return new ReservationCompletedResponse(
                    false,
                    null,
                    "Veículo se encontra indisponível no momento"
            );
        }

        Car car = carService.findCarByModel(reservationRequest.carModel());
        car.setStatus(StatusVeichleEnum.RESERVADO);

        Reservation reservation = new Reservation(
                car,
                customer.id(),
                reservationRequest.sessionId(),
                reservationRequest.startDate(),
                reservationRequest.finishDate()
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        eventPublisher.publishEvent(new ReservationCreatedEvent(
                savedReservation.getId(),
                savedReservation.getCustomerId(),
                savedReservation.getCar().getId(),
                savedReservation.getStartDate(),
                savedReservation.getEndDate()
        ));

        return new ReservationCompletedResponse(
                true,
                toResponse(savedReservation),
                "Reserva realizada com sucesso"
        );
    }

    private Optional<Reservation> findReservationBySessionIdAndCarModelOptional(UUID sessionId, String carModel) {

        Car car = carService.findCarByModel(carModel);

        return reservationRepository.findBySessionIdAndCarId(sessionId, car.getId());

    }

    private ReservationResponse toResponse(Reservation reservation) {
        CustomerLookupResponse customerLookup = customerUseCase.findById(reservation.getCustomerId());
        CustomerResponse customer = customerLookup.customer();

        if (!customerLookup.found() || customer == null) {
            return new ReservationResponse(
                reservation.getCar().getModel(),
                reservation.getCar().getCategory().getCode(),
                reservation.getCar().getPlate(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                "Cliente não encontrado",
                null,
                null
            );
        }

        return new ReservationResponse(
            reservation.getCar().getModel(),
            reservation.getCar().getCategory().getCode(),
            reservation.getCar().getPlate(),
            reservation.getStartDate(),
            reservation.getEndDate(),
            customer.name(),
            customer.document(),
            customer.phone()
        );
    }

    private boolean isInvalidRequest(CreateReservationRequest reservationRequest) {
        return reservationRequest == null
                || reservationRequest.sessionId() == null
                || reservationRequest.document() == null
                || reservationRequest.document().isBlank()
                || reservationRequest.startDate() == null
                || reservationRequest.finishDate() == null
                || reservationRequest.carModel() == null
                || reservationRequest.carModel().isBlank();
    }
}
