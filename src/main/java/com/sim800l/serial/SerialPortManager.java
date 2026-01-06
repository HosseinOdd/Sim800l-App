package com.sim800l.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.sim800l.util.MessageEncoder;

import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

public class SerialPortManager {
    private SerialPort serialPort;
    private Thread readerThread;
    private boolean isConnected;
    private static final int BAUD_RATE = 9600;
    private BiConsumer<String, String> messageReceivedCallback;
    private StringBuilder responseBuffer = new StringBuilder();
    private volatile boolean pauseReading = false;
    private StringBuilder logBuffer = new StringBuilder();
    
    public SerialPortManager() {
        this.isConnected = false;
    }
    
    public void setMessageReceivedCallback(BiConsumer<String, String> callback) {
        this.messageReceivedCallback = callback;
    }
    
    public boolean connect(String portName) {
        if (portName == null || portName.isEmpty()) {
            addLog("Connect failed: No port specified");
            return false;
        }
        
        addLog("Connecting to: " + portName);
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(BAUD_RATE);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
        
        if (!serialPort.openPort()) {
            addLog("Failed to open port: " + portName);
            return false;
        }
        
        addLog("Port opened successfully");
        isConnected = true;
        sleep(1000);
        
        // Initialize SIM800L with proper sequence
        addLog("Initializing SIM800L...");
        if (!sendCommandWithResponse("AT", "OK", 2000)) {
            addLog("Initialization failed");
            disconnect();
            return false;
        }
        
        // Configure SMS to text mode
        addLog("Setting text mode...");
        sendCommandWithResponse("AT+CMGF=1", "OK", 2000);
        
        // Configure character set for receiving
        addLog("Setting character set...");
        sendCommandWithResponse("AT+CSCS=\"GSM\"", "OK", 2000);
        
        // Configure how to handle incoming messages
        addLog("Configuring auto-receive...");
        sendCommandWithResponse("AT+CNMI=2,2,0,0,0", "OK", 2000);
        
        addLog("Starting reader thread...");
        startReaderThread();
        addLog("Connected successfully!");
        return true;
    }
    
    public void disconnect() {
        addLog("Disconnecting...");
        isConnected = false;
        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
            try {
                readerThread.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            addLog("Port closed");
        }
        addLog("Disconnected");
    }
    
    public boolean isConnected() {
        return isConnected && serialPort != null && serialPort.isOpen();
    }
    
    public boolean testConnection() {
        if (!isConnected()) {
            addLog("Test failed: Not connected");
            return false;
        }
        
        addLog("Testing connection...");
        
        // Pause reader thread temporarily to avoid conflict
        pauseReading = true;
        sleep(200); // Give reader thread time to pause
        
        try {
            // Clear any pending data
            while (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(buffer, buffer.length);
            }
            
            // Send AT command
            sendCommand("AT");
            
            // Wait for OK response directly
            StringBuilder response = new StringBuilder();
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < 3000) {
                if (serialPort.bytesAvailable() > 0) {
                    byte[] buffer = new byte[serialPort.bytesAvailable()];
                    int numRead = serialPort.readBytes(buffer, buffer.length);
                    if (numRead > 0) {
                        String data = new String(buffer, 0, numRead, StandardCharsets.UTF_8);
                        response.append(data);
                        addLog("Test RX: " + data.trim());
                        
                        if (response.toString().contains("OK")) {
                            addLog("Test successful!");
                            return true;
                        }
                        
                        if (response.toString().contains("ERROR")) {
                            addLog("Test failed: ERROR received");
                            return false;
                        }
                    }
                }
                sleep(50);
            }
            
            addLog("Test failed: Timeout (received: " + response.toString().trim() + ")");
            return false;
        } finally {
            // Resume reader thread
            pauseReading = false;
            addLog("Reader thread resumed");
        }
    }
    
    public void sendSMS(String phoneNumber, String message) {
        if (!isConnected()) {
            throw new RuntimeException("Not connected to serial port");
        }
        
        // Validate phone number to prevent command injection
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be empty");
        }
        
        // Sanitize phone number - only allow digits, +, -, spaces, and parentheses
        String sanitizedPhone = phoneNumber.replaceAll("[^0-9+\\-() ]", "");
        if (sanitizedPhone.isEmpty()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        
        // Validate message
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        
        // Check message length (SMS max is 160 for 7-bit, 70 for UCS2)
        // Use codePointCount for proper emoji handling
        boolean isAscii = MessageEncoder.isAscii(message);
        int actualLength = message.codePointCount(0, message.length());
        int maxLength = isAscii ? 160 : 70;
        if (actualLength > maxLength) {
            throw new IllegalArgumentException("Message too long. Max " + maxLength + " characters for " + 
                (isAscii ? "English" : "Unicode") + " messages. Current: " + actualLength);
        }
        
        addLog("Sending SMS to: " + sanitizedPhone);
        addLog("Message: " + message);
        
        // Detect language and choose encoding
        if (isAscii) {
            addLog("Detected: English/ASCII - Using 7-bit encoding");
            sendSMS_TextMode(sanitizedPhone, message);
        } else {
            addLog("Detected: Unicode (Persian/Arabic/etc) - Using PDU mode");
            sendSMS_PDUMode(sanitizedPhone, message);
        }
    }
    
    /**
     * Send SMS in Text Mode (for ASCII/English)
     */
    private void sendSMS_TextMode(String phoneNumber, String message) {
        
        try {
            // Validate and sanitize message - prevent Ctrl+Z injection
            if (message.contains("\u001A") || message.contains("\u001B")) {
                throw new IllegalArgumentException("Message contains invalid control characters");
            }
            
            // Remove any CR/LF to prevent command injection
            String sanitizedMessage = message.replaceAll("[\r\n]", " ");
            
            // Use GSM mode
            if (!sendCommandWithResponse("AT+CSCS=\"GSM\"", "OK", 2000)) {
                throw new RuntimeException("Failed to set GSM mode");
            }
            
            // Pause reader thread to read prompt directly
            pauseReading = true;
            sleep(100);
            
            try {
                // Send SMS command with phone number
                sendCommand("AT+CMGS=\"" + phoneNumber + "\"");
                
                // Wait for ">" prompt directly
                addLog("Waiting for: >");
                long startTime = System.currentTimeMillis();
                StringBuilder response = new StringBuilder();
                
                while (System.currentTimeMillis() - startTime < 5000) {
                    if (serialPort.bytesAvailable() > 0) {
                        byte[] buffer = new byte[serialPort.bytesAvailable()];
                        serialPort.readBytes(buffer, buffer.length);
                        String data = new String(buffer, StandardCharsets.UTF_8);
                        response.append(data);
                        addLog("CMD RX: " + data.trim());
                        
                        if (response.toString().contains(">")) {
                            addLog("Response received: >");
                            break;
                        }
                    }
                    sleep(50);
                }
                
                if (!response.toString().contains(">")) {
                    throw new RuntimeException("No prompt received for message");
                }
                
                addLog("Sending message body...");
                // Send only ASCII characters (sanitized)
                byte[] messageBytes = sanitizedMessage.getBytes(StandardCharsets.US_ASCII);
                serialPort.writeBytes(messageBytes, messageBytes.length);
                sleep(100);
                
                addLog("Sending Ctrl+Z...");
                // Send Ctrl+Z to submit
                serialPort.writeBytes(new byte[]{26}, 1);
                
                // Wait for OK response
                startTime = System.currentTimeMillis();
                response = new StringBuilder();
                
                while (System.currentTimeMillis() - startTime < 30000) {
                    if (serialPort.bytesAvailable() > 0) {
                        byte[] buffer = new byte[serialPort.bytesAvailable()];
                        serialPort.readBytes(buffer, buffer.length);
                        String data = new String(buffer, StandardCharsets.UTF_8);
                        response.append(data);
                        addLog("CMD RX: " + data.trim());
                        
                        if (response.toString().contains("OK")) {
                            addLog("SMS sent successfully!");
                            break;
                        }
                        if (response.toString().contains("ERROR")) {
                            throw new RuntimeException("Message not sent - ERROR response");
                        }
                    }
                    sleep(100);
                }
                
                if (!response.toString().contains("OK")) {
                    throw new RuntimeException("Message not sent - timeout");
                }
            } finally {
                pauseReading = false;
            }
        } catch (Exception e) {
            addLog("Error sending SMS: " + e.getMessage());
            // Try to recover
            sendCommand("\u001B"); // ESC to cancel
            sleep(500);
            throw e;
        }
    }
    
    /**
     * Send SMS in PDU Mode (for Unicode/Persian/Arabic)
     */
    private void sendSMS_PDUMode(String phoneNumber, String message) {
        try {
            // Set to PDU mode (AT+CMGF=0)
            if (!sendCommandWithResponse("AT+CMGF=0", "OK", 2000)) {
                throw new RuntimeException("Failed to set PDU mode");
            }
            
            // Encode message to PDU
            com.sim800l.util.PDUEncoder.PDUResult pduResult = com.sim800l.util.PDUEncoder.encodePDU(phoneNumber, message);
            String pduString = pduResult.pdu;
            int tpduLength = pduResult.tpduLength;
            
            addLog("PDU: " + pduString);
            addLog("TPDU Length: " + tpduLength);
            
            // Pause reader thread
            pauseReading = true;
            sleep(100);
            
            try {
                // Send AT+CMGS command with TPDU length
                sendCommand("AT+CMGS=" + tpduLength);
                
                // Wait for ">" prompt
                addLog("Waiting for: >");
                long startTime = System.currentTimeMillis();
                StringBuilder response = new StringBuilder();
                
                while (System.currentTimeMillis() - startTime < 5000) {
                    if (serialPort.bytesAvailable() > 0) {
                        byte[] buffer = new byte[serialPort.bytesAvailable()];
                        serialPort.readBytes(buffer, buffer.length);
                        String data = new String(buffer, StandardCharsets.UTF_8);
                        response.append(data);
                        addLog("CMD RX: " + data.trim());
                        
                        if (response.toString().contains(">")) {
                            addLog("Response received: >");
                            break;
                        }
                    }
                    sleep(50);
                }
                
                if (!response.toString().contains(">")) {
                    throw new RuntimeException("No prompt received for PDU");
                }
                
                addLog("Sending PDU string...");
                // Send PDU string as ASCII (it's hex string)
                byte[] pduBytes = pduString.getBytes(StandardCharsets.US_ASCII);
                serialPort.writeBytes(pduBytes, pduBytes.length);
                sleep(100);
                
                addLog("Sending Ctrl+Z...");
                // Send Ctrl+Z to submit
                serialPort.writeBytes(new byte[]{26}, 1);
                
                // Wait for OK response
                startTime = System.currentTimeMillis();
                response = new StringBuilder();
                
                while (System.currentTimeMillis() - startTime < 30000) {
                    if (serialPort.bytesAvailable() > 0) {
                        byte[] buffer = new byte[serialPort.bytesAvailable()];
                        serialPort.readBytes(buffer, buffer.length);
                        String data = new String(buffer, StandardCharsets.UTF_8);
                        response.append(data);
                        addLog("CMD RX: " + data.trim());
                        
                        if (response.toString().contains("OK")) {
                            addLog("SMS sent successfully via PDU mode!");
                            break;
                        }
                        if (response.toString().contains("ERROR")) {
                            throw new RuntimeException("PDU message not sent - ERROR response");
                        }
                    }
                    sleep(100);
                }
                
                if (!response.toString().contains("OK")) {
                    throw new RuntimeException("PDU message not sent - timeout");
                }
                
                // Switch back to text mode for receiving
                pauseReading = false;
                sleep(100);
                sendCommandWithResponse("AT+CMGF=1", "OK", 2000);
                
            } finally {
                pauseReading = false;
            }
        } catch (Exception e) {
            addLog("Error sending SMS via PDU: " + e.getMessage());
            // Try to switch back to text mode
            try {
                sendCommandWithResponse("AT+CMGF=1", "OK", 2000);
            } catch (Exception ex) {
                // Ignore
            }
            // Try to recover
            sendCommand("\u001B"); // ESC to cancel
            sleep(500);
            throw e;
        }
    }
    
    private void sendCommand(String command) {
        if (serialPort != null && serialPort.isOpen()) {
            String cmd = command + "\r\n";
            serialPort.writeBytes(cmd.getBytes(StandardCharsets.UTF_8), cmd.length());
            addLog("TX: " + command);
            sleep(100);
        }
    }
    
    private boolean sendCommandWithResponse(String command, String expectedResponse, int timeoutMs) {
        responseBuffer.setLength(0);
        
        // Pause reader thread to avoid conflict
        pauseReading = true;
        sleep(100);
        
        try {
            // Clear any pending data
            while (serialPort.bytesAvailable() > 0) {
                byte[] buffer = new byte[serialPort.bytesAvailable()];
                serialPort.readBytes(buffer, buffer.length);
            }
            
            sendCommand(command);
            addLog("Waiting for: " + expectedResponse);
            
            // Read response directly
            StringBuilder localBuffer = new StringBuilder();
            long startTime = System.currentTimeMillis();
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (serialPort.bytesAvailable() > 0) {
                    byte[] buffer = new byte[serialPort.bytesAvailable()];
                    int numRead = serialPort.readBytes(buffer, buffer.length);
                    if (numRead > 0) {
                        String data = new String(buffer, 0, numRead, StandardCharsets.UTF_8);
                        localBuffer.append(data);
                        addLog("CMD RX: " + data.trim());
                        
                        if (localBuffer.toString().contains(expectedResponse)) {
                            addLog("Response received: OK");
                            return true;
                        }
                        
                        if (localBuffer.toString().contains("ERROR")) {
                            addLog("ERROR response received");
                            return false;
                        }
                    }
                }
                sleep(50);
            }
            
            addLog("Timeout waiting for: " + expectedResponse);
            return false;
        } finally {
            // Resume reader thread
            pauseReading = false;
        }
    }
    
    private boolean waitForResponse(String expectedResponse, int timeoutMs) {
        // Pause reader thread to avoid conflict
        boolean wasReading = !pauseReading;
        pauseReading = true;
        
        if (wasReading) {
            sleep(100); // Give reader thread time to pause
        }
        
        try {
            long startTime = System.currentTimeMillis();
            StringBuilder localBuffer = new StringBuilder();
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (serialPort.bytesAvailable() > 0) {
                    byte[] buffer = new byte[serialPort.bytesAvailable()];
                    int numRead = serialPort.readBytes(buffer, buffer.length);
                    if (numRead > 0) {
                        String data = new String(buffer, 0, numRead, StandardCharsets.UTF_8);
                        localBuffer.append(data);
                        addLog("Wait RX: " + data.trim());
                        
                        if (localBuffer.toString().contains(expectedResponse)) {
                            return true;
                        }
                        
                        // Check for errors
                        if (localBuffer.toString().contains("ERROR")) {
                            addLog("ERROR response received");
                            return false;
                        }
                    }
                }
                sleep(50);
            }
            addLog("Timeout waiting for: " + expectedResponse);
            return false;
        } finally {
            // Resume reader thread only if it was reading before
            if (wasReading) {
                pauseReading = false;
            }
        }
    }
    
    private void startReaderThread() {
        readerThread = new Thread(() -> {
            StringBuilder buffer = new StringBuilder();
            String pendingSender = null;
            
            while (isConnected && !Thread.interrupted()) {
                try {
                    // Check if reading is paused (e.g., during test connection)
                    if (pauseReading) {
                        Thread.sleep(100);
                        continue;
                    }
                    
                    if (serialPort.bytesAvailable() > 0) {
                        byte[] readBuffer = new byte[serialPort.bytesAvailable()];
                        int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
                        if (numRead > 0) {
                            String data = new String(readBuffer, 0, numRead, StandardCharsets.UTF_8);
                            buffer.append(data);
                            addLog("Reader RX: " + data.trim());
                        }
                        
                        // Process complete lines
                        while (buffer.indexOf("\r\n") != -1) {
                            int lineEnd = buffer.indexOf("\r\n");
                            String line = buffer.substring(0, lineEnd).trim();
                            buffer.delete(0, lineEnd + 2);
                            
                            if (line.startsWith("+CMT:")) {
                                // Incoming message header
                                pendingSender = parseSender(line);
                                addLog("Incoming SMS from: " + pendingSender);
                            } else if (pendingSender != null && !line.isEmpty() && !line.equals("OK")) {
                                // Message body
                                String decodedMessage = MessageEncoder.decode(line);
                                addLog("SMS Body: " + decodedMessage);
                                if (messageReceivedCallback != null) {
                                    final String sender = pendingSender;
                                    final String msg = decodedMessage;
                                    messageReceivedCallback.accept(sender, msg);
                                }
                                pendingSender = null;
                            }
                        }
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Error in reader thread: " + e.getMessage());
                }
            }
        });
        readerThread.setDaemon(true);
        readerThread.start();
    }
    
    private String parseSender(String cmtLine) {
        try {
            // Format: +CMT: "+1234567890","","21/01/05,12:30:45+00"
            String[] parts = cmtLine.split("\"");
            if (parts.length > 1) {
                String sender = parts[1];
                
                // Limit sender length to prevent DoS
                if (sender.length() > 100) {
                    sender = sender.substring(0, 100);
                }
                
                // Check if it's hex-encoded UCS2 (without regex)
                boolean isHex = sender.length() % 4 == 0 && sender.length() > 0;
                if (isHex) {
                    for (int i = 0; i < sender.length(); i++) {
                        char c = sender.charAt(i);
                        if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                            isHex = false;
                            break;
                        }
                    }
                    if (isHex) {
                        return MessageEncoder.decodeUCS2(sender);
                    }
                }
                return sender;
            }
        } catch (Exception e) {
            System.err.println("Error parsing sender: " + e.getMessage());
        }
        return "Unknown";
    }
    
    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private synchronized void addLog(String message) {
        String timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        logBuffer.append("[").append(timestamp).append("] ").append(message).append("\n");
        
        // Keep only last 1000 lines
        String[] lines = logBuffer.toString().split("\n");
        if (lines.length > 1000) {
            logBuffer = new StringBuilder();
            for (int i = lines.length - 1000; i < lines.length; i++) {
                logBuffer.append(lines[i]).append("\n");
            }
        }
    }
    
    public synchronized String getLogs() {
        return logBuffer.toString();
    }
    
    public synchronized void clearLogs() {
        logBuffer = new StringBuilder();
    }
    
    public static String[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] portNames = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            portNames[i] = ports[i].getSystemPortName();
        }
        return portNames;
    }
}
