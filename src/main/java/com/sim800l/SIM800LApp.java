package com.sim800l;

import com.fazecast.jSerialComm.SerialPort;
import com.sim800l.model.ChatItem;
import com.sim800l.model.MessageItem;
import com.sim800l.serial.SerialPortManager;
import com.sim800l.ui.ChatListCell;
import com.sim800l.ui.Theme;
import com.sim800l.util.NotificationManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SIM800LApp extends Application {
    private SerialPortManager serialManager;
    private ComboBox<String> portComboBox;
    private Button connectButton;
    private Button testConnectionButton;
    private Label statusLabel;
    private ListView<ChatItem> chatListView;
    private VBox messageArea;
    private ScrollPane messageScrollPane;
    private TextField messageInput;
    private Button sendButton;
    private ObservableList<ChatItem> chatItems;
    private Map<String, ChatItem> chatMap;
    private Map<String, com.sim800l.model.Contact> contacts;
    private String currentPhoneNumber;
    private Button themeButton;
    private Label chatHeaderLabel;
    private Stage logViewerStage = null;
    private javafx.animation.Timeline logUpdateTimeline = null;
    private Stage contactsManagerStage = null;
    private Stage addContactDialogStage = null;
    private Stage newMessageDialogStage = null;

    @Override
    public void start(Stage primaryStage) {
        serialManager = new SerialPortManager();
        chatItems = FXCollections.observableArrayList();
        
        // Load saved data
        chatMap = com.sim800l.util.DataManager.loadChats();
        contacts = com.sim800l.util.DataManager.loadContacts();
        
        // Populate chat list from loaded data
        chatItems.addAll(chatMap.values());
        
        primaryStage.setTitle("SMS Manager - SIM800L");
        
        // Main layout
        BorderPane root = new BorderPane();
        root.setPrefSize(1000, 700);
        
        // Left sidebar
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);
        
        // Center: Message display
        VBox centerPanel = createCenterPanel();
        root.setCenter(centerPanel);
        
        // Bottom: Input area
        HBox inputArea = createInputArea();
        root.setBottom(inputArea);
        
        Scene scene = new Scene(root);
        applyThemeToScene(scene);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Refresh ports on startup
        refreshPorts();
        
        // Setup message listener
        serialManager.setMessageReceivedCallback((phoneNumber, message) -> {
            Platform.runLater(() -> {
                addOrUpdateChat(phoneNumber);
                addMessage(phoneNumber, message, LocalDateTime.now(), false);
                if (!phoneNumber.equals(currentPhoneNumber)) {
                    NotificationManager.show("New SMS from " + phoneNumber, message);
                }
            });
        });
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(280);
        sidebar.setPadding(new Insets(15));
        sidebar.getStyleClass().add("sidebar");
        
        // Title
        Label titleLabel = new Label("SMS Manager");
        titleLabel.setFont(Font.font("Inter", FontWeight.BOLD, 18));
        titleLabel.getStyleClass().add("title-label");
        
        // Connection panel
        VBox connectionPanel = new VBox(8);
        connectionPanel.getStyleClass().add("connection-panel");
        connectionPanel.setPadding(new Insets(12));
        
        Label portLabel = new Label("Serial Port:");
        portLabel.setFont(Font.font("Inter", 12));
        portComboBox = new ComboBox<>();
        portComboBox.setMaxWidth(Double.MAX_VALUE);
        
        Button refreshButton = new Button("Refresh");
        refreshButton.setFont(Font.font("Inter", 11));
        refreshButton.setOnAction(e -> refreshPorts());
        
        connectButton = new Button("Connect");
        connectButton.setFont(Font.font("Inter", FontWeight.BOLD, 12));
        connectButton.setMaxWidth(Double.MAX_VALUE);
        connectButton.setOnAction(e -> handleConnect());
        
        testConnectionButton = new Button("Test Connection");
        testConnectionButton.setFont(Font.font("Inter", 11));
        testConnectionButton.setMaxWidth(Double.MAX_VALUE);
        testConnectionButton.setDisable(true);
        testConnectionButton.setOnAction(e -> testConnection());
        
        statusLabel = new Label("Disconnected");
        statusLabel.setFont(Font.font("Inter", 11));
        statusLabel.getStyleClass().add("status-label");
        
        connectionPanel.getChildren().addAll(
            portLabel, portComboBox, refreshButton,
            connectButton, testConnectionButton, statusLabel
        );
        
        // Theme button with TEXT instead of emoji
        themeButton = new Button(Theme.isDark() ? "Light" : "Dark");
        themeButton.setFont(Font.font("Inter", FontWeight.NORMAL, 11));
        themeButton.setMaxWidth(Double.MAX_VALUE);
        themeButton.setOnAction(e -> {
            Theme.toggle();
            themeButton.setText(Theme.isDark() ? "Light" : "Dark");
            applyThemeToScene(themeButton.getScene());
        });
        
        // Log viewer button
        Button logButton = new Button("View Logs");
        logButton.setFont(Font.font("Inter", FontWeight.NORMAL, 11));
        logButton.setMaxWidth(Double.MAX_VALUE);
        logButton.setOnAction(e -> showLogViewer());
        
        // Contacts button
        Button contactsButton = new Button("Contacts");
        contactsButton.setFont(Font.font("Inter", FontWeight.NORMAL, 11));
        contactsButton.setMaxWidth(Double.MAX_VALUE);
        contactsButton.setOnAction(e -> showContactsManager());
        
        // New Message button
        Button newMessageButton = new Button("New Message");
        newMessageButton.setFont(Font.font("Inter", FontWeight.BOLD, 11));
        newMessageButton.setMaxWidth(Double.MAX_VALUE);
        newMessageButton.setOnAction(e -> showNewMessageDialog());
        
        // Chat list
        Label chatsLabel = new Label("Chats");
        chatsLabel.setFont(Font.font("Inter", FontWeight.BOLD, 14));
        
        chatListView = new ListView<>(chatItems);
        chatListView.setCellFactory(param -> new ChatListCell());
        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadChat(newVal.getPhoneNumber());
            }
        });
        
        // Context menu for chat list
        chatListView.setOnContextMenuRequested(event -> {
            ChatItem selectedChat = chatListView.getSelectionModel().getSelectedItem();
            if (selectedChat != null) {
                ContextMenu contextMenu = new ContextMenu();
                
                MenuItem editContactItem = new MenuItem("Edit Contact");
                editContactItem.setOnAction(e -> showAddContactDialog(selectedChat.getPhoneNumber()));
                
                MenuItem deleteChatItem = new MenuItem("Delete Chat");
                deleteChatItem.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Chat");
                    confirm.setHeaderText("Delete chat with " + getDisplayName(selectedChat.getPhoneNumber()) + "?");
                    confirm.setContentText("This will delete all messages in this chat.");
                    applyThemeToScene(confirm.getDialogPane().getScene());
                    
                    if (confirm.showAndWait().get() == ButtonType.OK) {
                        chatMap.remove(selectedChat.getPhoneNumber());
                        chatItems.remove(selectedChat);
                        com.sim800l.util.DataManager.saveChats(chatMap);
                        if (selectedChat.getPhoneNumber().equals(currentPhoneNumber)) {
                            currentPhoneNumber = null;
                            messageArea.getChildren().clear();
                            sendButton.setDisable(true);
                            chatHeaderLabel.setText("Select a chat");
                        }
                    }
                });
                
                contextMenu.getItems().addAll(editContactItem, deleteChatItem);
                contextMenu.show(chatListView, event.getScreenX(), event.getScreenY());
            }
        });
        
        VBox.setVgrow(chatListView, Priority.ALWAYS);
        
        sidebar.getChildren().addAll(
            titleLabel,
            connectionPanel,
            themeButton,
            logButton,
            contactsButton,
            newMessageButton,
            chatsLabel,
            chatListView
        );
        
        return sidebar;
    }

    private VBox createCenterPanel() {
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(20));
        centerPanel.getStyleClass().add("center-panel");
        
        chatHeaderLabel = new Label("Select a chat");
        chatHeaderLabel.setFont(Font.font("Inter", FontWeight.BOLD, 16));
        chatHeaderLabel.getStyleClass().add("chat-title");
        
        messageArea = new VBox(10);
        messageArea.setPadding(new Insets(10));
        messageArea.getStyleClass().add("message-area");
        
        messageScrollPane = new ScrollPane(messageArea);
        messageScrollPane.setFitToWidth(true);
        messageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(messageScrollPane, Priority.ALWAYS);
        
        centerPanel.getChildren().addAll(chatHeaderLabel, messageScrollPane);
        
        return centerPanel;
    }

    private HBox createInputArea() {
        HBox inputArea = new HBox(10);
        inputArea.setPadding(new Insets(15, 20, 15, 20));
        inputArea.setAlignment(Pos.CENTER);
        inputArea.getStyleClass().add("input-area");
        
        messageInput = new TextField();
        messageInput.setPromptText("Type a message...");
        messageInput.setFont(Font.font("Inter", 13));
        messageInput.setStyle("-fx-text-fill: " + Theme.inputText() + "; -fx-prompt-text-fill: " + Theme.mutedText() + ";");
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        messageInput.setOnAction(e -> sendMessage());
        
        sendButton = new Button("Send");
        sendButton.setFont(Font.font("Inter", FontWeight.BOLD, 13));
        sendButton.setPrefWidth(80);
        sendButton.setDisable(true);
        sendButton.setOnAction(e -> sendMessage());
        
        inputArea.getChildren().addAll(messageInput, sendButton);
        
        return inputArea;
    }

    private void refreshPorts() {
        portComboBox.getItems().clear();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            portComboBox.getItems().add(port.getSystemPortName());
        }
        if (portComboBox.getItems().size() > 0) {
            portComboBox.getSelectionModel().selectFirst();
        }
    }

    private void handleConnect() {
        if (serialManager.isConnected()) {
            serialManager.disconnect();
            connectButton.setText("Connect");
            testConnectionButton.setDisable(true);
            statusLabel.setText("Disconnected");
            statusLabel.setStyle("-fx-text-fill: #999;");
        } else {
            String selectedPort = portComboBox.getValue();
            if (selectedPort == null || selectedPort.isEmpty()) {
                showAlert("Please select a port");
                return;
            }
            
            try {
                serialManager.connect(selectedPort);
                connectButton.setText("Disconnect");
                testConnectionButton.setDisable(false);
                statusLabel.setText("Connected");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            } catch (Exception e) {
                showAlert("Connection failed: " + e.getMessage());
                statusLabel.setText("Connection failed");
                statusLabel.setStyle("-fx-text-fill: #f44336;");
            }
        }
    }

    private void testConnection() {
        try {
            boolean success = serialManager.testConnection();
            if (success) {
                showInfo("Connection test successful!");
                statusLabel.setText("Connected - Test OK");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            } else {
                showAlert("Connection test failed - No response");
                statusLabel.setText("Connected - Test Failed");
                statusLabel.setStyle("-fx-text-fill: #FFA500;");
            }
        } catch (Exception e) {
            showAlert("Test failed: " + e.getMessage());
            statusLabel.setText("Connected - Test Error");
            statusLabel.setStyle("-fx-text-fill: #f44336;");
        }
    }

    private void sendMessage() {
        if (currentPhoneNumber == null) {
            showAlert("Please select a chat first");
            return;
        }
        
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        if (!serialManager.isConnected()) {
            showAlert("Not connected to serial port");
            return;
        }
        
        try {
            serialManager.sendSMS(currentPhoneNumber, message);
            LocalDateTime timestamp = LocalDateTime.now();
            addMessage(currentPhoneNumber, message, timestamp, true);
            messageInput.clear();
            showInfo("Message sent successfully!");
        } catch (Exception e) {
            showAlert("Failed to send message: " + e.getMessage());
        }
    }

    private void addOrUpdateChat(String phoneNumber) {
        ChatItem chat = chatMap.get(phoneNumber);
        if (chat == null) {
            chat = new ChatItem(phoneNumber);
            chatMap.put(phoneNumber, chat);
            chatItems.add(0, chat);
        }
    }

    private void addMessage(String phoneNumber, String message, LocalDateTime timestamp, boolean isSent) {
        ChatItem chat = chatMap.get(phoneNumber);
        if (chat == null) {
            chat = new ChatItem(phoneNumber);
            chatMap.put(phoneNumber, chat);
            chatItems.add(0, chat);
        }
        
        MessageItem msgItem = new MessageItem(message, isSent);
        chat.addMessage(msgItem);
        
        // Move chat to top
        chatItems.remove(chat);
        chatItems.add(0, chat);
        
        // Save chats
        com.sim800l.util.DataManager.saveChats(chatMap);
        
        if (phoneNumber.equals(currentPhoneNumber)) {
            loadChat(phoneNumber);
            // Auto-scroll to bottom after message is added
            Platform.runLater(() -> {
                messageScrollPane.setVvalue(1.0);
                messageScrollPane.layout();
            });
        }
    }

    private void loadChat(String phoneNumber) {
        currentPhoneNumber = phoneNumber;
        sendButton.setDisable(false);
        messageArea.getChildren().clear();
        
        // Update header with contact name or phone number
        chatHeaderLabel.setText(getDisplayName(phoneNumber));
        
        ChatItem chat = chatMap.get(phoneNumber);
        if (chat == null) {
            return;
        }
        
        List<MessageItem> messages = chat.getMessages();
        
        for (MessageItem msg : messages) {
            VBox messageBox = new VBox(4);
            messageBox.getStyleClass().add("message-box");
            messageBox.setMaxWidth(500);
            
            // Use Label instead of TextField for better emoji support
            Label messageLabel = new Label(msg.getText());
            messageLabel.setFont(Font.font("System", 13));
            messageLabel.getStyleClass().add("message-text");
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(480);
            
            Label timeLabel = new Label(msg.getTime());
            timeLabel.setFont(Font.font("Inter", 10));
            timeLabel.getStyleClass().add("time-label");
            
            // Context menu for copy/delete
            ContextMenu contextMenu = new ContextMenu();
            MenuItem copyItem = new MenuItem("Copy");
            copyItem.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(msg.getText());
                clipboard.setContent(content);
            });
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> {
                chat.getMessages().remove(msg);
                com.sim800l.util.DataManager.saveChats(chatMap);
                loadChat(phoneNumber);
            });
            contextMenu.getItems().addAll(copyItem, deleteItem);
            messageBox.setOnContextMenuRequested(event -> {
                contextMenu.show(messageBox, event.getScreenX(), event.getScreenY());
            });
            
            messageBox.getChildren().addAll(messageLabel, timeLabel);
            
            HBox messageRow = new HBox();
            if (msg.isOutgoing()) {
                messageRow.setAlignment(Pos.CENTER_RIGHT);
                messageBox.getStyleClass().add("sent-message");
                messageBox.setStyle("-fx-background-color: " + Theme.outgoingBubble() + "; -fx-background-radius: 12; -fx-padding: 10;");
                messageLabel.setStyle("-fx-text-fill: white;");
            } else {
                messageRow.setAlignment(Pos.CENTER_LEFT);
                messageBox.getStyleClass().add("received-message");
                messageBox.setStyle("-fx-background-color: " + Theme.incomingBubble() + "; -fx-background-radius: 12; -fx-padding: 10;");
                messageLabel.setStyle("-fx-text-fill: " + Theme.incomingBubbleText() + ";");
            }
            messageRow.getChildren().add(messageBox);
            
            messageArea.getChildren().add(messageRow);
        }
        
        Platform.runLater(() -> messageScrollPane.setVvalue(1.0));
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyThemeToScene(alert.getDialogPane().getScene());
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        applyThemeToScene(alert.getDialogPane().getScene());
        alert.showAndWait();
    }
    
    private void showLogViewer() {
        // اگر پنجره از قبل باز است، آن را به جلو بیاور
        if (logViewerStage != null && logViewerStage.isShowing()) {
            logViewerStage.toFront();
            logViewerStage.requestFocus();
            return;
        }
        
        // ساخت پنجره جدید
        logViewerStage = new Stage();
        logViewerStage.setTitle("Serial Log Viewer");
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        
        Label titleLabel = new Label("Live Serial Communication Log");
        titleLabel.setFont(Font.font("Inter", FontWeight.BOLD, 16));
        
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setFont(Font.font("Monospace", 12));
        logArea.setWrapText(false);
        logArea.setStyle("-fx-cursor: text;"); // Make it selectable
        VBox.setVgrow(logArea, Priority.ALWAYS);
        
        // Update log every 500ms
        logUpdateTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(500),
                e -> {
                    String logs = serialManager.getLogs();
                    logArea.setText(logs);
                    logArea.setScrollTop(Double.MAX_VALUE);
                }
            )
        );
        logUpdateTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        logUpdateTimeline.play();
        
        HBox buttonBox = new HBox(10);
        Button clearButton = new Button("Clear Logs");
        clearButton.setFont(Font.font("Inter", 11));
        clearButton.setOnAction(e -> {
            serialManager.clearLogs();
            logArea.clear();
        });
        
        Button refreshButton = new Button("Refresh Now");
        refreshButton.setFont(Font.font("Inter", 11));
        refreshButton.setOnAction(e -> {
            String logs = serialManager.getLogs();
            logArea.setText(logs);
            logArea.setScrollTop(Double.MAX_VALUE);
        });
        
        buttonBox.getChildren().addAll(clearButton, refreshButton);
        
        root.getChildren().addAll(titleLabel, logArea, buttonBox);
        
        Scene scene = new Scene(root, 700, 500);
        applyThemeToScene(scene);
        logViewerStage.setScene(scene);
        
        // وقتی پنجره بسته شد، timeline را متوقف کن و متغیرها را null کن
        logViewerStage.setOnCloseRequest(e -> {
            if (logUpdateTimeline != null) {
                logUpdateTimeline.stop();
                logUpdateTimeline = null;
            }
            logViewerStage = null;
        });
        
        logViewerStage.show();
    }
    
    private void applyThemeToScene(Scene scene) {
        String css = generateThemeCSS();
        scene.getRoot().setStyle(css);
    }
    
    private String generateThemeCSS() {
        return "-fx-base: " + Theme.primaryBg() + ";" +
               "-fx-background: " + Theme.secondaryBg() + ";" +
               "-fx-control-inner-background: " + Theme.tertiaryBg() + ";" +
               "-fx-text-fill: " + Theme.primaryText() + ";";
    }
    
    private String getDisplayName(String phoneNumber) {
        com.sim800l.model.Contact contact = contacts.get(phoneNumber);
        return contact != null ? contact.getName() : phoneNumber;
    }
    
    private void showContactsManager() {
        // اگر پنجره از قبل باز است، آن را به جلو بیاور
        if (contactsManagerStage != null && contactsManagerStage.isShowing()) {
            contactsManagerStage.toFront();
            contactsManagerStage.requestFocus();
            return;
        }
        
        // ساخت پنجره جدید
        contactsManagerStage = new Stage();
        contactsManagerStage.setTitle("Contacts Manager");
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Contacts");
        titleLabel.setFont(Font.font("Inter", FontWeight.BOLD, 18));
        
        ListView<com.sim800l.model.Contact> contactsList = new ListView<>();
        contactsList.getItems().addAll(contacts.values());
        contactsList.setCellFactory(param -> new ListCell<com.sim800l.model.Contact>() {
            @Override
            protected void updateItem(com.sim800l.model.Contact contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setText(null);
                } else {
                    setText(contact.getName() + "\n" + contact.getPhoneNumber());
                }
            }
        });
        VBox.setVgrow(contactsList, Priority.ALWAYS);
        
        HBox buttonBox = new HBox(10);
        Button addButton = new Button("Add Contact");
        addButton.setOnAction(e -> {
            showAddContactDialog(null);
        });
        
        Button editButton = new Button("Edit");
        editButton.setOnAction(e -> {
            com.sim800l.model.Contact selected = contactsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAddContactDialog(selected.getPhoneNumber());
            }
        });
        
        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> {
            com.sim800l.model.Contact selected = contactsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                contacts.remove(selected.getPhoneNumber());
                com.sim800l.util.DataManager.saveContacts(contacts);
                contactsList.getItems().remove(selected);
                
                // Update chat list if this contact has a chat
                if (chatMap.containsKey(selected.getPhoneNumber())) {
                    chatMap.get(selected.getPhoneNumber()).setName(null);
                    com.sim800l.util.DataManager.saveChats(chatMap);
                    chatListView.refresh();
                    
                    // Update header if this chat is currently open
                    if (selected.getPhoneNumber().equals(currentPhoneNumber)) {
                        chatHeaderLabel.setText(selected.getPhoneNumber());
                    }
                }
            }
        });
        
        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);
        root.getChildren().addAll(titleLabel, contactsList, buttonBox);
        
        Scene scene = new Scene(root, 400, 500);
        applyThemeToScene(scene);
        contactsManagerStage.setScene(scene);
        contactsManagerStage.show();
    }
    
    private void showAddContactDialog(String existingPhone) {
        // اگر پنجره از قبل باز است، آن را ببند
        if (addContactDialogStage != null && addContactDialogStage.isShowing()) {
            addContactDialogStage.close();
        }
        
        // ساخت پنجره جدید
        addContactDialogStage = new Stage();
        addContactDialogStage.setTitle(existingPhone == null ? "Add Contact" : "Edit Contact");
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        Label titleLabel = new Label(existingPhone == null ? "New Contact" : "Edit Contact");
        titleLabel.setFont(Font.font("Inter", FontWeight.BOLD, 16));
        
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number");
        phoneField.setStyle("-fx-text-fill: " + Theme.inputText() + "; -fx-prompt-text-fill: " + Theme.mutedText() + ";");
        if (existingPhone != null) {
            phoneField.setText(existingPhone);
            phoneField.setEditable(false);
        }
        
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        nameField.setStyle("-fx-text-fill: " + Theme.inputText() + "; -fx-prompt-text-fill: " + Theme.mutedText() + ";");
        
        TextField emailField = new TextField();
        emailField.setPromptText("Email (optional)");
        emailField.setStyle("-fx-text-fill: " + Theme.inputText() + "; -fx-prompt-text-fill: " + Theme.mutedText() + ";");
        
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes (optional)");
        notesArea.setPrefRowCount(3);
        notesArea.setStyle("-fx-text-fill: " + Theme.inputText() + "; -fx-prompt-text-fill: " + Theme.mutedText() + ";");
        
        // Load existing contact data
        if (existingPhone != null && contacts.containsKey(existingPhone)) {
            com.sim800l.model.Contact contact = contacts.get(existingPhone);
            nameField.setText(contact.getName());
            emailField.setText(contact.getEmail());
            notesArea.setText(contact.getNotes());
        }
        
        HBox buttonBox = new HBox(10);
        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            String phone = phoneField.getText().trim();
            String name = nameField.getText().trim();
            
            if (phone.isEmpty() || name.isEmpty()) {
                showAlert("Phone number and name are required!");
                return;
            }
            
            com.sim800l.model.Contact contact = new com.sim800l.model.Contact(phone, name);
            contact.setEmail(emailField.getText().trim());
            contact.setNotes(notesArea.getText().trim());
            
            contacts.put(phone, contact);
            com.sim800l.util.DataManager.saveContacts(contacts);
            
            // Update chat name if exists
            if (chatMap.containsKey(phone)) {
                chatMap.get(phone).setName(name);
                com.sim800l.util.DataManager.saveChats(chatMap);
                chatListView.refresh();
                
                // Update header if this chat is currently open
                if (phone.equals(currentPhoneNumber)) {
                    chatHeaderLabel.setText(name);
                }
            }
            
            // Refresh contacts manager if open
            if (contactsManagerStage != null && contactsManagerStage.isShowing()) {
                contactsManagerStage.close();
                showContactsManager();
            }
            
            addContactDialogStage.close();
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> addContactDialogStage.close());
        
        buttonBox.getChildren().addAll(saveButton, cancelButton);
        root.getChildren().addAll(titleLabel, phoneField, nameField, emailField, notesArea, buttonBox);
        
        Scene scene = new Scene(root, 350, 400);
        applyThemeToScene(scene);
        addContactDialogStage.setScene(scene);
        addContactDialogStage.show();
    }
    
    private void showNewMessageDialog() {
        // اگر پنجره از قبل باز است، آن را به جلو بیاور
        if (newMessageDialogStage != null && newMessageDialogStage.isShowing()) {
            newMessageDialogStage.toFront();
            newMessageDialogStage.requestFocus();
            return;
        }
        
        // ساخت پنجره جدید
        newMessageDialogStage = new Stage();
        newMessageDialogStage.setTitle("New Message");
        
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Send message to:");
        titleLabel.setFont(Font.font("Inter", FontWeight.BOLD, 16));
        
        // Phone number input
        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone Number (e.g., +989123456789)");
        phoneField.setStyle("-fx-text-fill: " + Theme.inputText() + "; -fx-prompt-text-fill: " + Theme.mutedText() + ";");
        
        // Show contacts for quick selection
        Label orLabel = new Label("Or select from contacts:");
        orLabel.setFont(Font.font("Inter", 11));
        orLabel.setStyle("-fx-text-fill: " + Theme.secondaryText() + ";");
        
        ListView<com.sim800l.model.Contact> contactsList = new ListView<>();
        contactsList.getItems().addAll(contacts.values());
        contactsList.setPrefHeight(150);
        contactsList.setCellFactory(param -> new ListCell<com.sim800l.model.Contact>() {
            @Override
            protected void updateItem(com.sim800l.model.Contact contact, boolean empty) {
                super.updateItem(contact, empty);
                if (empty || contact == null) {
                    setText(null);
                } else {
                    setText(contact.getName() + " - " + contact.getPhoneNumber());
                }
            }
        });
        
        // When contact is selected, fill phone field
        contactsList.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                phoneField.setText(selected.getPhoneNumber());
            }
        });
        
        HBox buttonBox = new HBox(10);
        Button startButton = new Button("Start Chat");
        startButton.setOnAction(e -> {
            String phone = phoneField.getText().trim();
            
            if (phone.isEmpty()) {
                showAlert("Phone number is required!");
                return;
            }
            
            // Create or open chat
            addOrUpdateChat(phone);
            loadChat(phone);
            
            // Select the chat in list
            for (ChatItem item : chatItems) {
                if (item.getPhoneNumber().equals(phone)) {
                    chatListView.getSelectionModel().select(item);
                    break;
                }
            }
            
            newMessageDialogStage.close();
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> newMessageDialogStage.close());
        
        buttonBox.getChildren().addAll(startButton, cancelButton);
        root.getChildren().addAll(titleLabel, phoneField, orLabel, contactsList, buttonBox);
        
        Scene scene = new Scene(root, 400, 450);
        applyThemeToScene(scene);
        newMessageDialogStage.setScene(scene);
        newMessageDialogStage.show();
    }

    @Override
    public void stop() {
        // متوقف کردن timeline لاگ
        if (logUpdateTimeline != null) {
            logUpdateTimeline.stop();
        }
        
        // بستن پنجره لاگ
        if (logViewerStage != null && logViewerStage.isShowing()) {
            logViewerStage.close();
        }
        
        // قطع اتصال سریال
        if (serialManager != null && serialManager.isConnected()) {
            serialManager.disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
