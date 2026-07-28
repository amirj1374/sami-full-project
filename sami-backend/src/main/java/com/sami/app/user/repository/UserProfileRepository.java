package com.sami.app.user.repository;

import com.sami.app.user.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data-access for {@link UserProfile}. The {@code exists...IdNot} variants back
 * the duplicate checks (phone / employee code / national code), matching the
 * partial unique indexes so violations surface as clean 409s instead of raw
 * constraint errors.
 */
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<UserProfile> findByUserId(Long userId);

    boolean existsByPhoneNumberAndUserIdNot(String phoneNumber, Long userId);

    boolean existsByEmployeeCodeAndUserIdNot(String employeeCode, Long userId);

    boolean existsByNationalCodeAndUserIdNot(String nationalCode, Long userId);
}
