package com.sim800l.ui;

import com.sim800l.model.ChatItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ChatListCell extends ListCell<ChatItem> {
    
    @Override
    protected void updateItem(ChatItem chat, boolean empty) {
        super.updateItem(chat, empty);
        
        if (empty || chat == null) {
            setGraphic(null);
            setText(null);
            setStyle("-fx-background-color: transparent;");
            return;
        }
        
        HBox content = new HBox(10);
        content.setPadding(new Insets(10, 12, 10, 12));
        content.setAlignment(Pos.CENTER_LEFT);
        
        // Avatar - small and minimal
        Circle avatar = new Circle(18);
        avatar.setFill(Color.web(Theme.accent()));
        
        StackPane avatarPane = new StackPane(avatar);
        String displayName = chat.getName() != null ? chat.getName() : chat.getPhoneNumber();
        Label initial = new Label(displayName.substring(0, 1).toUpperCase());
        initial.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 13));
        initial.setTextFill(Color.WHITE);
        avatarPane.getChildren().add(initial);
        
        // Chat info
        VBox chatInfo = new VBox(4);
        chatInfo.setAlignment(Pos.CENTER_LEFT);
        
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(displayName);
        nameLabel.setFont(Font.font("Inter", FontWeight.SEMI_BOLD, 13));
        nameLabel.setTextFill(Color.web(Theme.primaryText()));
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label timeLabel = new Label(chat.getLastTime());
        timeLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 11));
        timeLabel.setTextFill(Color.web(Theme.mutedText()));
        
        nameRow.getChildren().addAll(nameLabel, spacer, timeLabel);
        
        Label lastMsg = new Label(chat.getLastMessage());
        lastMsg.setFont(Font.font("Inter", FontWeight.NORMAL, 12));
        lastMsg.setTextFill(Color.web(Theme.secondaryText()));
        lastMsg.setMaxWidth(200);
        
        chatInfo.getChildren().addAll(nameRow, lastMsg);
        HBox.setHgrow(chatInfo, Priority.ALWAYS);
        
        content.getChildren().addAll(avatarPane, chatInfo);
        
        // Unread badge - minimal
        if (chat.getUnreadCount() > 0) {
            Circle badge = new Circle(8);
            badge.setFill(Color.web(Theme.error()));
            StackPane badgePane = new StackPane(badge);
            content.getChildren().add(badgePane);
        }
        
        setGraphic(content);
        setText(null);
        setStyle("-fx-background-color: transparent; -fx-padding: 2;");
        
        // Simple hover effect
        setOnMouseEntered(e -> {
            content.setStyle(
                "-fx-background-color: " + Theme.tertiaryBg() + ";" +
                "-fx-background-radius: 6;"
            );
        });
        
        setOnMouseExited(e -> {
            content.setStyle("-fx-background-color: transparent;");
        });
    }
}
