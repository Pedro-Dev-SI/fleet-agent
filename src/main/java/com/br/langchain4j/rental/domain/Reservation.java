package com.br.langchain4j.rental.domain;

import com.br.langchain4j.rental.domain.enums.ReservationStatusEnum;
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

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reservation")
public class Reservation {

    public Reservation(){}

    public Reservation(Car car, UUID customerId, UUID sessionId, LocalDateTime startDate, LocalDateTime endDate, ReservationStatusEnum status) {
        this.car = car;
        this.customerId = customerId;
        this.sessionId = sessionId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatusEnum status;


    public UUID getId() {
        return id;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public ReservationStatusEnum getStatus() {
        return status;
    }

    public void cancel() {
        if (status == ReservationStatusEnum.CANCELLED) {
            throw new IllegalStateException("Reserva já está cancelada");
        }

        if (status == ReservationStatusEnum.COMPLETED) {
            throw new IllegalStateException("Reserva concluída não pode ser cancelada");
        }

        if (status != ReservationStatusEnum.CREATED && status != ReservationStatusEnum.CONFIRMED) {
            throw new IllegalStateException("Reserva não pode ser cancelada no status atual");
        }

        this.status = ReservationStatusEnum.CANCELLED;
    }
}
