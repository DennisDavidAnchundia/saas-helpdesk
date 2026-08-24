package com.helpdesk.dto;

/** Usuario conectado ahora mismo al workspace (para la barra de presencia del chat). */
public record OnlineUserResponse(Long id, String fullName, String role) {
}
