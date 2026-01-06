# SIM800L SMS Manager

A secure SMS management application for SIM800L GSM modules with full Unicode and emoji support.

[![Build](https://github.com/YOUR_USERNAME/sim800l-manager/actions/workflows/build.yml/badge.svg)](https://github.com/YOUR_USERNAME/sim800l-manager/actions/workflows/build.yml)
[![Release](https://github.com/YOUR_USERNAME/sim800l-manager/actions/workflows/release.yml/badge.svg)](https://github.com/YOUR_USERNAME/sim800l-manager/actions/workflows/release.yml)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.2-blue.svg)](https://openjfx.io/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

## Features

- Send and receive SMS messages in real-time
- Multi-language support (English, Arabic, Persian, Chinese, emoji)
- Modern chat-style user interface
- Contact management system
- Automatic encoding detection (7-bit GSM / UCS2)
- PDU mode for Unicode messages
- Desktop notifications
- Thread-safe operations
- Secure input validation

## Requirements

- Java 17 or higher
- Maven 3.6+ (for building)
- SIM800L GSM module
- USB-to-Serial adapter
- Active SIM card

## Installation

### Option 1: Download Release (Recommended)

Download pre-built executables from [Releases](../../releases):

- **Linux**: `SIM800L-Manager-linux-x64.tar.gz`
- **Windows**: `SIM800L-Manager-windows-x64.zip`
- **macOS**: `SIM800L-Manager-macos-x64.dmg`

No Java installation required!

### Option 2: Build from Source

```bash
git clone https://github.com/yourusername/sim800l-manager.git
cd sim800l-manager
mvn clean package
```

### Run Application

```bash
mvn javafx:run
```

Or run the compiled JAR:

```bash
java -jar target/SIM800LManager-v2.0.jar
```

## Quick Start

1. Connect SIM800L module via USB-Serial adapter
2. Insert active SIM card and power on
3. On Linux, add user to dialout group:
   ```bash
   sudo usermod -aG dialout $USER
   ```
4. Launch application and select serial port
5. Click "Connect" and wait for confirmation
6. Start sending messages

## Usage

### Sending Messages

**Text Mode (English)** - Up to 160 characters:
```
Hello, how are you?
```

**PDU Mode (Unicode)** - Up to 70 characters:
```
مرحبا كيف حالك？
你好，这是一条测试消息。
Hello 😀 How are you? 🎉
```

The application automatically detects the appropriate encoding mode.

### Message Encoding

| Type | Encoding | Max Length |
|------|----------|------------|
| English/ASCII | 7-bit GSM | 160 chars |
| Unicode/Emoji | UCS2 | 70 chars |

## Configuration

### Data Storage

Application data is stored in `~/.sim800l/`:
- `chats.dat` - Message history
- `contacts.dat` - Saved contacts

### Serial Port Settings

- Baud Rate: 9600
- Data Bits: 8
- Stop Bits: 1
- Parity: None

## Security

This application implements comprehensive security measures:

- Input validation and sanitization
- Command injection prevention
- ReDoS protection
- Safe deserialization
- Thread-safe operations

For details, see [SECURITY.md](SECURITY.md).

## Troubleshooting

### Connection Issues

**Cannot connect to serial port:**
1. Check SIM800L power LED is blinking
2. Verify USB cable connection
3. Check serial port permissions (Linux)
4. Try different serial ports

**Module not responding:**
1. Verify power supply (4.2V, 2A required)
2. Check SIM card insertion
3. Wait 10-15 seconds after power on
4. Restart the module

### Message Issues

**Cannot send Unicode:**
- Automatic PDU encoding is enabled
- Check SIM card supports Unicode SMS

**Message too long:**
- English/ASCII: max 160 characters
- Unicode: max 70 characters
- Emoji counted correctly (no concatenation support)

## Building

```bash
# Compile
mvn clean compile

# Build JAR
mvn clean package

# Run
mvn javafx:run
```

Output: `target/SIM800LManager-v2.0.jar`

## Project Structure

```
src/main/java/com/sim800l/
├── SIM800LApp.java              # Main application
├── SerialPortManager.java       # Serial communication
├── PDUEncoder.java              # PDU encoding
├── MessageEncoder.java          # Text encoding
├── DataManager.java             # Data persistence
├── Contact.java                 # Contact model
├── ChatMessage.java             # Message model
├── NotificationManager.java     # Notifications
└── Theme.java                   # UI theming
```

## Dependencies

- [JavaFX 17.0.2](https://openjfx.io/) - UI framework
- [jSerialComm 2.10.4](https://github.com/Fazecast/jSerialComm) - Serial communication

## Contributing

Contributions are welcome. Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Support

- Issues: [GitHub Issues](../../issues)
- Security: [SECURITY.md](SECURITY.md)

---

Made with care for reliable SMS communication.
