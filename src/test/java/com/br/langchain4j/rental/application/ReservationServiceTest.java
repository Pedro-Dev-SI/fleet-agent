package com.br.langchain4j.rental.application;

import com.br.langchain4j.customer.api.CustomerUseCase;
import com.br.langchain4j.customer.api.CustomerLookupResponse;
import com.br.langchain4j.customer.api.CustomerResponse;
import com.br.langchain4j.rental.api.event.ReservationCreatedEvent;
import com.br.langchain4j.rental.domain.Car;
import com.br.langchain4j.rental.domain.RentalCategory;
import com.br.langchain4j.rental.domain.Reservation;
import com.br.langchain4j.rental.domain.enums.StatusVeichleEnum;
import com.br.langchain4j.rental.api.CreateReservationRequest;
import com.br.langchain4j.rental.api.ReservationCompletedResponse;
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
        Reservation existingReservation = new Reservation(
                car,
                customerId,
                sessionId,
                LocalDateTime.parse("2026-08-01T10:00:00"),
                LocalDateTime.parse("2026-08-05T10:00:00")
        );

        when(carService.findCarByModel("Onix")).thenReturn(car);
        when(reservationRepository.findBySessionIdAndCarId(sessionId, null)).thenReturn(Optional.of(existingReservation));
        when(customerUseCase.findById(customerId)).thenReturn(foundCustomer(customerId));

        ReservationCompletedResponse response = reservationService.createReservation(request(sessionId));

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Reserva já existe para esta sessão e veículo");
        assertThat(response.reservation().customerName()).isEqualTo("Maria Silva");
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldRejectReservationWhenEndDateIsNotAfterStartDate() {
        UUID sessionId = UUID.randomUUID();
        when(carService.findCarByModel("Onix")).thenReturn(car());
        when(reservationRepository.findBySessionIdAndCarId(sessionId, null)).thenReturn(Optional.empty());

        CreateReservationRequest request = new CreateReservationRequest(
                sessionId,
                "123.456.789-00",
                LocalDateTime.parse("2026-08-01T10:00:00"),
                LocalDateTime.parse("2026-08-01T10:00:00"),
                "Onix"
        );

        ReservationCompletedResponse response = reservationService.createReservation(request);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Data de entrega deve ser posterior à data de retirada");
        verify(customerUseCase, never()).findByDocument(anyString());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldRejectReservationWhenCustomerDoesNotExist() {
        UUID sessionId = UUID.randomUUID();
        when(carService.findCarByModel("Onix")).thenReturn(car());
        when(reservationRepository.findBySessionIdAndCarId(sessionId, null)).thenReturn(Optional.empty());
        when(customerUseCase.findByDocument("123.456.789-00"))
                .thenReturn(new CustomerLookupResponse(false, null, "Cliente não encontrado"));

        ReservationCompletedResponse response = reservationService.createReservation(request(sessionId));

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
        when(customerUseCase.findByDocument("123.456.789-00")).thenReturn(foundCustomer(customerId));
        when(carService.checkAvailabilityByCarModel("Onix")).thenReturn(false);

        ReservationCompletedResponse response = reservationService.createReservation(request(sessionId));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Veículo se encontra indisponível no momento");
        verify(reservationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(ReservationCreatedEvent.class));
    }

    @Test
    void shouldCreateReservationAndMarkCarAsReserved() {
        UUID sessionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID carId = UUID.randomUUID();
        CreateReservationRequest request = request(sessionId);
        Car car = spy(car());
        Reservation savedReservation = mock(Reservation.class);

        when(car.getId()).thenReturn(carId);
        when(carService.findCarByModel("Onix")).thenReturn(car);
        when(reservationRepository.findBySessionIdAndCarId(sessionId, carId)).thenReturn(Optional.empty());
        when(customerUseCase.findByDocument("123.456.789-00")).thenReturn(foundCustomer(customerId));
        when(carService.checkAvailabilityByCarModel("Onix")).thenReturn(true);
        when(customerUseCase.findById(customerId)).thenReturn(foundCustomer(customerId));
        when(savedReservation.getId()).thenReturn(reservationId);
        when(savedReservation.getCustomerId()).thenReturn(customerId);
        when(savedReservation.getCar()).thenReturn(car);
        when(savedReservation.getStartDate()).thenReturn(request.startDate());
        when(savedReservation.getEndDate()).thenReturn(request.finishDate());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationCompletedResponse response = reservationService.createReservation(request);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Reserva realizada com sucesso");
        assertThat(response.reservation().carModel()).isEqualTo("Onix");
        assertThat(response.reservation().customerDocument()).isEqualTo("12345678900");
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.RESERVADO);
        verify(reservationRepository, times(1)).save(any(Reservation.class));

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

    private CreateReservationRequest request(UUID sessionId) {
        return new CreateReservationRequest(
                sessionId,
                "123.456.789-00",
                LocalDateTime.parse("2026-08-01T10:00:00"),
                LocalDateTime.parse("2026-08-05T10:00:00"),
                "Onix"
        );
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
