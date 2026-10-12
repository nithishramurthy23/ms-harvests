package com.pooja.farm.fresh.entity;



import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;

import java.util.ArrayList;
import java.util.List;

import com.pooja.farm.fresh.entity.Product;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Column(name = "icon_class")
    private String iconClass;

    @OneToMany(mappedBy = "category")
    private List<Product> products;

     @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<DiscountRule> discountRules = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconClass() {
        return iconClass;
    }

    public void setIconClass(String iconClass) {
        this.iconClass = iconClass;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

   public List<DiscountRule> getDiscountRules() {
        return discountRules;
    }

    public void setDiscountRules(List<DiscountRule> discountRules) {
        this.discountRules = discountRules;
    }

    @Override
    public String toString() {
        return "Category [id=" + id + ", name=" + name + ", description=" + description + ", iconClass=" + iconClass
                + ", products=" + products + ", discountRules=" + discountRules + "]";
    }


    
}