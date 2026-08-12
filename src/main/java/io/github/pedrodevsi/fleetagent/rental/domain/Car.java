package io.github.pedrodevsi.fleetagent.rental.domain;

import io.github.pedrodevsi.fleetagent.rental.domain.enums.StatusVeichleEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "car")
public class Car {

    public Car(){}

    public Car(RentalCategory category, String model, String plate, StatusVeichleEnum status) {
        this.category = category;
        this.model = model;
        this.plate = plate;
        this.status = status;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne()
    @JoinColumn(name = "category_id")
    private RentalCategory category;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String plate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusVeichleEnum status;

    public UUID getId() {
        return id;
    }

    public RentalCategory getCategory() {
        return category;
    }

    public void setCategory(RentalCategory category) {
        this.category = category;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public StatusVeichleEnum getStatus() {
        return status;
    }

    public void setStatus(StatusVeichleEnum status) {
        this.status = status;
    }

    public void reserve() {
        if (status != StatusVeichleEnum.DISPONIVEL) {
            throw new IllegalStateException("Veículo se encontra indisponível no momento");
        }

        this.status = StatusVeichleEnum.RESERVADO;
    }

    public void release() {
        this.status = StatusVeichleEnum.DISPONIVEL;
    }
}
