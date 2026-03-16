package com.k1den.synccart_v20.models;

public class Message {
    private int id;
    private int chatId;
    private int userId;
    private String content;
    private String messageType;

    public int getId() {
        return id;
    }

    public int getChatId() {
        return chatId;
    }

    public int getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }
}