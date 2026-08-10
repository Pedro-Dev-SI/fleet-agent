package io.github.pedrodevsi.fleetagent.rental.repository;

import io.github.pedrodevsi.fleetagent.rental.domain.Car;
import io.github.pedrodevsi.fleetagent.rental.domain.enums.StatusVeichleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CarRepository extends JpaRepository<Car, UUID> {

    List<Car> findAllByCategoryCodeAndStatus(String code, StatusVeichleEnum status);

    Boolean existsByModelAndStatus(String model, StatusVeichleEnum status);

    Optional<Car> findByModel(String model);
}
