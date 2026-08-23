package com.helpdesk.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Todos los campos opcionales: solo se actualizan los que vienen. */
public class UpdateSlaPolicyRequest {

    @Min(value = 1, message = "Las horas deben ser al menos 1")
    @Max(value = 720, message = "Las horas no pueden superar 720 (30 dias)")
    private Integer urgentHours;

    @Min(value = 1, message = "Las horas deben ser al menos 1")
    @Max(value = 720, message = "Las horas no pueden superar 720 (30 dias)")
    private Integer highHours;

    @Min(value = 1, message = "Las horas deben ser al menos 1")
    @Max(value = 720, message = "Las horas no pueden superar 720 (30 dias)")
    private Integer mediumHours;

    @Min(value = 1, message = "Las horas deben ser al menos 1")
    @Max(value = 720, message = "Las horas no pueden superar 720 (30 dias)")
    private Integer lowHours;

    public Integer getUrgentHours() { return urgentHours; }
    public void setUrgentHours(Integer urgentHours) { this.urgentHours = urgentHours; }

    public Integer getHighHours() { return highHours; }
    public void setHighHours(Integer highHours) { this.highHours = highHours; }

    public Integer getMediumHours() { return mediumHours; }
    public void setMediumHours(Integer mediumHours) { this.mediumHours = mediumHours; }

    public Integer getLowHours() { return lowHours; }
    public void setLowHours(Integer lowHours) { this.lowHours = lowHours; }
}
