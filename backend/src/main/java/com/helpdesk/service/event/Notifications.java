package com.helpdesk.service.event;

/**
 * Eventos de dominio que disparan notificaciones. Llevan datos planos
 * (nada de entidades) para que el listener async no toque la sesion de Hibernate.
 */
public final class Notifications {

    private Notifications() {}

    /** Un ticket fue asignado a un agente. */
    public record Assigned(Long ticketId, String title, String tenantName,
                           String agentEmail, String agentName, String customerName) {}

    /** El ticket cambio a RESOLVED o CLOSED: se avisa al cliente. */
    public record Resolved(Long ticketId, String title, String status,
                           String customerEmail, String customerName, String agentName) {}

    /** El cliente escribio en el chat: se avisa al agente asignado. */
    public record CustomerReplied(Long ticketId, String title,
                                  String agentEmail, String agentName,
                                  String customerName, String preview) {}
}
