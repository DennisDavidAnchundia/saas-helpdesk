package com.helpdesk.service.event;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envia los emails de notificacion. Solo existe si app.mail.enabled=true;
 * en dev se usa Mailpit (SMTP :1025, UI http://localhost:8025).
 * Nunca rompe el flujo principal: si el envio falla, solo loguea.
 */
@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class EmailNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailNotificationListener(JavaMailSender mailSender,
                                     @Value("${spring.mail.from:helpdesk@localhost}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Async
    @TransactionalEventListener
    public void onAssigned(Notifications.Assigned event) {
        String body = """
                Hola %s:

                Te asignaron el ticket #%d "%s" de %s (%s).

                Entra al panel para atenderlo.
                """.formatted(event.agentName(), event.ticketId(), event.title(),
                event.customerName(), event.tenantName());
        send(event.agentEmail(), "[Ticket #%d] Te asignaron un caso".formatted(event.ticketId()), body);
    }

    @Async
    @TransactionalEventListener
    public void onResolved(Notifications.Resolved event) {
        String body = """
                Hola %s:

                Tu ticket #%d "%s" fue marcado como %s%s.

                Si consideras que el problema persiste, puedes reabrirlo desde el portal.
                """.formatted(event.customerName(), event.ticketId(), event.title(),
                event.status(),
                event.agentName() != null ? " por %s".formatted(event.agentName()) : "");
        send(event.customerEmail(), "[Ticket #%d] %s".formatted(event.ticketId(),
                "RESOLVED".equals(event.status()) ? "Tu caso fue resuelto" : "Tu caso fue cerrado"), body);
    }

    @Async
    @TransactionalEventListener
    public void onCustomerReplied(Notifications.CustomerReplied event) {
        String body = """
                Hola %s:

                %s escribio en el chat del ticket #%d "%s":

                "%s"

                Responde desde el panel de chat.
                """.formatted(event.agentName(), event.customerName(), event.ticketId(),
                event.title(), event.preview());
        send(event.agentEmail(), "[Ticket #%d] Nuevo mensaje del cliente".formatted(event.ticketId()), body);
    }

    private void send(String to, String subject, String textBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody);
            mailSender.send(message);
            log.info("Email de notificacion enviado a {}: {}", to, subject);
        } catch (Exception e) {
            log.warn("No se pudo enviar email a {}: {}", to, e.getMessage());
        }
    }
}
