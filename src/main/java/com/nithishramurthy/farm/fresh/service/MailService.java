package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.ContactFormRequest;
import com.nithishramurthy.farm.fresh.mail.MailConstant;
import com.nithishramurthy.farm.fresh.notification.OrderEmail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

/** Builds the app's emails (OTP, contact form, order confirmation) and hands them to MailDispatcher. */
@Service
public class MailService {

    private final MailDispatcher mailDispatcher;

    public MailService(MailDispatcher mailDispatcher) {
        this.mailDispatcher = mailDispatcher;
    }

    /**
     * Queues a simple text email. Sending happens in the background (see MailDispatcher).
     * @param to Recipient email address.
     * @param subject Email subject.
     * @param body Email body content.
     */
    public void sendTestMail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(MailConstant.ADMIN_EMAIL);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailDispatcher.send(message);
    }

    /**
     * Sends a contact form submission email to the admin.
     * @param request The ContactFormRequest DTO.
     */
    public void sendContactMail(ContactFormRequest request) {
        String subject = "New Contact Form Submission from " + request.getName();
        String body = String.format(
            "Name: %s\nEmail: %s\nMessage: %s",
            request.getName(),
            request.getMail(), // Assuming getMail() is the correct getter for email from ContactFormRequest
            request.getMessage()
        );
        sendTestMail(MailConstant.ADMIN_EMAIL, subject, body);
    }

    /**
     * Sends an OTP email to the specified recipient.
     * @param toEmail The email address to send the OTP to.
     * @param otp The One-Time Password to send.
     */
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(MailConstant.ADMIN_EMAIL);
        message.setTo(toEmail);
        message.setSubject("Farm Fresh Account Verification OTP");
        message.setText("Dear User,\n\nYour One-Time Password (OTP) for Farm Fresh account verification is: " + otp + "\n\nThis OTP is valid for 5 minutes. Do not share it with anyone.\n\nThank you,\nFarm Fresh Team");
        mailDispatcher.send(message);
    }

    /** Queues an order email (plain text plus HTML). Built by OrderEmailTemplates. */
    public void sendOrderEmail(OrderEmail email) {
        mailDispatcher.sendHtml(email.to(), email.subject(), email.text(), email.html());
    }
}
