package com.helpdesk.service;

import com.helpdesk.dto.SlaPolicyResponse;
import com.helpdesk.dto.UpdateSlaPolicyRequest;
import com.helpdesk.model.Tenant;
import com.helpdesk.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public SlaPolicyResponse getSlaPolicy(Long tenantId) {
        return SlaPolicyResponse.from(getTenant(tenantId));
    }

    @Transactional
    public SlaPolicyResponse updateSlaPolicy(Long tenantId, UpdateSlaPolicyRequest request) {
        Tenant tenant = getTenant(tenantId);
        if (request.getUrgentHours() != null) {
            tenant.setSlaUrgentHours(request.getUrgentHours());
        }
        if (request.getHighHours() != null) {
            tenant.setSlaHighHours(request.getHighHours());
        }
        if (request.getMediumHours() != null) {
            tenant.setSlaMediumHours(request.getMediumHours());
        }
        if (request.getLowHours() != null) {
            tenant.setSlaLowHours(request.getLowHours());
        }
        return SlaPolicyResponse.from(tenantRepository.save(tenant));
    }

    private Tenant getTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa no encontrada"));
    }
}
