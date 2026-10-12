// src/main/java/com/pooja/farm/fresh/dto/AuthResponse.java

package com.pooja.farm.fresh.dto;

import com.pooja.farm.fresh.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// This DTO would contain information sent back after successful authentication,
// such as a JWT token and basic user info.
public class AuthResponse {
    private String token; // For JWT authentication
    private String message;
    private String userName;
    private String userEmail;
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;

    // Constructors, Getters, Setters
    public AuthResponse() {}

     public AuthResponse(String token, User user) {
        this.token = token;
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.address = user.getAddress();
    }

    public AuthResponse(String token, String message, String userName, String userEmail) {
        this.token = token;
        this.message = message;
        this.userName = userName;
        this.userEmail = userEmail;
    }

    // New constructor to handle direct User object
    public AuthResponse(String token, User user, String message) {
        this.token = token;
        this.message = message;
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.address = user.getAddress();
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "AuthResponse [token=" + token + ", message=" + message + ", userName=" + userName + ", userEmail="
                + userEmail + ", id=" + id + ", name=" + name + ", email=" + email + ", phone=" + phone + ", address=" + address + "]";
    }
}
