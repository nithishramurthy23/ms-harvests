package com.nithishramurthy.farm.fresh.dto;

import com.nithishramurthy.farm.fresh.entity.User;



/** Profile details returned by /api/auth/me and after a profile update. */
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String role; // USER, MEMBER, ADMIN or OWNER

    public UserProfileResponse() {}

    public UserProfileResponse(Long id, String name, String email, String phone, String address) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
    }

    public UserProfileResponse(User user) {
        this(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getAddress());
        this.role = user.roleEnum().name();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
        return "UserProfileResponse [id=" + id + ", name=" + name + ", email=" + email + ", phone=" + phone
                + ", address=" + address + "]";
    }

    
}

