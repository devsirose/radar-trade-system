package com.radartrade.platform.service.payment.repository;

import com.radartrade.platform.service.payment.domain.PaymentMethod;
import com.radartrade.platform.service.payment.domain.valueobject.PaymentMethodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {
    List<PaymentMethod> findByUser(UUID userId);

    Optional<PaymentMethod> findByUserAndType(UUID userId, PaymentMethodType type);
}

