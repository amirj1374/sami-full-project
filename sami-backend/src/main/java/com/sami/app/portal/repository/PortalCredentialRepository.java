package com.sami.app.portal.repository;

import com.sami.app.portal.domain.PortalCredential;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortalCredentialRepository extends JpaRepository<PortalCredential, Long> {
    @EntityGraph(attributePaths = {"method"})
    List<PortalCredential> findAllByAccountIdAndRevokedAtIsNull(Long accountId);
}
