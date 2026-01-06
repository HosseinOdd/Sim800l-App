package com.sim800l.util;

public class MessageEncoder {
    
    public static String encode(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (isAscii(text)) {
            return text;
        }
        return encodeUCS2(text);
    }
    
    public static String decode(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // Check if it's hex without regex (prevent ReDoS)
        if (text.length() % 4 == 0) {
            boolean isHex = true;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                    isHex = false;
                    break;
                }
            }
            if (isHex) {
                return decodeUCS2(text);
            }
        }
        return text;
    }
    
    public static boolean isAscii(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        // Use simple loop instead of regex to prevent ReDoS
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c > 127) {
                return false;
            }
        }
        return true;
    }
    
    public static String encodeUCS2(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        if (text.isEmpty()) {
            return "";
        }
        StringBuilder hex = new StringBuilder();
        
        // Use codePoints to properly handle emoji (surrogate pairs)
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            
            if (Character.isSupplementaryCodePoint(codePoint)) {
                // Emoji and other supplementary characters (4-byte UTF-16)
                // Encode as surrogate pair
                char highSurrogate = Character.highSurrogate(codePoint);
                char lowSurrogate = Character.lowSurrogate(codePoint);
                hex.append(String.format("%04X%04X", (int) highSurrogate, (int) lowSurrogate));
                i += 2; // Surrogate pair takes 2 chars
            } else {
                // Regular BMP characters (2-byte UTF-16)
                hex.append(String.format("%04X", codePoint));
                i += 1;
            }
        }
        return hex.toString();
    }
    
    public static String decodeUCS2(String hex) {
        if (hex == null) {
            return "";
        }
        try {
            hex = hex.replaceAll("[^0-9A-Fa-f]", "");
            if (hex.length() % 4 != 0) {
                hex = hex.substring(0, hex.length() - (hex.length() % 4));
            }
            if (hex.isEmpty()) {
                return "";
            }
            
            // Limit decode length to prevent DoS attacks (max 1000 chars)
            int maxChars = Math.min(hex.length() / 4, 1000);
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < maxChars * 4; i += 4) {
                String hexChar = hex.substring(i, i + 4);
                int charCode = Integer.parseInt(hexChar, 16);
                
                // Check if this is a high surrogate (emoji first part)
                if (Character.isHighSurrogate((char) charCode) && i + 8 <= maxChars * 4) {
                    // Read the next 4 hex digits for low surrogate
                    String hexLow = hex.substring(i + 4, i + 8);
                    int lowSurrogate = Integer.parseInt(hexLow, 16);
                    
                    if (Character.isLowSurrogate((char) lowSurrogate)) {
                        // Combine surrogate pair into codePoint
                        int codePoint = Character.toCodePoint((char) charCode, (char) lowSurrogate);
                        result.appendCodePoint(codePoint);
                        i += 4; // Skip the low surrogate in next iteration
                        continue;
                    }
                }
                
                // Validate Unicode range to prevent invalid characters
                if (charCode >= 0 && charCode <= 0x10FFFF) {
                    result.append((char) charCode);
                }
            }
            return result.toString().trim();
        } catch (Exception e) {
            // Return original hex on error (safer than crashing)
            return hex;
        }
    }
}
