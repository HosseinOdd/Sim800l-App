package com.sim800l.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChatItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String phoneNumber;
    private String name;
    private List<MessageItem> messages;
    private int unreadCount;
    
    public ChatItem(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.name = phoneNumber;
        this.messages = new ArrayList<>();
        this.unreadCount = 0;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<MessageItem> getMessages() {
        return messages;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int count) {
        this.unreadCount = count;
    }
    
    public void incrementUnread() {
        this.unreadCount++;
    }
    
    public void addMessage(MessageItem message) {
        messages.add(message);
    }
    
    public String getLastMessage() {
        if (messages.isEmpty()) {
            return "Start a new conversation";
        }
        MessageItem last = messages.get(messages.size() - 1);
        String prefix = last.isOutgoing() ? "You: " : "";
        String text = last.getText();
        return prefix + (text.length() > 40 ? text.substring(0, 40) + "..." : text);
    }
    
    public String getLastTime() {
        if (messages.isEmpty()) {
            return "";
        }
        return messages.get(messages.size() - 1).getTime();
    }
}
