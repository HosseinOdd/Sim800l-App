package com.sim800l.model;

import java.io.Serializable;

public class Contact implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String phoneNumber;
    private String name;
    private String email;
    private String notes;
    
    public Contact(String phoneNumber, String name) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        // Limit lengths to prevent memory issues
        this.phoneNumber = phoneNumber.length() > 50 ? phoneNumber.substring(0, 50) : phoneNumber;
        this.name = name.length() > 100 ? name.substring(0, 100) : name;
        this.email = "";
        this.notes = "";
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        this.phoneNumber = phoneNumber.length() > 50 ? phoneNumber.substring(0, 50) : phoneNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name.length() > 100 ? name.substring(0, 100) : name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        if (email == null) {
            email = "";
        }
        // Limit email length
        this.email = email.length() > 100 ? email.substring(0, 100) : email;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        if (notes == null) {
            notes = "";
        }
        // Limit notes length to prevent memory issues
        this.notes = notes.length() > 1000 ? notes.substring(0, 1000) : notes;
    }
    
    @Override
    public String toString() {
        return name + " (" + phoneNumber + ")";
    }
}
