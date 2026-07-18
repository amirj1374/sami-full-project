package com.sami.app.user.repository;

import com.sami.app.user.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data-access for {@link UserStatus}. The flagged rows are DB-unique (partial indexes). */
public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    List<UserStatus> findAllByOrderByDisplayOrderAsc();

    Optional<UserStatus> findByIsDefaultTrue();

    Optional<UserStatus> findByIsArchivedStateTrue();

    Optional<UserStatus> findByIsDeletedStateTrue();

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
