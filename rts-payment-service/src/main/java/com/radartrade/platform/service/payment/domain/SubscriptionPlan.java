package com.radartrade.platform.service.payment.domain;

import com.radartrade.platform.service.payment.domain.valueobject.BillingCycleType;
import com.radartrade.platform.service.payment.domain.valueobject.PlanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
