package com.sami.app.knowledge.repository;

import com.sami.app.knowledge.domain.SopRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SopRoleRepository extends JpaRepository<SopRole, Long> {
    List<SopRole> findAllBySopIdOrderByDisplayOrderAsc(Long sopId);
    void deleteAllBySopId(Long sopId);
}
