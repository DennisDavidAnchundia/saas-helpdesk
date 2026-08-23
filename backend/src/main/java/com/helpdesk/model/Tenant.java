package com.helpdesk.model;

import com.helpdesk.model.enums.SubscriptionPlan;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.FREE;

    // SLA de resolucion configurable (horas maximas por prioridad)
    @Column(name = "sla_urgent_hours", nullable = false)
    private int slaUrgentHours = 4;

    @Column(name = "sla_high_hours", nullable = false)
    private int slaHighHours = 8;

    @Column(name = "sla_medium_hours", nullable = false)
    private int slaMediumHours = 24;

    @Column(name = "sla_low_hours", nullable = false)
    private int slaLowHours = 72;

    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Tenant() {}

    public Tenant(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public SubscriptionPlan getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public int getSlaUrgentHours() { return slaUrgentHours; }
    public void setSlaUrgentHours(int slaUrgentHours) { this.slaUrgentHours = slaUrgentHours; }

    public int getSlaHighHours() { return slaHighHours; }
    public void setSlaHighHours(int slaHighHours) { this.slaHighHours = slaHighHours; }

    public int getSlaMediumHours() { return slaMediumHours; }
    public void setSlaMediumHours(int slaMediumHours) { this.slaMediumHours = slaMediumHours; }

    public int getSlaLowHours() { return slaLowHours; }
    public void setSlaLowHours(int slaLowHours) { this.slaLowHours = slaLowHours; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
