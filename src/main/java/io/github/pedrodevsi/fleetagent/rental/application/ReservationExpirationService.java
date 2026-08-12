package io.github.pedrodevsi.fleetagent.rental.application;

import io.github.pedrodevsi.fleetagent.rental.domain.Reservation;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.ReservationStatusEnum;
import io.github.pedrodevsi.fleetagent.rental.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationExpirationService {

    private static final List<ReservationStatusEnum> ACTIVE_STATUSES = List.of(
            ReservationStatusEnum.CREATED,
            ReservationStatusEnum.CONFIRMED
    );

    private final ReservationRepository reservationRepository;
    private final Clock clock;

    public ReservationExpirationService(ReservationRepository reservationRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.clock = clock;
    }

    @Transactional
    public int completeExpiredReservations() {
        List<Reservation> expiredReservations = reservationRepository.findExpiredActiveForUpdate(
                LocalDateTime.now(clock),
                ACTIVE_STATUSES
        );

        int completed = 0;
        for (Reservation reservation : expiredReservations) {
            if (reservation.complete()) {
                completed++;
            }
        }

        return completed;
    }
}
