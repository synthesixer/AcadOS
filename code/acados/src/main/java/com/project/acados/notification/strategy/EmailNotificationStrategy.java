package com.project.acados.notification.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email channel: sends a plain-text email through JavaMailSender (Mailtrap Sandbox SMTP).
 * A failed email is logged and not rethrown, so it never rolls back the caller's work.
 * Reference: Implement_Plan-AcadOS.md §14.6
 */
@Slf4j
@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotificationStrategy(JavaMailSender mailSender,
                                     @Value("${acados.mail.from:no-reply@acados.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(fromAddress);
        mail.setTo(message.recipient().getEmail());
        mail.setSubject(message.title());
        mail.setText(message.message());
        try {
            mailSender.send(mail);
        } catch (MailException e) {
            log.warn("Could not send {} email to user {}: {}",
                    message.type(), message.recipient().getId(), e.getMessage());
        }
    }
}
