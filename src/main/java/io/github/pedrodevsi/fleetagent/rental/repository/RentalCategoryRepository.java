package io.github.pedrodevsi.fleetagent.rental.repository;

import io.github.pedrodevsi.fleetagent.rental.domain.RentalCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentalCategoryRepository extends JpaRepository<RentalCategory, Long> {

    Optional<RentalCategory> findByCodeIgnoreCase(String code);

    List<RentalCategory> findByActiveTrueOrderByNameAsc();
}
