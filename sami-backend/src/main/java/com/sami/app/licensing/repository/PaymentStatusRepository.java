package com.sami.app.licensing.repository;

import com.sami.app.licensing.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentStatusRepository extends JpaRepository<PaymentStatus, Long> {

    List<PaymentStatus> findAllByOrderByDisplayOrderAsc();

    Optional<PaymentStatus> findByCode(String code);

    Optional<PaymentStatus> findByIsDefaultTrue();
}
