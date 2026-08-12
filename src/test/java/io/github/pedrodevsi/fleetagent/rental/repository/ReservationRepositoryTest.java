package io.github.pedrodevsi.fleetagent.rental.repository;

import io.github.pedrodevsi.fleetagent.customer.domain.Customer;
import io.github.pedrodevsi.fleetagent.customer.domain.enums.CustomerType;
import io.github.pedrodevsi.fleetagent.rental.domain.Car;
import io.github.pedrodevsi.fleetagent.rental.domain.RentalCategory;
import io.github.pedrodevsi.fleetagent.rental.domain.Reservation;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.ReservationStatusEnum;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.StatusVeichleEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
class ReservationRepositoryTest {

    private static final List<ReservationStatusEnum> ACTIVE_STATUSES = List.of(
            ReservationStatusEnum.CREATED,
            ReservationStatusEnum.CONFIRMED
    );

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg17")
                    .asCompatibleSubstituteFor("postgres")
    );

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalCategoryRepository rentalCategoryRepository;

    @Autowired
    private io.github.pedrodevsi.fleetagent.customer.repository.CustomerRepository customerRepository;

    @Test
    void shouldReturnMostRecentActiveReservationAndIgnoreCancelledOne() {
        Customer customer = persistCustomer("12345678900");
        Car firstCar = persistCar("Onix Test", "TST-1001");
        Car secondCar = persistCar("Argo Test", "TST-1002");
        Car cancelledCar = persistCar("Polo Test", "TST-1003");

        reservationRepository.save(reservation(
                firstCar,
                customer,
                "2026-09-01T10:00:00",
                "2026-09-03T10:00:00",
                ReservationStatusEnum.CREATED
        ));
        Reservation expected = reservationRepository.save(reservation(
                secondCar,
                customer,
                "2026-10-01T10:00:00",
                "2026-10-03T10:00:00",
                ReservationStatusEnum.CONFIRMED
        ));
        reservationRepository.saveAndFlush(reservation(
                cancelledCar,
                customer,
                "2026-11-01T10:00:00",
                "2026-11-03T10:00:00",
                ReservationStatusEnum.CANCELLED
        ));

        Optional<Reservation> result = reservationRepository
                .findFirstByCustomerIdAndStatusInOrderByStartDateDesc(customer.getId(), ACTIVE_STATUSES);

        assertThat(result).contains(expected);
    }

    @Test
    void shouldPreventTwoActiveReservationsForSameCar() {
        Customer firstCustomer = persistCustomer("12345678901");
        Customer secondCustomer = persistCustomer("12345678902");
        Car car = persistCar("Creta Test", "TST-2001");

        reservationRepository.saveAndFlush(reservation(
                car,
                firstCustomer,
                "2026-09-01T10:00:00",
                "2026-09-03T10:00:00",
                ReservationStatusEnum.CREATED
        ));

        assertThatThrownBy(() -> reservationRepository.saveAndFlush(reservation(
                car,
                secondCustomer,
                "2026-10-01T10:00:00",
                "2026-10-03T10:00:00",
                ReservationStatusEnum.CONFIRMED
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldIgnoreCancelledReservationWhenCheckingIdempotency() {
        Customer customer = persistCustomer("12345678903");
        Car car = persistCar("Kwid Test", "TST-3001");
        UUID sessionId = UUID.randomUUID();
        LocalDateTime startDate = LocalDateTime.parse("2026-09-01T10:00:00");
        LocalDateTime endDate = LocalDateTime.parse("2026-09-03T10:00:00");
        reservationRepository.saveAndFlush(new Reservation(
                car,
                customer.getId(),
                sessionId,
                startDate,
                endDate,
                ReservationStatusEnum.CANCELLED
        ));

        Optional<Reservation> result = reservationRepository.findActiveIdempotentReservation(
                sessionId,
                customer.getId(),
                car.getId(),
                startDate,
                endDate,
                ACTIVE_STATUSES
        );

        assertThat(result).isEmpty();
    }

    private Customer persistCustomer(String document) {
        return customerRepository.saveAndFlush(new Customer(
                "Cliente Teste",
                document,
                null,
                null,
                CustomerType.INDIVIDUAL
        ));
    }

    private Car persistCar(String model, String plate) {
        RentalCategory category = rentalCategoryRepository.findByCodeIgnoreCase("economico")
                .orElseGet(() -> rentalCategoryRepository.saveAndFlush(new RentalCategory(
                        "economico",
                        "Econômico",
                        new BigDecimal("120.00"),
                        new BigDecimal("0.0500"),
                        true
                )));
        return carRepository.saveAndFlush(new Car(category, model, plate, StatusVeichleEnum.RESERVADO));
    }

    private Reservation reservation(
            Car car,
            Customer customer,
            String startDate,
            String endDate,
            ReservationStatusEnum status
    ) {
        return new Reservation(
                car,
                customer.getId(),
                UUID.randomUUID(),
                LocalDateTime.parse(startDate),
                LocalDateTime.parse(endDate),
                status
        );
    }
}
