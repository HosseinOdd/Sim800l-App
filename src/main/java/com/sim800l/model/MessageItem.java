package com.sim800l.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageItem implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String text;
    private boolean outgoing;
    private LocalDateTime timestamp;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    public MessageItem(String text, boolean outgoing) {
        this.text = text;
        this.outgoing = outgoing;
        this.timestamp = LocalDateTime.now();
    }
    
    public String getText() {
        return text;
    }
    
    public boolean isOutgoing() {
        return outgoing;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getTime() {
        return timestamp.format(TIME_FORMATTER);
    }
}
