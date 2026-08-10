package io.github.pedrodevsi.fleetagent.customer.domain;

import io.github.pedrodevsi.fleetagent.customer.domain.enums.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class Customer {

    protected Customer() {
    }

    public Customer(String name, String document, String email, String phone, CustomerType type) {
        this.name = name;
        this.document = document;
        this.email = email;
        this.phone = phone;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String document;

    @Column(length = 160)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private CustomerType type;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public CustomerType getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
