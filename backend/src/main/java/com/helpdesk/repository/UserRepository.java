package com.helpdesk.repository;

import com.helpdesk.model.User;
import com.helpdesk.model.enums.Provider;
import com.helpdesk.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTenantIdAndEmail(Long tenantId, String email);

    boolean existsByTenantIdAndEmail(Long tenantId, String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    List<User> findByTenantIdAndRole(Long tenantId, Role role);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    List<User> findByTenantIdOrderByIdAsc(Long tenantId);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.role = 'AGENT' AND u.isActive = true")
    List<User> findActiveAgentsByTenant(@Param("tenantId") Long tenantId);
}
