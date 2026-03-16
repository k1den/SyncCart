package com.k1den.synccart_v20.models;

public class ListItem {
    private int id;
    private int listId;
    private String name;
    private String category; // <-- ДОБАВИЛИ
    private boolean isBought;

    private String assigneeName;

    public String getAssigneeName() {
        return assigneeName;
    }

    // ДОБАВЬ ГЕТТЕР:
    public String getCategory() {
        return category;
    }

    private Integer assignedUserId;

    public int getId() {
        return id;
    }

    public int getListId() {
        return listId;
    }

    public String getName() {
        return name;
    }

    public boolean isBought() {
        return isBought;
    } // Внимание: метод называется isBought()

    public Integer getAssignedUserId() {
        return assignedUserId;
    }
}