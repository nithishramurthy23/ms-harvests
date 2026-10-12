package com.nithishramurthy.farm.fresh.service;

import com.nithishramurthy.farm.fresh.dto.ContactFormRequest;
import com.nithishramurthy.farm.fresh.entity.Order; // Import Order entity
import com.nithishramurthy.farm.fresh.entity.OrderItem; // Import OrderItem entity
import com.nithishramurthy.farm.fresh.mail.MailConstant;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter; // Import DateTimeFormatter

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

    /**
     * Sends an order confirmation email to the customer.
     * @param order The saved Order entity.
     * @param recipientEmail The customer's email address.
     * @param recipientName The customer's name.
     */
    public void sendOrderConfirmationEmail(Order order, String recipientEmail, String recipientName) {
        String subject = "Farm Fresh - Your Order #" + order.getId() + " Confirmation";

        StringBuilder body = new StringBuilder();
        body.append("Dear ").append(recipientName).append(",\n\n");
        body.append("Thank you for your order with Farm Fresh! Here are the details of your recent purchase:\n\n");
        body.append("Order ID: ").append(order.getId()).append("\n");
        body.append("Order Date: ").append(order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        body.append("Total Amount: ₹").append(order.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP)).append("\n\n");
        body.append("Shipping Address: ").append(order.getShippingAddress()).append("\n");
        body.append("Payment Method: ").append(order.getPaymentMethod()).append("\n\n");
        body.append("Order Items:\n");
        order.getOrderItems().forEach(item -> {
            body.append("- ").append(item.getProductName())
                .append(" x ").append(item.getQuantity())
                .append(" @ ₹").append(item.getProductPrice().setScale(2, BigDecimal.ROUND_HALF_UP)) // CORRECTED: Changed to getProductPrice()
                .append(" each\n");
        });
        body.append("\nWe will notify you once your order is shipped.\n\n");
        body.append("Thanks for choosing Farm Fresh!\n");
        body.append("The Farm Fresh Team");

        sendTestMail(recipientEmail, subject, body.toString());
    }
}