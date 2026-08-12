package io.github.pedrodevsi.fleetagent.rental.repository;

import io.github.pedrodevsi.fleetagent.rental.domain.Car;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.StatusVeichleEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarRepository extends JpaRepository<Car, UUID> {

    List<Car> findAllByCategoryCodeAndStatus(String code, StatusVeichleEnum status);

    Boolean existsByModelAndStatus(String model, StatusVeichleEnum status);

    Optional<Car> findByModel(String model);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select car from Car car where car.model = :model")
    Optional<Car> findByModelForUpdate(@Param("model") String model);
}
