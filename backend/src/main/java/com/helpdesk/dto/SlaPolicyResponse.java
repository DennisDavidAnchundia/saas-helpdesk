package com.helpdesk.dto;

import com.helpdesk.model.Tenant;

/** Politica SLA de resolucion del tenant (horas maximas por prioridad). */
public class SlaPolicyResponse {

    private int urgentHours;
    private int highHours;
    private int mediumHours;
    private int lowHours;

    public SlaPolicyResponse() {}

    public static SlaPolicyResponse from(Tenant t) {
        SlaPolicyResponse r = new SlaPolicyResponse();
        r.urgentHours = t.getSlaUrgentHours();
        r.highHours = t.getSlaHighHours();
        r.mediumHours = t.getSlaMediumHours();
        r.lowHours = t.getSlaLowHours();
        return r;
    }

    public int getUrgentHours() { return urgentHours; }
    public void setUrgentHours(int urgentHours) { this.urgentHours = urgentHours; }

    public int getHighHours() { return highHours; }
    public void setHighHours(int highHours) { this.highHours = highHours; }

    public int getMediumHours() { return mediumHours; }
    public void setMediumHours(int mediumHours) { this.mediumHours = mediumHours; }

    public int getLowHours() { return lowHours; }
    public void setLowHours(int lowHours) { this.lowHours = lowHours; }
}
