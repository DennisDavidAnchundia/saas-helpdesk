package com.helpdesk.repository;

import com.helpdesk.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByTenantId(Long tenantId);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);
}
