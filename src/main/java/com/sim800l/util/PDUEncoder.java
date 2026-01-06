package com.sim800l.util;

public class PDUEncoder {
    
    /**
     * Encode SMS to PDU format
     * @param phoneNumber Recipient phone number
     * @param message Message text
     * @return PDU string and length
     */
    public static PDUResult encodePDU(String phoneNumber, String message) {
        // Validate inputs
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        
        // Sanitize phone number
        String sanitizedPhone = phoneNumber.replaceAll("[^0-9+]", "");
        if (sanitizedPhone.isEmpty()) {
            throw new IllegalArgumentException("Invalid phone number - no valid digits");
        }
        
        // Use codePointCount for proper emoji handling
        int actualLength = message.codePointCount(0, message.length());
        
        // Check message length for UCS2 (70 chars max for single SMS)
        if (!MessageEncoder.isAscii(message) && actualLength > 70) {
            throw new IllegalArgumentException("Unicode message too long. Max 70 characters. Current: " + actualLength);
        }
        
        // Check message length for 7-bit (160 chars max for single SMS)
        if (MessageEncoder.isAscii(message) && actualLength > 160) {
            throw new IllegalArgumentException("ASCII message too long. Max 160 characters. Current: " + actualLength);
        }
        
        StringBuilder pdu = new StringBuilder();
        
        // SMSC (SMS Center) - using default (00)
        pdu.append("00");
        
        // PDU type - SMS-SUBMIT with validity period
        // 01 = SMS-SUBMIT
        // + 10 = Validity Period Format (relative)
        pdu.append("11");
        
        // Message Reference (00 = let phone set it)
        pdu.append("00");
        
        // Destination Address (phone number)
        String encodedPhone = encodePhoneNumber(sanitizedPhone);
        pdu.append(encodedPhone);
        
        // Protocol Identifier (00 = standard)
        pdu.append("00");
        
        // Data Coding Scheme
        boolean isAscii = MessageEncoder.isAscii(message);
        if (isAscii) {
            // 00 = 7-bit GSM default alphabet
            pdu.append("00");
        } else {
            // 08 = UCS2 (16-bit Unicode)
            pdu.append("08");
        }
        
        // Validity Period (relative format) - FF = maximum (63 weeks)
        pdu.append("FF");
        
        // User Data
        if (isAscii) {
            // Encode as 7-bit GSM
            String encoded7bit = encode7bit(message);
            // User Data Length (in septets for 7-bit)
            pdu.append(String.format("%02X", message.length()));
            pdu.append(encoded7bit);
        } else {
            // Encode as UCS2 (16-bit Unicode)
            String encodedUCS2 = MessageEncoder.encodeUCS2(message);
            // User Data Length (in bytes for UCS2)
            pdu.append(String.format("%02X", encodedUCS2.length() / 2));
            pdu.append(encodedUCS2);
        }
        
        // Calculate TPDU length (everything except SMSC)
        int tpduLength = (pdu.length() - 2) / 2;
        
        return new PDUResult(pdu.toString(), tpduLength);
    }
    
    /**
     * Encode phone number in PDU format
     */
    private static String encodePhoneNumber(String phoneNumber) {
        StringBuilder result = new StringBuilder();
        
        // Remove + if present
        String number = phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;
        
        // Type of address
        // 91 = international format
        // 81 = national format
        String typeOfAddress = phoneNumber.startsWith("+") ? "91" : "81";
        
        // Length of phone number (number of digits)
        result.append(String.format("%02X", number.length()));
        
        // Type of address
        result.append(typeOfAddress);
        
        // Swap digits in pairs (semi-octets)
        if (number.length() % 2 != 0) {
            number += "F"; // Pad with F if odd length
        }
        
        for (int i = 0; i < number.length(); i += 2) {
            result.append(number.charAt(i + 1));
            result.append(number.charAt(i));
        }
        
        return result.toString();
    }
    
    /**
     * Encode message as 7-bit GSM
     */
    private static String encode7bit(String message) {
        StringBuilder result = new StringBuilder();
        int bits = 0;
        int carry = 0;
        
        for (int i = 0; i < message.length(); i++) {
            int char7bit = message.charAt(i) & 0x7F;
            
            int shift = bits % 7;
            if (shift == 0) {
                result.append(String.format("%02X", char7bit));
            } else {
                int current = ((char7bit << shift) | carry) & 0xFF;
                result.append(String.format("%02X", current));
                carry = char7bit >> (7 - shift);
            }
            
            bits += 7;
            
            // Every 8th character doesn't need a new byte
            if (shift == 6) {
                carry = 0;
            }
        }
        
        // Append remaining carry if any
        if (carry != 0) {
            result.append(String.format("%02X", carry));
        }
        
        return result.toString();
    }
    
    /**
     * Result class containing PDU string and TPDU length
     */
    public static class PDUResult {
        public final String pdu;
        public final int tpduLength;
        
        public PDUResult(String pdu, int tpduLength) {
            this.pdu = pdu;
            this.tpduLength = tpduLength;
        }
    }
}
