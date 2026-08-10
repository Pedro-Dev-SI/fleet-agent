package io.github.pedrodevsi.fleetagent.rental.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "rental_category")
public class RentalCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "daily_base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyBasePrice;

    @Column(name = "insurance_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal insuranceRate;

    @Column(nullable = false)
    private boolean active;

    protected RentalCategory() {
    }

    public RentalCategory(String code, String name, BigDecimal dailyBasePrice, BigDecimal insuranceRate, boolean active) {
        this.code = code;
        this.name = name;
        this.dailyBasePrice = dailyBasePrice;
        this.insuranceRate = insuranceRate;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDailyBasePrice() {
        return dailyBasePrice;
    }

    public void setDailyBasePrice(BigDecimal dailyBasePrice) {
        this.dailyBasePrice = dailyBasePrice;
    }

    public BigDecimal getInsuranceRate() {
        return insuranceRate;
    }

    public void setInsuranceRate(BigDecimal insuranceRate) {
        this.insuranceRate = insuranceRate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
