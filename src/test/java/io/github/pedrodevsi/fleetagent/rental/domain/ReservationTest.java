package io.github.pedrodevsi.fleetagent.rental.domain;

import io.github.pedrodevsi.fleetagent.rental.domain.enums.ReservationStatusEnum;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.StatusVeichleEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    @Test
    void shouldCompleteActiveReservationAndReleaseCar() {
        Car car = new Car(null, "Onix", "ABC-1234", StatusVeichleEnum.RESERVADO);
        Reservation reservation = reservation(car, ReservationStatusEnum.CONFIRMED);

        boolean changed = reservation.complete();

        assertThat(changed).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatusEnum.COMPLETED);
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.DISPONIVEL);
    }

    @Test
    void shouldNotCompleteReservationTwice() {
        Car car = new Car(null, "Onix", "ABC-1234", StatusVeichleEnum.DISPONIVEL);
        Reservation reservation = reservation(car, ReservationStatusEnum.COMPLETED);

        boolean changed = reservation.complete();

        assertThat(changed).isFalse();
        assertThat(car.getStatus()).isEqualTo(StatusVeichleEnum.DISPONIVEL);
    }

    @Test
    void shouldNotCompleteCancelledReservation() {
        Car car = new Car(null, "Onix", "ABC-1234", StatusVeichleEnum.DISPONIVEL);
        Reservation reservation = reservation(car, ReservationStatusEnum.CANCELLED);

        assertThatThrownBy(reservation::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Reserva cancelada não pode ser concluída");
    }

    private Reservation reservation(Car car, ReservationStatusEnum status) {
        return new Reservation(
                car,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.parse("2026-08-01T10:00:00"),
                LocalDateTime.parse("2026-08-05T10:00:00"),
                status
        );
    }
}
