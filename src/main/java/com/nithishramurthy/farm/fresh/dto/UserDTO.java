package com.nithishramurthy.farm.fresh.dto;

import com.nithishramurthy.farm.fresh.entity.User;

public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String address;
    private String phone;

    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.address = user.getAddress();
        this.phone = user.getPhone();

        
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return "UserDTO [id=" + id + ", name=" + name + ", email=" + email + ", address=" + address + ", phone=" + phone
                + "]";
    }

    
}
