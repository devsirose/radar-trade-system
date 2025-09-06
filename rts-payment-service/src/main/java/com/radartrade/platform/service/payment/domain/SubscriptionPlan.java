package com.radartrade.platform.service.payment.domain;

import com.radartrade.platform.service.payment.domain.valueobject.BillingCycleType;
import com.radartrade.platform.service.payment.domain.valueobject.PlanStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "billing_cycle_type")
    private BillingCycleType billingCycle;

    private Integer trialDays = 0;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "plan_status")
    private PlanStatus status = PlanStatus.ACTIVE;

    private Instant createdAt = Instant.now();

    // ================= Constructors =================
    public SubscriptionPlan() {
    }

    public SubscriptionPlan(UUID id, String name, String description, BigDecimal price, String currency,
                            BillingCycleType billingCycle, Integer trialDays, PlanStatus status, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.billingCycle = billingCycle;
        this.trialDays = trialDays;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ================= Getters & Setters =================
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BillingCycleType getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycleType billingCycle) {
        this.billingCycle = billingCycle;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(Integer trialDays) {
        this.trialDays = trialDays;
    }

    public PlanStatus getStatus() {
        return status;
    }

    public void setStatus(PlanStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
