package com.k1den.synccart_v20.models;

public class ChatInvitation {
    private int id;
    private int chatId;
    private int inviteeId;
    private String chatTitle;

    public int getId() {
        return id;
    }

    public int getChatId() {
        return chatId;
    }

    public int getInviteeId() {
        return inviteeId;
    }

    public String getChatTitle() {
        return chatTitle;
    }
}