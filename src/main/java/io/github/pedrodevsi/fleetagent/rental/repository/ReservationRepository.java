package io.github.pedrodevsi.fleetagent.rental.repository;

import io.github.pedrodevsi.fleetagent.rental.domain.Reservation;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.ReservationStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("""
            select reservation
            from Reservation reservation
            where reservation.sessionId = :sessionId
              and reservation.customerId = :customerId
              and reservation.car.id = :carId
              and reservation.startDate = :startDate
              and reservation.endDate = :endDate
              and reservation.status in :statuses
            """)
    Optional<Reservation> findActiveIdempotentReservation(
            @Param("sessionId") UUID sessionId,
            @Param("customerId") UUID customerId,
            @Param("carId") UUID carId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("statuses") Collection<ReservationStatusEnum> statuses
    );

    Optional<Reservation> findFirstByCustomerIdAndStatusInOrderByStartDateDesc(
            UUID customerId,
            Collection<ReservationStatusEnum> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select reservation from Reservation reservation join fetch reservation.car where reservation.id = :id")
    Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reservation
            from Reservation reservation
            join fetch reservation.car
            where reservation.endDate <= :now
              and reservation.status in :statuses
            """)
    List<Reservation> findExpiredActiveForUpdate(
            @Param("now") LocalDateTime now,
            @Param("statuses") Collection<ReservationStatusEnum> statuses
    );
}
