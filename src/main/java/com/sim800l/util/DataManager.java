package com.sim800l.util;

import com.sim800l.model.ChatItem;
import com.sim800l.model.Contact;

import java.io.*;
import java.util.*;

public class DataManager {
    private static final String DATA_DIR = System.getProperty("user.home") + "/.sim800l";
    private static final String CHATS_FILE = DATA_DIR + "/chats.dat";
    private static final String CONTACTS_FILE = DATA_DIR + "/contacts.dat";
    
    static {
        // Create data directory if it doesn't exist
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    // Save chats
    public static void saveChats(Map<String, ChatItem> chatMap) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CHATS_FILE))) {
            oos.writeObject(chatMap);
        } catch (IOException e) {
            System.err.println("Error saving chats: " + e.getMessage());
        }
    }
    
    // Load chats with security checks
    @SuppressWarnings("unchecked")
    public static Map<String, ChatItem> loadChats() {
        File file = new File(CHATS_FILE);
        if (!file.exists()) {
            return new HashMap<>();
        }
        
        // Check file size to prevent DoS (max 10MB)
        if (file.length() > 10 * 1024 * 1024) {
            System.err.println("Chats file too large - possible corruption");
            return new HashMap<>();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            
            // Validate type before casting
            if (!(obj instanceof Map)) {
                System.err.println("Invalid chats file format");
                return new HashMap<>();
            }
            
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            Map<String, ChatItem> result = new HashMap<>();
            
            // Validate each entry
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof ChatItem) {
                    result.put((String) entry.getKey(), (ChatItem) entry.getValue());
                }
            }
            
            return result;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading chats: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    // Save contacts
    public static void saveContacts(Map<String, Contact> contacts) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CONTACTS_FILE))) {
            oos.writeObject(contacts);
        } catch (IOException e) {
            System.err.println("Error saving contacts: " + e.getMessage());
        }
    }
    
    // Load contacts with security checks
    @SuppressWarnings("unchecked")
    public static Map<String, Contact> loadContacts() {
        File file = new File(CONTACTS_FILE);
        if (!file.exists()) {
            return new HashMap<>();
        }
        
        // Check file size to prevent DoS (max 10MB)
        if (file.length() > 10 * 1024 * 1024) {
            System.err.println("Contacts file too large - possible corruption");
            return new HashMap<>();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            
            // Validate type before casting
            if (!(obj instanceof Map)) {
                System.err.println("Invalid contacts file format");
                return new HashMap<>();
            }
            
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            Map<String, Contact> result = new HashMap<>();
            
            // Validate each entry
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Contact) {
                    result.put((String) entry.getKey(), (Contact) entry.getValue());
                }
            }
            
            return result;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading contacts: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    // Delete chat
    public static void deleteChat(String phoneNumber, Map<String, ChatItem> chatMap) {
        chatMap.remove(phoneNumber);
        saveChats(chatMap);
    }
    
    // Clear all data
    public static void clearAllData() {
        new File(CHATS_FILE).delete();
        new File(CONTACTS_FILE).delete();
    }
}
