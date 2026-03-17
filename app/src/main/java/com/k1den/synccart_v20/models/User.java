package com.k1den.synccart_v20.models;

public class User {
    private int id;
    private String email;
    private String username;
    private String avatarColor;
    private String city;

    public User() {
    }

    public User(int id, String email, String username, String avatarColor, String city) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.avatarColor = avatarColor;
        this.city = city;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarColor() {
        return avatarColor;
    }

    public void setAvatarColor(String avatarColor) {
        this.avatarColor = avatarColor;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
