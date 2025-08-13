package com.radartrade.platform.service.payment.repsitory;

import com.radartrade.platform.service.payment.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
}
