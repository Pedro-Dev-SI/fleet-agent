package io.github.pedrodevsi.fleetagent.rental.application;

import io.github.pedrodevsi.fleetagent.customer.api.CustomerUseCase;
import io.github.pedrodevsi.fleetagent.customer.api.CustomerLookupResponse;
import io.github.pedrodevsi.fleetagent.customer.api.CustomerResponse;
import io.github.pedrodevsi.fleetagent.rental.api.CancelReservationRequest;
import io.github.pedrodevsi.fleetagent.rental.api.ReservationCancelledResponse;
import io.github.pedrodevsi.fleetagent.rental.api.ReservationUseCase;
import io.github.pedrodevsi.fleetagent.rental.api.event.ReservationCancelledEvent;
import io.github.pedrodevsi.fleetagent.rental.api.event.ReservationCreatedEvent;
import io.github.pedrodevsi.fleetagent.rental.domain.Car;
import io.github.pedrodevsi.fleetagent.rental.domain.Reservation;
import io.github.pedrodevsi.fleetagent.rental.api.CreateReservationRequest;
import io.github.pedrodevsi.fleetagent.rental.api.ReservationCreatedResponse;
import io.github.pedrodevsi.fleetagent.rental.api.ReservationResponse;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.ReservationStatusEnum;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.StatusVeichleEnum;
import io.github.pedrodevsi.fleetagent.rental.repository.ReservationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Objects;
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
            CustomerUseCase customerUseCase,
            ApplicationEventPublisher eventPublisher
    ) {
        this.reservationRepository = reservationRepository;
        this.carService = carService;
        this.customerUseCase = customerUseCase;
        this.eventPublisher = eventPublisher;
    }

    public ReservationCreatedResponse findByCustomerDocument(String document) {

        if (document == null || document.isBlank()) {
            return new ReservationCreatedResponse(
                    false,
                    null,
                    "CPF do cliente é obrigatório para consultar a reserva"
            );
        }

        CustomerLookupResponse customerResponse = customerUseCase.findByDocument(document);

        if (!customerResponse.found() || customerResponse.customer() == null) {
            return new ReservationCreatedResponse(
                    false,
                    null,
                    customerResponse.message()
            );
        }

        Optional<Reservation> reservation = reservationRepository.findByCustomerId(customerResponse.customer().id());

        return reservation.map(value -> new ReservationCreatedResponse(
                true,
                toResponse(value),
                "Reserva encontrada"
        )).orElseGet(() -> new ReservationCreatedResponse(
                false,
                null,
                "Reserva não encontrada"
        ));

    }

    @Transactional
    public ReservationCreatedResponse createReservation(CreateReservationRequest reservationRequest) {

        if (isInvalidRequest(reservationRequest)) {
            return new ReservationCreatedResponse(
                    false,
                    null,
                    "Dados da reserva são obrigatórios"
            );
        }

        var reservationOp = findReservationBySessionIdAndCarModelOptional(reservationRequest.sessionId(), reservationRequest.carModel());

        if (reservationOp.isPresent()) {
            return new ReservationCreatedResponse(
                    true,
                    toResponse(reservationOp.get()),
                    "Reserva já existe para esta sessão e veículo"
            );
        }

        if (reservationRequest.finishDate().isBefore(reservationRequest.startDate())
                || reservationRequest.finishDate().isEqual(reservationRequest.startDate())) {
            return new ReservationCreatedResponse(
                    false,
                    null,
                    "Data de entrega deve ser posterior à data de retirada"
            );
        }

        CustomerLookupResponse customerLookup = customerUseCase.findByDocument(reservationRequest.document());

        if (!customerLookup.found()) {
            return new ReservationCreatedResponse(
                    false,
                    null,
                    customerLookup.message()
            );
        }

        CustomerResponse customer = customerLookup.customer();

        if (!carService.checkAvailabilityByCarModel(reservationRequest.carModel())) {
            return new ReservationCreatedResponse(
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
                reservationRequest.finishDate(),
                ReservationStatusEnum.CREATED
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        eventPublisher.publishEvent(new ReservationCreatedEvent(
                savedReservation.getId(),
                savedReservation.getCustomerId(),
                savedReservation.getCar().getId(),
                savedReservation.getStartDate(),
                savedReservation.getEndDate()
        ));

        return new ReservationCreatedResponse(
                true,
                toResponse(savedReservation),
                "Reserva criada com sucesso"
        );
    }

    @Override
    @Transactional
    public ReservationCancelledResponse cancelReservation(CancelReservationRequest request) {
        if (isInvalidRequest(request)) {
            return cancellationFailure(null, "Dados para cancelamento são obrigatórios");
        }

        CustomerLookupResponse customerLookup = customerUseCase.findByDocument(request.document());

        if (!customerLookup.found() || customerLookup.customer() == null) {
            return cancellationFailure(request.reservationId(), customerLookup.message());
        }

        Optional<Reservation> reservationOptional = reservationRepository.findById(request.reservationId());

        if (reservationOptional.isEmpty()) {
            return cancellationFailure(request.reservationId(), "Reserva não encontrada");
        }

        Reservation reservation = reservationOptional.get();
        UUID customerId = customerLookup.customer().id();

        if (!Objects.equals(reservation.getCustomerId(), customerId)) {
            return cancellationFailure(request.reservationId(), "Reserva não pertence ao cliente informado");
        }

        try {
            reservation.cancel();
        } catch (IllegalStateException exception) {
            return cancellationFailure(request.reservationId(), exception.getMessage());
        }

        Car car = reservation.getCar();
        car.setStatus(StatusVeichleEnum.DISPONIVEL);

        reservationRepository.save(reservation);

        eventPublisher.publishEvent(new ReservationCancelledEvent(
                reservation.getId(),
                reservation.getCustomerId(),
                car.getId()
        ));

        return new ReservationCancelledResponse(
                true,
                reservation.getId(),
                reservation.getStatus().name(),
                "Reserva cancelada com sucesso"
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
                reservation.getId(),
                reservation.getStatus().name(),
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
            reservation.getId(),
            reservation.getStatus().name(),
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

    private ReservationCancelledResponse cancellationFailure(UUID reservationId, String message) {
        return new ReservationCancelledResponse(false, reservationId, null, message);
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

    private boolean isInvalidRequest(CancelReservationRequest request) {
        return request == null
                || request.reservationId() == null
                || request.document() == null
                || request.document().isBlank();
    }
}
