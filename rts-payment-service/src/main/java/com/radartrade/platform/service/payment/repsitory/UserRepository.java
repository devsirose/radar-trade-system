package com.radartrade.platform.service.payment.repsitory;

import com.radartrade.platform.service.payment.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {}