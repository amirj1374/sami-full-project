package com.sami.app.files.repository;

import com.sami.app.files.domain.RetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetentionPolicyRepository extends JpaRepository<RetentionPolicy, Long> {

    Optional<RetentionPolicy> findByCode(String code);

    Optional<RetentionPolicy> findFirstByIsDefaultTrue();

    List<RetentionPolicy> findAllByOrderByDisplayOrderAsc();
}
