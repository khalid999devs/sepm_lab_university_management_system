package com.kuet.ums.repository;

import com.kuet.ums.entity.Role;
import com.kuet.ums.entity.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<UserAccount, Long> {
    List<UserAccount> findAllByRoleOrderByIdAsc(Role role);
    Optional<UserAccount> findByIdAndRole(Long id, Role role);
    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);

    // Serialize admin deletions so concurrent requests cannot remove every admin.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UserAccount a where a.role = :role order by a.id")
    List<UserAccount> lockAccountsByRole(@Param("role") Role role);
}
