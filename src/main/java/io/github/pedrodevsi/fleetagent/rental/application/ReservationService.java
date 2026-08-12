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
import io.github.pedrodevsi.fleetagent.rental.repository.ReservationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReservationService implements ReservationUseCase {

    private static final List<ReservationStatusEnum> ACTIVE_STATUSES = List.of(
            ReservationStatusEnum.CREATED,
            ReservationStatusEnum.CONFIRMED
    );
    private final ReservationRepository reservationRepository;
    private final CarService carService;
    private final CustomerUseCase customerUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            CarService carService,
            CustomerUseCase customerUseCase,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.carService = carService;
        this.customerUseCase = customerUseCase;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
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

        Optional<Reservation> reservation = reservationRepository
                .findFirstByCustomerIdAndStatusInOrderByStartDateDesc(
                        customerResponse.customer().id(),
                        ACTIVE_STATUSES
                );

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

        if (reservationRequest.finishDate().isBefore(reservationRequest.startDate())
                || reservationRequest.finishDate().isEqual(reservationRequest.startDate())) {
            return new ReservationCreatedResponse(
                    false,
                    null,
                    "Data de entrega deve ser posterior à data de retirada"
            );
        }

        CustomerLookupResponse customerLookup = customerUseCase.findByDocument(reservationRequest.document());

        if (!customerLookup.found() || customerLookup.customer() == null) {
            return new ReservationCreatedResponse(
                    false,
                    null,
                    customerLookup.message()
            );
        }

        CustomerResponse customer = customerLookup.customer();
        Car car = carService.findCarByModelForUpdate(reservationRequest.carModel());

        Optional<Reservation> existingReservation = reservationRepository.findActiveIdempotentReservation(
                reservationRequest.sessionId(),
                customer.id(),
                car.getId(),
                reservationRequest.startDate(),
                reservationRequest.finishDate(),
                ACTIVE_STATUSES
        );

        if (existingReservation.isPresent()) {
            return new ReservationCreatedResponse(
                    true,
                    toResponse(existingReservation.get()),
                    "Reserva já existe para esta sessão e veículo"
            );
        }

        if (!reservationRequest.startDate().isAfter(LocalDateTime.now(clock))) {
            return new ReservationCreatedResponse(
                    false,
                    null,
                    "Data de retirada deve estar no futuro"
            );
        }

        try {
            car.reserve();
        } catch (IllegalStateException exception) {
            return new ReservationCreatedResponse(false, null, exception.getMessage());
        }

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

        Optional<Reservation> reservationOptional = reservationRepository.findByIdForUpdate(request.reservationId());

        if (reservationOptional.isEmpty()) {
            return cancellationFailure(request.reservationId(), "Reserva não encontrada");
        }

        Reservation reservation = reservationOptional.get();
        UUID customerId = customerLookup.customer().id();

        if (!Objects.equals(reservation.getCustomerId(), customerId)) {
            return cancellationFailure(request.reservationId(), "Reserva não pertence ao cliente informado");
        }

        boolean cancelled;
        try {
            cancelled = reservation.cancel();
        } catch (IllegalStateException exception) {
            return cancellationFailure(request.reservationId(), exception.getMessage());
        }

        if (!cancelled) {
            return new ReservationCancelledResponse(
                    true,
                    reservation.getId(),
                    reservation.getStatus().name(),
                    "Reserva já estava cancelada"
            );
        }

        reservationRepository.save(reservation);

        eventPublisher.publishEvent(new ReservationCancelledEvent(
                reservation.getId(),
                reservation.getCustomerId(),
                reservation.getCar().getId()
        ));

        return new ReservationCancelledResponse(
                true,
                reservation.getId(),
                reservation.getStatus().name(),
                "Reserva cancelada com sucesso"
        );
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
