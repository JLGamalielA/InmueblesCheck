package com.example.inmueblecheck;

public class User {
    private String uid;
    private String email;
    private String role;


    public User() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public String toString() {
        return email != null ? email : "Usuario";
    }
}