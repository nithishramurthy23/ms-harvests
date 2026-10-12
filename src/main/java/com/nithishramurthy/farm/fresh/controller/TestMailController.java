package com.nithishramurthy.farm.fresh.controller;




import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nithishramurthy.farm.fresh.service.MailService;


/** Development helper that sends a fixed test email to check the mail settings. */
@RestController
@RequestMapping("/api/test/mail")
public class TestMailController {

    private final MailService mailService;

    public TestMailController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping
    public ResponseEntity<String> sendTestMail() {
        try {
            mailService.sendTestMail(
                "recipient@example.com",
                "Test Email from Farm Fresh App", 
                "This is a test message sent via Spring Boot email service."
            );
            return ResponseEntity.ok("Test email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Failed to send test email: " + e.getMessage());
        }
    }
}