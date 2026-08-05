package com.br.langchain4j.rental.application;

import com.br.langchain4j.customer.api.CustomerLookupResponse;
import com.br.langchain4j.customer.api.CustomerResponse;
import com.br.langchain4j.customer.api.CustomerUseCase;
import com.br.langchain4j.rental.api.CancelReservationRequest;
import com.br.langchain4j.rental.api.CreateReservationRequest;
import com.br.langchain4j.rental.api.ReservationCancelledResponse;
import com.br.langchain4j.rental.api.ReservationCreatedResponse;
import com.br.langchain4j.rental.api.event.ReservationCancelledEvent;
import com.br.langchain4j.rental.api.event.ReservationCreatedEvent;
import com.br.langchain4j.rental.domain.Car;
import com.br.langchain4j.rental.domain.RentalCategory;
import com.br.langchain4j.rental.domain.Reservation;
import com.br.langchain4j.rental.domain.enums.ReservationStatusEnum;
import com.br.langchain4j.rental.domain.enums.StatusVeichleEnum;
import com.br.langchain4j.rental.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationServiceTest {

    private static final String CUSTOMER_DOCUMENT = "123.456.789-00";

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final CarService carService = mock(CarService.class);
    private final CustomerUseCase customerUseCase = mock(CustomerUseCase.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ReservationService reservationService = new ReservationService(
            reservationRepository,
            carService,
            customerUseCase,
            eventPublisher
    );

    @Test
    void shouldReturnExistingReservationForSessionAndCar() {
        UUID sessionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Car car = car();
        Reservation existingReservation = reservation(
                UUID.randomUUID(),
                customerId,
                sessionId,
                car,
                ReservationStatusEnum.CREATED
        );

        when(carService.findCarByModel("Onix")).thenReturn(car);
        when(reservationRepository.findBySessionIdAndCarId(sessionId, null)).thenReturn(Optional.of(existingReservation));
        when(customerUseCase.findById(customerId)).thenReturn(foundCustomer(customerId));

        ReservationCreatedResponse response = reservationService.createReservation(createRequest(sessionId));

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Reserva já existe para esta sessão e veículo");
        assertThat(response.reservation().customerName()).isEqualTo("Maria Silva");
        assertThat(response.reservation().status()).isEqualTo("CREATED");
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ReservationCreatedEvent.class));
    }

    @Test
    void shouldRejectReservationWhenEndDateIsNotAfterStartDate() {
        UUID sessionId = UUID.randomUUID();
        when(carService.findCarByModel("Onix")).thenReturn(car());
        when(reservationRepository.findBySessionIdAndCarId(sessionId, null)).thenReturn(Optional.empty());

        CreateReservationRequest request = new CreateReservationRequest(
                sessionId,
                CUSTOMER_DOCUMENT,
                LocalDateTime.parse("2026-08-01T10:00:00"),
                LocalDateTime.parse("2026-08-01T10:00:00"),
                "Onix"
        );

        ReservationCreatedResponse response = reservationService.createReservation(request);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Data de entrega deve ser posterior à data de retirada");
        verify(customerUseCase, never()).findByDocument(anyString());
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ReservationCreatedEvent.class));
    }

    @Test
    void shouldRejectReservationWhenCustomerDoesNotExist() {
        UUID sessionId = UUID.randomUUID();
        when(carService.findCarByModel("Onix")).thenReturn(car());
        when(reservationRepository.findBySessionIdAndCarId(sessionId, null)).thenReturn(Optional.empty());
        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT))
                .thenReturn(new CustomerLookupResponse(false, null, "Cliente não encontrado"));

        ReservationCreatedResponse response = reservationService.createReservation(createRequest(sessionId));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Cliente não encontrado");
        verify(carService, never()).checkAvailabilityByCarModel(anyString());
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ReservationCreatedEvent.class));
    }

    @Test
    void shouldRejectReservationWhenCarIsUnavailable() {
        UUID sessionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(carService.findCarByModel("Onix")).thenReturn(car());
        when(reservationRepository.findBySessionIdAndCarId(sessionId, null)).thenReturn(Optional.empty());
        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT)).thenReturn(foundCustomer(customerId));
        when(carService.checkAvailabilityByCarModel("Onix")).thenReturn(false);

        ReservationCreatedResponse response = reservationService.createReservation(createRequest(sessionId));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Veículo se encontra indisponível no momento");
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ReservationCreatedEvent.class));
    }

    @Test
    void shouldCreateReservationMarkCarAsReservedAndPublishEvent() {
        UUID sessionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        CreateReservationRequest request = createRequest(sessionId);
        Car car = spy(car());
        Reservation savedReservation = mock(Reservation.class);

        when(car.getId()).thenReturn(carId);
        when(carService.findCarByModel("Onix")).thenReturn(car);
        when(reservationRepository.findBySessionIdAndCarId(sessionId, carId)).thenReturn(Optional.empty());
        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT)).thenReturn(foundCustomer(customerId));
        when(carService.checkAvailabilityByCarModel("Onix")).thenReturn(true);
        when(customerUseCase.findById(customerId)).thenReturn(foundCustomer(customerId));
        when(savedReservation.getId()).thenReturn(reservationId);
        when(savedReservation.getCustomerId()).thenReturn(customerId);
        when(savedReservation.getCar()).thenReturn(car);
        when(savedReservation.getStartDate()).thenReturn(request.startDate());
        when(savedReservation.getEndDate()).thenReturn(request.finishDate());
        when(savedReservation.getStatus()).thenReturn(ReservationStatusEnum.CREATED);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationCreatedResponse response = reservationService.createReservation(request);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Reserva criada com sucesso");
        assertThat(response.reservation().reservationId()).isEqualTo(reservationId);
        assertThat(response.reservation().status()).isEqualTo("CREATED");
        assertThat(response.reservation().carModel()).isEqualTo("Onix");
        assertThat(response.reservation().customerDocument()).isEqualTo("12345678900");
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.RESERVADO);

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository, times(1)).save(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().getStatus()).isEqualTo(ReservationStatusEnum.CREATED);

        ArgumentCaptor<ReservationCreatedEvent> eventCaptor =
                ArgumentCaptor.forClass(ReservationCreatedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new ReservationCreatedEvent(
                reservationId,
                customerId,
                carId,
                request.startDate(),
                request.finishDate()
        ));
    }

    @Test
    void shouldCancelOwnedReservationReleaseCarAndPublishEvent() {
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        Car car = reservedCar(carId);
        Reservation reservation = reservation(
                reservationId,
                customerId,
                UUID.randomUUID(),
                car,
                ReservationStatusEnum.CREATED
        );

        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT)).thenReturn(foundCustomer(customerId));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationCancelledResponse response = reservationService.cancelReservation(
                new CancelReservationRequest(reservationId, CUSTOMER_DOCUMENT)
        );

        assertThat(response.success()).isTrue();
        assertThat(response.reservationId()).isEqualTo(reservationId);
        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(response.message()).isEqualTo("Reserva cancelada com sucesso");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatusEnum.CANCELLED);
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.DISPONIVEL);
        verify(reservationRepository, times(1)).save(reservation);

        ArgumentCaptor<ReservationCancelledEvent> eventCaptor =
                ArgumentCaptor.forClass(ReservationCancelledEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new ReservationCancelledEvent(
                reservationId,
                customerId,
                carId
        ));
    }

    @Test
    void shouldRejectCancellationWhenCustomerDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT))
                .thenReturn(new CustomerLookupResponse(false, null, "Cliente não encontrado"));

        ReservationCancelledResponse response = reservationService.cancelReservation(
                new CancelReservationRequest(reservationId, CUSTOMER_DOCUMENT)
        );

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Cliente não encontrado");
        verify(reservationRepository, never()).findById(any());
        verifyNoCancellationSideEffects();
    }

    @Test
    void shouldRejectCancellationWhenReservationDoesNotExist() {
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT)).thenReturn(foundCustomer(customerId));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        ReservationCancelledResponse response = reservationService.cancelReservation(
                new CancelReservationRequest(reservationId, CUSTOMER_DOCUMENT)
        );

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Reserva não encontrada");
        verifyNoCancellationSideEffects();
    }

    @Test
    void shouldRejectCancellationWhenReservationBelongsToAnotherCustomer() {
        UUID informedCustomerId = UUID.randomUUID();
        UUID ownerCustomerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Car car = reservedCar(UUID.randomUUID());
        Reservation reservation = reservation(
                reservationId,
                ownerCustomerId,
                UUID.randomUUID(),
                car,
                ReservationStatusEnum.CREATED
        );

        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT)).thenReturn(foundCustomer(informedCustomerId));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        ReservationCancelledResponse response = reservationService.cancelReservation(
                new CancelReservationRequest(reservationId, CUSTOMER_DOCUMENT)
        );

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Reserva não pertence ao cliente informado");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatusEnum.CREATED);
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.RESERVADO);
        verifyNoCancellationSideEffects();
    }

    @Test
    void shouldRejectCancellationWhenReservationIsAlreadyCancelled() {
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Car car = reservedCar(UUID.randomUUID());
        Reservation reservation = reservation(
                reservationId,
                customerId,
                UUID.randomUUID(),
                car,
                ReservationStatusEnum.CANCELLED
        );

        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT)).thenReturn(foundCustomer(customerId));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        ReservationCancelledResponse response = reservationService.cancelReservation(
                new CancelReservationRequest(reservationId, CUSTOMER_DOCUMENT)
        );

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Reserva já está cancelada");
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.RESERVADO);
        verifyNoCancellationSideEffects();
    }

    @Test
    void shouldRejectCancellationWhenReservationIsCompleted() {
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Car car = reservedCar(UUID.randomUUID());
        Reservation reservation = reservation(
                reservationId,
                customerId,
                UUID.randomUUID(),
                car,
                ReservationStatusEnum.COMPLETED
        );

        when(customerUseCase.findByDocument(CUSTOMER_DOCUMENT)).thenReturn(foundCustomer(customerId));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        ReservationCancelledResponse response = reservationService.cancelReservation(
                new CancelReservationRequest(reservationId, CUSTOMER_DOCUMENT)
        );

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Reserva concluída não pode ser cancelada");
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.RESERVADO);
        verifyNoCancellationSideEffects();
    }

    private void verifyNoCancellationSideEffects() {
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ReservationCancelledEvent.class));
    }

    private CreateReservationRequest createRequest(UUID sessionId) {
        return new CreateReservationRequest(
                sessionId,
                CUSTOMER_DOCUMENT,
                LocalDateTime.parse("2026-08-01T10:00:00"),
                LocalDateTime.parse("2026-08-05T10:00:00"),
                "Onix"
        );
    }

    private Reservation reservation(
            UUID reservationId,
            UUID customerId,
            UUID sessionId,
            Car car,
            ReservationStatusEnum status
    ) {
        Reservation reservation = spy(new Reservation(
                car,
                customerId,
                sessionId,
                LocalDateTime.parse("2026-08-01T10:00:00"),
                LocalDateTime.parse("2026-08-05T10:00:00"),
                status
        ));
        when(reservation.getId()).thenReturn(reservationId);
        return reservation;
    }

    private CustomerLookupResponse foundCustomer(UUID customerId) {
        return new CustomerLookupResponse(
                true,
                new CustomerResponse(
                        customerId,
                        "Maria Silva",
                        "12345678900",
                        "maria@email.com",
                        "11999999999",
                        "INDIVIDUAL"
                ),
                "Usuário encontrado no sistema"
        );
    }

    private Car reservedCar(UUID carId) {
        Car car = spy(car());
        car.setStatus(StatusVeichleEnum.RESERVADO);
        when(car.getId()).thenReturn(carId);
        return car;
    }

    private Car car() {
        return new Car(
                new RentalCategory(
                        "economico",
                        "Econômico",
                        new BigDecimal("120.00"),
                        new BigDecimal("0.0500"),
                        true
                ),
                "Onix",
                "ABC-1234",
                StatusVeichleEnum.DISPONIVEL
        );
    }
}
