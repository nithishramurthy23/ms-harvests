package com.pooja.farm.fresh.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult; // Import BindingResult
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pooja.farm.fresh.dto.ContactFormRequest;
import com.pooja.farm.fresh.service.MailService;

import jakarta.validation.Valid;

// Remove the duplicate @RestController import
// import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final MailService mailService;

    public ContactController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping
    // Use ContactFormRequest and BindingResult for validation
    public ResponseEntity<?> submitContactForm(@Valid @RequestBody ContactFormRequest contactFormRequest, BindingResult bindingResult) {
        try {
            // Check for validation errors
            if (bindingResult.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                bindingResult.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(errors); // Return detailed validation errors
            }

            // Process the contact form (send email)
            // Ensure your MailService has a sendContactMail method that accepts ContactFormRequest
            mailService.sendContactMail(contactFormRequest);

            return ResponseEntity.ok().body("Message sent successfully!");
        } catch (Exception e) {
            // Log the error for debugging
            e.printStackTrace(); 
            return ResponseEntity.internalServerError().body("Failed to send message: " + e.getMessage());
        }
    }
}