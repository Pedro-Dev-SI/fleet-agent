package io.github.pedrodevsi.fleetagent.rental.application;

import io.github.pedrodevsi.fleetagent.rental.domain.Car;
import io.github.pedrodevsi.fleetagent.rental.domain.Reservation;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.ReservationStatusEnum;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.StatusVeichleEnum;
import io.github.pedrodevsi.fleetagent.rental.repository.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationExpirationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-06T12:00:00Z"),
            ZoneOffset.UTC
    );

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ReservationExpirationService service = new ReservationExpirationService(
            reservationRepository,
            CLOCK
    );

    @Test
    void shouldCompleteExpiredActiveReservations() {
        Car car = new Car(null, "Onix", "ABC-1234", StatusVeichleEnum.RESERVADO);
        Reservation reservation = new Reservation(
                car,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.parse("2026-08-01T10:00:00"),
                LocalDateTime.parse("2026-08-05T10:00:00"),
                ReservationStatusEnum.CREATED
        );
        LocalDateTime now = LocalDateTime.now(CLOCK);
        List<ReservationStatusEnum> activeStatuses = List.of(
                ReservationStatusEnum.CREATED,
                ReservationStatusEnum.CONFIRMED
        );
        when(reservationRepository.findExpiredActiveForUpdate(now, activeStatuses))
                .thenReturn(List.of(reservation));

        int completed = service.completeExpiredReservations();

        assertThat(completed).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatusEnum.COMPLETED);
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.DISPONIVEL);
        verify(reservationRepository).findExpiredActiveForUpdate(now, activeStatuses);
    }
}
