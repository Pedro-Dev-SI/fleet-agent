package io.github.pedrodevsi.fleetagent.customer.repository;

import io.github.pedrodevsi.fleetagent.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByDocument(String document);
}
