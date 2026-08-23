package com.helpdesk.service;

import com.helpdesk.exception.QuotaExceededException;
import com.helpdesk.model.Subscription;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.enums.SubscriptionPlan;
import com.helpdesk.model.enums.SubscriptionStatus;
import com.helpdesk.repository.SubscriptionRepository;
import com.helpdesk.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SubscriptionService {

    private static final int PERIOD_DAYS = 30;

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               TenantRepository tenantRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Subscription getOrCreateForTenant(Long tenantId) {
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createFreeForTenant(tenantId));
        return refreshPeriodIfNeeded(subscription);
    }

    private Subscription createFreeForTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));

        Subscription subscription = new Subscription();
        subscription.setTenant(tenant);
        subscription.setPlan(SubscriptionPlan.FREE);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setTicketsLimit(SubscriptionPlan.FREE.getTicketLimit());
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(PERIOD_DAYS));
        return subscriptionRepository.save(subscription);
    }

    private Subscription refreshPeriodIfNeeded(Subscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        if (subscription.getCurrentPeriodEnd() != null
                && subscription.getCurrentPeriodEnd().isBefore(now)) {
            subscription.setTicketsUsed(0);
            subscription.setCurrentPeriodStart(now);
            subscription.setCurrentPeriodEnd(now.plusDays(PERIOD_DAYS));
            return subscriptionRepository.save(subscription);
        }
        return subscription;
    }

    @Transactional
    public void activate(Long tenantId, String stripeCustomerId,
                         String stripeSubscriptionId, SubscriptionPlan plan) {
        Subscription subscription = getOrCreateRaw(tenantId);
        subscription.setStripeCustomerId(stripeCustomerId);
        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setTicketsLimit(plan.getTicketLimit());
        subscription.setTicketsUsed(0);
        subscription.setCurrentPeriodStart(LocalDateTime.now());
        subscription.setCurrentPeriodEnd(LocalDateTime.now().plusDays(PERIOD_DAYS));
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void markPastDue(String stripeCustomerId) {
        subscriptionRepository.findByStripeCustomerId(stripeCustomerId).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            subscriptionRepository.save(subscription);
        });
    }

    @Transactional
    public void registerTicketCreated(Long tenantId) {
        Subscription subscription = getOrCreateForTenant(tenantId);
        subscription.setTicketsUsed(subscription.getTicketsUsed() + 1);
        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public void assertCanCreateTicket(Long tenantId) {
        Subscription subscription = getOrCreateForTenant(tenantId);
        if (subscription.getPlan() == SubscriptionPlan.ENTERPRISE) {
            return;
        }
        if (subscription.getTicketsUsed() >= subscription.getTicketsLimit()) {
            throw new QuotaExceededException(
                    "Alcanzaste el limite de " + subscription.getTicketsLimit()
                            + " tickets/mes del plan " + subscription.getPlan()
                            + ". Mejora tu plan para crear mas tickets.");
        }
    }

    private Subscription getOrCreateRaw(Long tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .orElseGet(() -> createFreeForTenant(tenantId));
    }
}
