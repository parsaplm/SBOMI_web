package com.parsa.middleware.model;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class StatusColor {
    @Id
    private String status;
    private String color;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}