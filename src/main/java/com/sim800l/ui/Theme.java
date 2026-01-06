package com.sim800l.ui;

import javafx.scene.paint.Color;

public class Theme {
    
    public enum Mode {
        LIGHT, DARK
    }
    
    private static Mode currentMode = Mode.DARK;
    
    // Background colors
    public static String primaryBg() {
        return currentMode == Mode.DARK ? "#1a1a1a" : "#ffffff";
    }
    
    public static String secondaryBg() {
        return currentMode == Mode.DARK ? "#242424" : "#f6f8fa";
    }
    
    public static String tertiaryBg() {
        return currentMode == Mode.DARK ? "#2e2e2e" : "#eaeef2";
    }
    
    // Text colors
    public static String primaryText() {
        return currentMode == Mode.DARK ? "#f0f0f0" : "#24292f";
    }
    
    public static String secondaryText() {
        return currentMode == Mode.DARK ? "#a0a0a0" : "#57606a";
    }
    
    public static String mutedText() {
        return currentMode == Mode.DARK ? "#707070" : "#8c959f";
    }
    
    // Accent colors
    public static String accent() {
        return currentMode == Mode.DARK ? "#4a9eff" : "#0969da";
    }
    
    public static String accentHover() {
        return currentMode == Mode.DARK ? "#6bb3ff" : "#0550ae";
    }
    
    // Message bubble colors
    public static String outgoingBubble() {
        return currentMode == Mode.DARK ? "#0084ff" : "#0969da";
    }
    
    public static String incomingBubble() {
        return currentMode == Mode.DARK ? "#2e2e2e" : "#f6f8fa";
    }
    
    public static String incomingBubbleText() {
        return currentMode == Mode.DARK ? "#f0f0f0" : "#24292f";
    }
    
    // Border colors
    public static String border() {
        return currentMode == Mode.DARK ? "#3a3a3a" : "#d0d7de";
    }
    
    public static String borderHover() {
        return currentMode == Mode.DARK ? "#4a9eff" : "#0969da";
    }
    
    // Status colors
    public static String success() {
        return "#2ea043";
    }
    
    public static String error() {
        return "#f85149";
    }
    
    public static String warning() {
        return "#d29922";
    }
    
    // Input field colors
    public static String inputBg() {
        return currentMode == Mode.DARK ? "#2e2e2e" : "#ffffff";
    }
    
    public static String inputText() {
        return currentMode == Mode.DARK ? "#f0f0f0" : "#24292f";
    }
    
    public static String inputBorder() {
        return currentMode == Mode.DARK ? "#3a3a3a" : "#d0d7de";
    }
    
    public static String inputFocusBorder() {
        return currentMode == Mode.DARK ? "#4a9eff" : "#0969da";
    }
    
    // Methods
    public static Mode getMode() {
        return currentMode;
    }
    
    public static void setMode(Mode mode) {
        currentMode = mode;
    }
    
    public static void toggle() {
        currentMode = (currentMode == Mode.DARK) ? Mode.LIGHT : Mode.DARK;
    }
    
    public static boolean isDark() {
        return currentMode == Mode.DARK;
    }
}
