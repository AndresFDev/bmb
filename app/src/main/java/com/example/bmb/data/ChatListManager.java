package com.example.bmb.data;

import java.util.List;

public class ChatListManager {

    private String chatId;
    private List<String> userIds;

    public ChatListManager() {
    }

    public ChatListManager(String chatId, List<String> userIds) {
        this.chatId = chatId;
        this.userIds = userIds;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }
}