package com.helpdesk.service.event;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationListenerTest {

    private JavaMailSender mailSender;
    private EmailNotificationListener listener;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
        listener = new EmailNotificationListener(mailSender, "no-reply@test.local");
    }

    @Test
    void assignedEmailGoesToTheAgent() throws Exception {
        listener.onAssigned(new Notifications.Assigned(
                7L, "Impresora rota", "TecnoFix",
                "ana@tecnofix.com", "Ana Agente", "Carla Cliente"));

        MimeMessage sent = captureSent();
        org.junit.jupiter.api.Assertions.assertEquals("ana@tecnofix.com", sent.getAllRecipients()[0].toString());
        org.junit.jupiter.api.Assertions.assertTrue(sent.getSubject().contains("[Ticket #7]"));
        String body = bodyOf(sent);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Te asignaron el ticket"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Carla Cliente"));
    }

    @Test
    void resolvedEmailGoesToTheCustomerWithStatus() throws Exception {
        listener.onResolved(new Notifications.Resolved(
                3L, "No carga la web", "RESOLVED",
                "pedro@tecnofix.com", "Pedro Cliente", "Carlos Agente"));

        MimeMessage sent = captureSent();
        org.junit.jupiter.api.Assertions.assertEquals("pedro@tecnofix.com", sent.getAllRecipients()[0].toString());
        org.junit.jupiter.api.Assertions.assertTrue(sent.getSubject().contains("resuelto"));
        org.junit.jupiter.api.Assertions.assertTrue(bodyOf(sent).contains("RESOLVED"));
    }

    @Test
    void customerReplyEmailIncludesPreviewAndGoesToAgent() throws Exception {
        listener.onCustomerReplied(new Notifications.CustomerReplied(
                2L, "Internet lento", "laura@tecnofix.com", "Laura Admin",
                "Pedro Cliente", "Sigue lento, ya reinicie el router"));

        MimeMessage sent = captureSent();
        org.junit.jupiter.api.Assertions.assertEquals("laura@tecnofix.com", sent.getAllRecipients()[0].toString());
        String body = bodyOf(sent);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Nuevo mensaje del cliente")
                || sent.getSubject().contains("Nuevo mensaje"));
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("ya reinicie el router"));
    }

    /** El listener nunca debe romper el flujo: un fallo de SMTP no propaga excepciones. */
    @Test
    void smtpFailureIsSwallowedAndLogged() {
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP caido"))
                .when(mailSender).send(any(MimeMessage.class));

        listener.onAssigned(new Notifications.Assigned(
                1L, "T", "Empresa", "x@y.com", "Agente", "Cliente"));

        verify(mailSender).send(any(MimeMessage.class));
    }

    private MimeMessage captureSent() {
        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private String bodyOf(MimeMessage message) {
        try {
            Object content = message.getContent();
            if (content instanceof MimeMultipart multipart && multipart.getCount() > 0) {
                return multipart.getBodyPart(0).getContent().toString();
            }
            return content.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
