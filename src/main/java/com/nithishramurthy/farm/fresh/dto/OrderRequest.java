// src/main/java/com/nithishramurthy/farm/fresh/dto/OrderRequest.java

package com.nithishramurthy.farm.fresh.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class OrderRequest {

    @Valid
    @NotNull(message = "Shipping information is required")
    private ShippingInfo shippingInfo;

    @Valid
    @NotNull(message = "Items list cannot be null")
    @Size(min = 1, message = "Order must contain at least one item")
    private List<OrderItemDto> items;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // only "cod" is accepted until a payment gateway is added

    
    @NotBlank(message = "Order confirmation OTP is required")
    private String orderConfirmationOtp;


    // Constructors, Getters, Setters
    public OrderRequest() {}

    public ShippingInfo getShippingInfo() {
        return shippingInfo;
    }

    public void setShippingInfo(ShippingInfo shippingInfo) {
        this.shippingInfo = shippingInfo;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getOrderConfirmationOtp() {
        return orderConfirmationOtp;
    }

    public void setOrderConfirmationOtp(String orderConfirmationOtp) {
        this.orderConfirmationOtp = orderConfirmationOtp;
    }


    public static class ShippingInfo {
        @NotBlank(message = "Recipient name is required")
        private String name;

        @NotBlank(message = "Shipping address is required")
        private String address;

        @NotBlank(message = "City is required")
        private String city;

        @NotBlank(message = "Postal code is required")
        private String postalCode;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Invalid phone number format")
        private String phone;

        @NotBlank(message = "Email is required for shipping info")
        @Email(message = "Please enter a valid email address for shipping")
        private String email;

        @NotNull(message = "Expected delivery date is required")
        private LocalDate expectedDeliveryDate;

        // Constructors, Getters, Setters
        public ShippingInfo() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getPostalCode() {
            return postalCode;
        }

        public void setPostalCode(String postalCode) {
            this.postalCode = postalCode;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public LocalDate getExpectedDeliveryDate() {
            return expectedDeliveryDate;
        }

        public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
            this.expectedDeliveryDate = expectedDeliveryDate;
        }
    }

    public static class OrderItemDto {
        @NotNull(message = "Product ID is required for each item")
        private Long productId;

        @NotNull(message = "Quantity is required for each item")
        private Integer quantity;

        // Ignored when pricing the order; the server uses the database price.
        private BigDecimal price;

        // Constructors, Getters, Setters
        public OrderItemDto() {}

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }

}
