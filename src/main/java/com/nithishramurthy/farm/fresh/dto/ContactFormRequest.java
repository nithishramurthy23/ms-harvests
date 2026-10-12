package com.nithishramurthy.farm.fresh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Fields submitted from the "Contact us" form. */
public class ContactFormRequest {
    @NotBlank(message = "Name cannot be empty")
    private String name;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Please enter a valid email address")
    @JsonProperty("email") 
    private String mail;

    @NotBlank(message = "Message cannot be empty")
    private String message;

    // Constructors, Getters, Setters
    public ContactFormRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}