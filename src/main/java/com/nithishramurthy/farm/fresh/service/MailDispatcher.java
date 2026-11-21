package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.mail.MailConstant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends email on a background thread so a slow or failing mail server never
 * blocks or fails the HTTP request that triggered it. Retries with backoff.
 */
@Component
public class MailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MailDispatcher.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 2000;

    private final JavaMailSender javaMailSender;

    public MailDispatcher(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Async
    public void send(SimpleMailMessage message) {
        withRetries(String.join(",", message.getTo() == null ? new String[0] : message.getTo()),
                () -> javaMailSender.send(message));
    }

    /** An email with a plain-text and an HTML version; mail apps pick the one they can show. */
    @Async
    public void sendHtml(String to, String subject, String text, String html) {
        withRetries(to, () -> javaMailSender.send(mime -> {
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(MailConstant.ADMIN_EMAIL);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
        }));
    }

    private void withRetries(String to, Runnable sending) {
        long backoff = INITIAL_BACKOFF_MS;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                sending.run();
                log.info("Email sent to {}", to);
                return;
            } catch (MailException e) {
                log.warn("Email to {} failed (attempt {}/{}): {}", to, attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Giving up on email to {}", to, e);
                    return;
                }
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                backoff *= 2;
            }
        }
    }
}
