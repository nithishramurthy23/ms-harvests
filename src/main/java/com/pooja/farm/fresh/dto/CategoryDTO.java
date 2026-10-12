package com.pooja.farm.fresh.dto;

import com.pooja.farm.fresh.entity.Category;

public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private String iconClass;

    public CategoryDTO(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.description = category.getDescription();
        this.iconClass = category.getIconClass();
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

    @Override
    public String toString() {
        return "CategoryDTO [id=" + id + ", name=" + name + ", description=" + description + ", iconClass=" + iconClass
                + "]";
    }

    
}

