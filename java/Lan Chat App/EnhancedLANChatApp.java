import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;
import java.util.Map;

public class EnhancedLANChatApp extends JFrame {
    private static final String MULTICAST_ADDRESS = "230.0.0.1";
    private static final int PORT = 4446;
    private static final int TCP_PORT = 4447;
    private static final int WEB_PORT = 8080;
    private static final int BUFFER_SIZE = 8192;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB
    private static final String MESSAGE_PREFIX = "MSG:";
    private static final String FILE_PREFIX = "FILE:";
    private static final String SYSTEM_PREFIX = "SYS:";
    private static final String SESSION_FILES_DIR = "session_files";

    private MulticastSocket socket;
    private ServerSocket fileServerSocket;
    private ServerSocket webServerSocket;
    private InetAddress group;
    private NetworkInterface networkInterface;
    private String username;
    private String sessionId;

    // GUI Components
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;
    private JButton clearSessionButton;
    private JLabel statusLabel;
    private JScrollPane scrollPane;
    private JProgressBar progressBar;

    // File and session management
    private Map<String, FileInfo> sessionFiles = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();

    // Message receiver thread
    private Thread receiverThread;
    private Thread fileServerThread;
    private Thread webServerThread;
    private boolean isConnected = false;

    private static class FileInfo {
        String fileName;
        String filePath;
        String sender;
        long fileSize;
        String timestamp;
        String fileId;

        FileInfo(String fileName, String filePath, String sender, long fileSize, String timestamp) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.sender = sender;
            this.fileSize = fileSize;
            this.timestamp = timestamp;
            this.fileId = UUID.randomUUID().toString();
        }
    }

    public EnhancedLANChatApp() {
        sessionId = UUID.randomUUID().toString();
        createSessionDirectory();
        initializeGUI();
        setupNetworking();
        startFileServer();
        startWebServer();
    }

    private void createSessionDirectory() {
        try {
            Files.createDirectories(Paths.get(SESSION_FILES_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create session directory: " + e.getMessage());
        }
    }

    private void initializeGUI() {
        setTitle("Enhanced LAN Chat - File Sharing up to 5GB + Web UI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Create components
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 12));
        chatArea.setBackground(Color.WHITE);
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 12));
        messageField.setPreferredSize(new Dimension(300, 30));

        sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(80, 30));
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);

        fileButton = new JButton("📁 Send File (up to 5GB)");
        fileButton.setPreferredSize(new Dimension(160, 30));
        fileButton.setBackground(new Color(34, 139, 34));
        fileButton.setForeground(Color.WHITE);
        fileButton.setFocusPainted(false);

        clearSessionButton = new JButton("🗑️ Clear Session");
        clearSessionButton.setPreferredSize(new Dimension(120, 30));
        clearSessionButton.setBackground(new Color(220, 20, 60));
        clearSessionButton.setForeground(Color.WHITE);
        clearSessionButton.setFocusPainted(false);

        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        statusLabel.setForeground(Color.RED);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setVisible(false);

        // Layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(fileButton);
        buttonPanel.add(clearSessionButton);
        buttonPanel.add(sendButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(inputPanel, BorderLayout.NORTH);
        bottomPanel.add(progressBar, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Event listeners
        sendButton.addActionListener(e -> sendMessage());
        fileButton.addActionListener(e -> sendFile());
        clearSessionButton.addActionListener(e -> clearSession());
        messageField.addActionListener(e -> sendMessage());

        // Window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        // Initially disable send components
        messageField.setEnabled(false);
        sendButton.setEnabled(false);
        fileButton.setEnabled(false);
        clearSessionButton.setEnabled(false);
    }

    private void setupNetworking() {
        username = JOptionPane.showInputDialog(this,
                "Enter your username:",
                "Enhanced LAN Chat - Username",
                JOptionPane.QUESTION_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            username = "Anonymous";
        }
        username = username.trim();

        try {
            socket = new MulticastSocket(PORT);
            group = InetAddress.getByName(MULTICAST_ADDRESS);
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            socket.joinGroup(new InetSocketAddress(group, PORT), networkInterface);

            isConnected = true;
            String localIP = InetAddress.getLocalHost().getHostAddress();
            statusLabel.setText("Status: Connected as " + username + " | Web UI: http://" + localIP + ":" + WEB_PORT);
            statusLabel.setForeground(new Color(34, 139, 34));

            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            fileButton.setEnabled(true);
            clearSessionButton.setEnabled(true);
            messageField.requestFocus();

            startMessageReceiver();
            sendSystemMessage(username + " joined the chat");
            appendMessage("=== Enhanced LAN Chat with Large File Support ===");
            appendMessage("Connected as: " + username);
            appendMessage("Multicast Address: " + MULTICAST_ADDRESS + ":" + PORT);
            appendMessage("File Server: " + localIP + ":" + TCP_PORT);
            appendMessage("Web UI: http://" + localIP + ":" + WEB_PORT);
            appendMessage("Max file size: 5GB");
            appendMessage("Session ID: " + sessionId);
            appendMessage("=============================================");
        } catch (Exception e) {
            showError("Failed to connect to chat: " + e.getMessage());
            statusLabel.setText("Status: Connection Failed");
        }
    }

    private void startFileServer() {
        fileServerThread = new Thread(() -> {
            try {
                fileServerSocket = new ServerSocket(TCP_PORT);
                appendMessage("File server started on port " + TCP_PORT);
                while (isConnected && !fileServerSocket.isClosed()) {
                    try {
                        Socket clientSocket = fileServerSocket.accept();
                        executorService.submit(() -> handleFileClient(clientSocket));
                    } catch (IOException e) {
                        if (isConnected) {
                            appendMessage("File server error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                appendMessage("Failed to start file server: " + e.getMessage());
            }
        });
        fileServerThread.setDaemon(true);
        fileServerThread.start();
    }

    private void startWebServer() {
        webServerThread = new Thread(() -> {
            try {
                webServerSocket = new ServerSocket(WEB_PORT);
                appendMessage("Web server started on port " + WEB_PORT);
                while (isConnected && !webServerSocket.isClosed()) {
                    try {
                        Socket clientSocket = webServerSocket.accept();
                        executorService.submit(() -> handleWebClient(clientSocket));
                    } catch (IOException e) {
                        if (isConnected) {
                            appendMessage("Web server error: " + e.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                appendMessage("Failed to start web server: " + e.getMessage());
            }
        });
        webServerThread.setDaemon(true);
        webServerThread.start();
    }

    private void handleFileClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {
            String request = in.readLine();
            if (request != null && request.startsWith("GET_FILE:")) {
                String fileId = request.substring(9);
                FileInfo fileInfo = sessionFiles.get(fileId);
                if (fileInfo != null) {
                    File file = new File(fileInfo.filePath);
                    if (file.exists()) {
                        out.write("OK\n".getBytes());
                        Files.copy(file.toPath(), out);
                    } else {
                        out.write("ERROR: File not found\n".getBytes());
                    }
                } else {
                    out.write("ERROR: File ID not found\n".getBytes());
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling file client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void handleWebClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {
            // Read the entire HTTP request
            String requestLine = in.readLine();
            if (requestLine == null) return;

            // Read headers
            String line;
            StringBuilder headers = new StringBuilder();
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                headers.append(line).append("\n");
            }

            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String path = requestParts.length > 1 ? requestParts[1] : "/";

            // Add CORS headers to all responses
            String corsHeaders = "Access-Control-Allow-Origin: *\r\n" +
                    "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
                    "Access-Control-Allow-Headers: Content-Type\r\n";

            if (method.equals("OPTIONS")) {
                // Handle preflight requests
                String response = "HTTP/1.1 200 OK\r\n" + corsHeaders + "\r\n";
                out.write(response.getBytes());
                return;
            }

            if (method.equals("GET")) {
                if (path.equals("/") || path.equals("/index.html")) {
                    sendWebUI(out, corsHeaders);
                } else if (path.equals("/api/messages")) {
                    sendMessages(out, corsHeaders);
                } else if (path.equals("/api/files")) {
                    sendFileList(out, corsHeaders);
                } else if (path.startsWith("/api/download/")) {
                    String fileId = path.substring(14);
                    sendFileDownload(out, fileId, corsHeaders);
                } else {
                    send404(out, corsHeaders);
                }
            } else if (method.equals("POST")) {
                if (path.equals("/api/messages")) {
                    handleSendMessage(in, out, corsHeaders, headers.toString());
                } else if (path.equals("/api/clear-session")) {
                    handleClearSession(out, corsHeaders);
                } else {
                    send404(out, corsHeaders);
                }
            } else {
                send404(out, corsHeaders);
            }
        } catch (IOException e) {
            System.err.println("Error handling web client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void handleSendMessage(BufferedReader in, OutputStream out, String corsHeaders, String headers) throws IOException {
        // Find content length
        int contentLength = 0;
        for (String header : headers.split("\n")) {
            if (header.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(header.split(":")[1].trim());
                break;
            }
        }

        // Read the JSON body
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
        String jsonBody = new String(body);

        try {
            // Simple JSON parsing
            String message = extractJsonValue(jsonBody, "message");
            String webUsername = extractJsonValue(jsonBody, "username");

            if (message != null && !message.trim().isEmpty()) {
                // Send the message via multicast
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                String fullMessage = "[" + timestamp + "] " + webUsername + ": " + message;
                String dataToSend = MESSAGE_PREFIX + fullMessage;
                byte[] data = dataToSend.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
                socket.send(packet);

                // Also add to local chat
                SwingUtilities.invokeLater(() -> appendMessage(fullMessage));

                String response = "HTTP/1.1 200 OK\r\n" + corsHeaders +
                        "Content-Type: application/json\r\n\r\n" +
                        "{\"status\":\"success\"}";
                out.write(response.getBytes());
            } else {
                String response = "HTTP/1.1 400 Bad Request\r\n" + corsHeaders +
                        "Content-Type: application/json\r\n\r\n" +
                        "{\"error\":\"Message is required\"}";
                out.write(response.getBytes());
            }
        } catch (Exception e) {
            String response = "HTTP/1.1 500 Internal Server Error\r\n" + corsHeaders +
                    "Content-Type: application/json\r\n\r\n" +
                    "{\"error\":\"Failed to send message\"}";
            out.write(response.getBytes());
        }
    }

    private void handleClearSession(OutputStream out, String corsHeaders) throws IOException {
        try {
            // Clear session files
            for (FileInfo fileInfo : sessionFiles.values()) {
                if (fileInfo.sender.equals(username)) {
                    Files.deleteIfExists(Paths.get(fileInfo.filePath));
                }
            }
            sessionFiles.clear();
            sendSystemMessage(username + " cleared the session");
            SwingUtilities.invokeLater(() -> appendMessage("Session cleared - all files removed"));

            String response = "HTTP/1.1 200 OK\r\n" + corsHeaders +
                    "Content-Type: application/json\r\n\r\n" +
                    "{\"status\":\"success\"}";
            out.write(response.getBytes());
        } catch (Exception e) {
            String response = "HTTP/1.1 500 Internal Server Error\r\n" + corsHeaders +
                    "Content-Type: application/json\r\n\r\n" +
                    "{\"error\":\"Failed to clear session\"}";
            out.write(response.getBytes());
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;
        startIndex += searchKey.length();
        int endIndex = json.indexOf("\"", startIndex);
        if (endIndex == -1) return null;
        return json.substring(startIndex, endIndex);
    }

    private void sendWebUI(OutputStream out, String corsHeaders) throws IOException {
        String html = generateWebUI();
        String response = "HTTP/1.1 200 OK\r\n" + corsHeaders +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + html.length() + "\r\n\r\n" +
                html;
        out.write(response.getBytes());
    }

    private void sendMessages(OutputStream out, String corsHeaders) throws IOException {
        StringBuilder messages = new StringBuilder();
        String chatContent = chatArea.getText();
        messages.append(chatContent);

        String response = "HTTP/1.1 200 OK\r\n" + corsHeaders +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + messages.length() + "\r\n\r\n" +
                messages.toString();
        out.write(response.getBytes());
    }

    private void sendFileList(OutputStream out, String corsHeaders) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("[");
        boolean first = true;

        for (FileInfo fileInfo : sessionFiles.values()) {
            if (!first) json.append(",");
            json.append("{")
                    .append("\"id\":\"").append(fileInfo.fileId).append("\",")
                    .append("\"name\":\"").append(fileInfo.fileName).append("\",")
                    .append("\"sender\":\"").append(fileInfo.sender).append("\",")
                    .append("\"size\":").append(fileInfo.fileSize).append(",")
                    .append("\"timestamp\":\"").append(fileInfo.timestamp).append("\"")
                    .append("}");
            first = false;
        }
        json.append("]");

        String response = "HTTP/1.1 200 OK\r\n" + corsHeaders +
                "Content-Type: application/json\r\n" +
                "Content-Length: " + json.length() + "\r\n\r\n" +
                json.toString();
        out.write(response.getBytes());
    }

    private void sendFileDownload(OutputStream out, String fileId, String corsHeaders) throws IOException {
        FileInfo fileInfo = sessionFiles.get(fileId);
        if (fileInfo != null) {
            File file = new File(fileInfo.filePath);
            if (file.exists()) {
                String responseHeader = "HTTP/1.1 200 OK\r\n" + corsHeaders +
                        "Content-Type: application/octet-stream\r\n" +
                        "Content-Disposition: attachment; filename=\"" + fileInfo.fileName + "\"\r\n" +
                        "Content-Length: " + file.length() + "\r\n\r\n";
                out.write(responseHeader.getBytes());
                Files.copy(file.toPath(), out);
            } else {
                send404(out, corsHeaders);
            }
        } else {
            send404(out, corsHeaders);
        }
    }

    private void send404(OutputStream out, String corsHeaders) throws IOException {
        String response = "HTTP/1.1 404 Not Found\r\n" + corsHeaders +
                "Content-Type: text/html\r\n\r\n" +
                "<html><body><h1>404 Not Found</h1></body></html>";
        out.write(response.getBytes());
    }

private String generateWebUI() {
    return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>LAN Chat Mobile</title>\n" +
            "    <style>\n" +
            "        * {\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "            box-sizing: border-box;\n" +
            "        }\n" +
            "        body {\n" +
            "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n" +
            "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "            min-height: 100vh;\n" +
            "            color: #333;\n" +
            "        }\n" +
            "        .container {\n" +
            "            max-width: 800px;\n" +
            "            margin: 0 auto;\n" +
            "            padding: 10px;\n" +
            "            height: 100vh;\n" +
            "            display: flex;\n" +
            "            flex-direction: column;\n" +
            "        }\n" +
            "        .header {\n" +
            "            background: rgba(255, 255, 255, 0.95);\n" +
            "            backdrop-filter: blur(10px);\n" +
            "            border-radius: 20px;\n" +
            "            padding: 20px;\n" +
            "            margin-bottom: 15px;\n" +
            "            text-align: center;\n" +
            "            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);\n" +
            "        }\n" +
            "        .header h1 {\n" +
            "            color: #4a5568;\n" +
            "            font-size: 1.8em;\n" +
            "            margin-bottom: 5px;\n" +
            "        }\n" +
            "        .status {\n" +
            "            color: #68d391;\n" +
            "            font-weight: 500;\n" +
            "            font-size: 0.9em;\n" +
            "        }\n" +
            "        .chat-container {\n" +
            "            flex: 1;\n" +
            "            display: flex;\n" +
            "            flex-direction: column;\n" +
            "            background: rgba(255, 255, 255, 0.95);\n" +
            "            backdrop-filter: blur(10px);\n" +
            "            border-radius: 20px;\n" +
            "            overflow: hidden;\n" +
            "            box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);\n" +
            "        }\n" +
            "        .tabs {\n" +
            "            display: flex;\n" +
            "            background: #f7fafc;\n" +
            "            border-bottom: 1px solid #e2e8f0;\n" +
            "        }\n" +
            "        .tab {\n" +
            "            flex: 1;\n" +
            "            padding: 15px;\n" +
            "            text-align: center;\n" +
            "            background: none;\n" +
            "            border: none;\n" +
            "            cursor: pointer;\n" +
            "            font-weight: 500;\n" +
            "            color: #718096;\n" +
            "            transition: all 0.3s ease;\n" +
            "        }\n" +
            "        .tab.active {\n" +
            "            color: #4299e1;\n" +
            "            background: #ebf8ff;\n" +
            "            border-bottom: 2px solid #4299e1;\n" +
            "        }\n" +
            "        .tab-content {\n" +
            "            flex: 1;\n" +
            "            display: none;\n" +
            "            flex-direction: column;\n" +
            "        }\n" +
            "        .tab-content.active {\n" +
            "            display: flex;\n" +
            "        }\n" +
            "        .chat-area {\n" +
            "            flex: 1;\n" +
            "            padding: 20px;\n" +
            "            overflow-y: auto;\n" +
            "            background: #f8f9fa;\n" +
            "            font-family: 'SF Mono', Monaco, monospace;\n" +
            "            font-size: 0.9em;\n" +
            "            line-height: 1.6;\n" +
            "            white-space: pre-wrap;\n" +
            "            word-wrap: break-word;\n" +
            "        }\n" +
            "        .input-area {\n" +
            "            display: flex;\n" +
            "            gap: 10px;\n" +
            "            padding: 15px;\n" +
            "            background: #fff;\n" +
            "            border-top: 1px solid #e2e8f0;\n" +
            "        }\n" +
            "        .message-input {\n" +
            "            flex: 1;\n" +
            "            padding: 12px 15px;\n" +
            "            border: 2px solid #e2e8f0;\n" +
            "            border-radius: 25px;\n" +
            "            font-size: 16px;\n" +
            "            outline: none;\n" +
            "            transition: border-color 0.3s ease;\n" +
            "        }\n" +
            "        .message-input:focus {\n" +
            "            border-color: #4299e1;\n" +
            "        }\n" +
            "        .send-btn {\n" +
            "            padding: 12px 20px;\n" +
            "            background: linear-gradient(135deg, #4299e1, #3182ce);\n" +
            "            color: white;\n" +
            "            border: none;\n" +
            "            border-radius: 25px;\n" +
            "            cursor: pointer;\n" +
            "            font-weight: 500;\n" +
            "            transition: transform 0.2s ease;\n" +
            "        }\n" +
            "        .send-btn:hover {\n" +
            "            transform: translateY(-1px);\n" +
            "        }\n" +
            "        .send-btn:active {\n" +
            "            transform: translateY(0);\n" +
            "        }\n" +
            "        .file-section {\n" +
            "            padding: 20px;\n" +
            "        }\n" +
            "        .file-upload {\n" +
            "            margin-bottom: 20px;\n" +
            "            text-align: center;\n" +
            "        }\n" +
            "        .file-input-wrapper {\n" +
            "            position: relative;\n" +
            "            display: inline-block;\n" +
            "            cursor: pointer;\n" +
            "            background: linear-gradient(135deg, #48bb78, #38a169);\n" +
            "            color: white;\n" +
            "            padding: 15px 30px;\n" +
            "            border-radius: 25px;\n" +
            "            font-weight: 500;\n" +
            "            transition: transform 0.2s ease;\n" +
            "        }\n" +
            "        .file-input-wrapper:hover {\n" +
            "            transform: translateY(-2px);\n" +
            "        }\n" +
            "        .file-input {\n" +
            "            position: absolute;\n" +
            "            opacity: 0;\n" +
            "            width: 100%;\n" +
            "            height: 100%;\n" +
            "            cursor: pointer;\n" +
            "        }\n" +
            "        .file-list {\n" +
            "            background: #f8f9fa;\n" +
            "            border-radius: 15px;\n" +
            "            padding: 15px;\n" +
            "            max-height: 400px;\n" +
            "            overflow-y: auto;\n" +
            "        }\n" +
            "        .file-item {\n" +
            "            display: flex;\n" +
            "            justify-content: space-between;\n" +
            "            align-items: center;\n" +
            "            padding: 15px;\n" +
            "            margin-bottom: 10px;\n" +
            "            background: white;\n" +
            "            border-radius: 12px;\n" +
            "            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);\n" +
            "            transition: transform 0.2s ease;\n" +
            "        }\n" +
            "        .file-item:hover {\n" +
            "            transform: translateY(-1px);\n" +
            "        }\n" +
            "        .file-info {\n" +
            "            flex: 1;\n" +
            "        }\n" +
            "        .file-name {\n" +
            "            font-weight: 600;\n" +
            "            color: #2d3748;\n" +
            "            margin-bottom: 5px;\n" +
            "        }\n" +
            "        .file-meta {\n" +
            "            font-size: 0.8em;\n" +
            "            color: #718096;\n" +
            "        }\n" +
            "        .download-btn {\n" +
            "            padding: 8px 16px;\n" +
            "            background: #4299e1;\n" +
            "            color: white;\n" +
            "            border: none;\n" +
            "            border-radius: 20px;\n" +
            "            cursor: pointer;\n" +
            "            font-size: 0.8em;\n" +
            "            font-weight: 500;\n" +
            "        }\n" +
            "        .progress-bar {\n" +
            "            width: 100%;\n" +
            "            height: 4px;\n" +
            "            background: #e2e8f0;\n" +
            "            border-radius: 2px;\n" +
            "            margin: 10px 0;\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "        .progress-fill {\n" +
            "            height: 100%;\n" +
            "            background: linear-gradient(90deg, #4299e1, #3182ce);\n" +
            "            border-radius: 2px;\n" +
            "            transition: width 0.3s ease;\n" +
            "            width: 0%;\n" +
            "        }\n" +
            "        .clear-session-btn {\n" +
            "            width: 100%;\n" +
            "            padding: 15px;\n" +
            "            background: linear-gradient(135deg, #e53e3e, #c53030);\n" +
            "            color: white;\n" +
            "            border: none;\n" +
            "            border-radius: 15px;\n" +
            "            cursor: pointer;\n" +
            "            font-weight: 500;\n" +
            "            margin-top: 15px;\n" +
            "            transition: transform 0.2s ease;\n" +
            "        }\n" +
            "        .clear-session-btn:hover {\n" +
            "            transform: translateY(-1px);\n" +
            "        }\n" +
            "        .notification {\n" +
            "            position: fixed;\n" +
            "            top: 20px;\n" +
            "            right: 20px;\n" +
            "            padding: 15px 20px;\n" +
            "            background: #48bb78;\n" +
            "            color: white;\n" +
            "            border-radius: 10px;\n" +
            "            transform: translateX(400px);\n" +
            "            transition: transform 0.3s ease;\n" +
            "            z-index: 1000;\n" +
            "        }\n" +
            "        .notification.show {\n" +
            "            transform: translateX(0);\n" +
            "        }\n" +
            "        .connection-status {\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 8px;\n" +
            "            margin-top: 5px;\n" +
            "        }\n" +
            "        .status-dot {\n" +
            "            width: 8px;\n" +
            "            height: 8px;\n" +
            "            border-radius: 50%;\n" +
            "            background: #68d391;\n" +
            "            animation: pulse 2s infinite;\n" +
            "        }\n" +
            "        @keyframes pulse {\n" +
            "            0% { opacity: 1; }\n" +
            "            50% { opacity: 0.5; }\n" +
            "            100% { opacity: 1; }\n" +
            "        }\n" +
            "        @media (max-width: 600px) {\n" +
            "            .container {\n" +
            "                padding: 5px;\n" +
            "            }\n" +
            "            .header {\n" +
            "                padding: 15px;\n" +
            "                margin-bottom: 10px;\n" +
            "            }\n" +
            "            .header h1 {\n" +
            "                font-size: 1.4em;\n" +
            "            }\n" +
            "            .input-area {\n" +
            "                padding: 10px;\n" +
            "            }\n" +
            "            .file-item {\n" +
            "                flex-direction: column;\n" +
            "                align-items: flex-start;\n" +
            "                gap: 10px;\n" +
            "            }\n" +
            "            .download-btn {\n" +
            "                width: 100%;\n" +
            "            }\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>🚀 LAN Chat Mobile</h1>\n" +
            "            <div class=\"status\" id=\"connectionStatus\">\n" +
            "                <div class=\"connection-status\">\n" +
            "                    <div class=\"status-dot\"></div>\n" +
            "                    <span>Connected</span>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div class=\"chat-container\">\n" +
            "            <div class=\"tabs\">\n" +
            "                <button class=\"tab active\" onclick=\"switchTab('chat')\">💬 Chat</button>\n" +
            "                <button class=\"tab\" onclick=\"switchTab('files')\">📁 Files</button>\n" +
            "            </div>\n" +
            "            <div class=\"tab-content active\" id=\"chatTab\">\n" +
            "                <div class=\"chat-area\" id=\"chatArea\"></div>\n" +
            "                <div class=\"input-area\">\n" +
            "                    <input type=\"text\" class=\"message-input\" id=\"messageInput\"\n" +
            "                           placeholder=\"Type your message...\" maxlength=\"500\">\n" +
            "                    <button class=\"send-btn\" onclick=\"sendMessage()\">Send</button>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            <div class=\"tab-content\" id=\"filesTab\">\n" +
            "                <div class=\"file-section\">\n" +
            "                    <div class=\"file-upload\">\n" +
            "                        <div class=\"file-input-wrapper\">\n" +
            "                            <input type=\"file\" class=\"file-input\" id=\"fileInput\"\n" +
            "                                   onchange=\"handleFileSelect()\" multiple>\n" +
            "                            📁 Select Files (up to 5GB)\n" +
            "                        </div>\n" +
            "                        <div class=\"progress-bar\" id=\"uploadProgress\" style=\"display: none;\">\n" +
            "                            <div class=\"progress-fill\" id=\"progressFill\"></div>\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                    <div class=\"file-list\" id=\"fileList\">\n" +
            "                        <div style=\"text-align: center; color: #718096; padding: 20px;\">\n" +
            "                            No files shared yet\n" +
            "                        </div>\n" +
            "                    </div>\n" +
            "                    <button class=\"clear-session-btn\" onclick=\"clearSession()\">\n" +
            "                        🗑️ Clear Session\n" +
            "                    </button>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <div class=\"notification\" id=\"notification\"></div>\n" +
            "    <script>\n" +
            "        let username = 'Mobile User';\n" +
            "        let isConnected = false;\n" +
            "        // Initialize the app\n" +
            "        function init() {\n" +
            "            username = prompt('Enter your username:') || 'Mobile User';\n" +
            "            connectToChat();\n" +
            "            startPeriodicUpdates();\n" +
            "            // Enable enter key for sending messages\n" +
            "            document.getElementById('messageInput').addEventListener('keypress', function(e) {\n" +
            "                if (e.key === 'Enter') {\n" +
            "                    sendMessage();\n" +
            "                }\n" +
            "            });\n" +
            "        }\n" +
            "        function connectToChat() {\n" +
            "            fetch('/api/messages')\n" +
            "                .then(response => {\n" +
            "                    if (response.ok) {\n" +
            "                        isConnected = true;\n" +
            "                        updateConnectionStatus();\n" +
            "                        showNotification('Connected to LAN Chat!');\n" +
            "                    } else {\n" +
            "                        throw new Error('Failed to connect to chat server');\n" +
            "                    }\n" +
            "                })\n" +
            "                .catch(error => {\n" +
            "                    showNotification(`Connection error: ${error.message}`, 'error');\n" +
            "                });\n" +
            "        }\n" +
            "        function sendMessage() {\n" +
            "            const input = document.getElementById('messageInput');\n" +
            "            const message = input.value.trim();\n" +
            "            if (!message) return;\n" +
            "            fetch('/api/messages', {\n" +
            "                method: 'POST',\n" +
            "                headers: { 'Content-Type': 'application/json' },\n" +
            "                body: JSON.stringify({ username, message })\n" +
            "            })\n" +
            "                .then(response => {\n" +
            "                    if (response.ok) {\n" +
            "                        input.value = '';\n" +
            "                        showNotification('Message sent!');\n" +
            "                    } else {\n" +
            "                        throw new Error('Failed to send message');\n" +
            "                    }\n" +
            "                })\n" +
            "                .catch(error => {\n" +
            "                    showNotification(`Error sending message: ${error.message}`, 'error');\n" +
            "                });\n" +
            "        }\n" +
            "        function fetchMessages() {\n" +
            "            fetch('/api/messages')\n" +
            "                .then(response => response.text())\n" +
            "                .then(data => {\n" +
            "                    const chatArea = document.getElementById('chatArea');\n" +
            "                    chatArea.textContent = data;\n" +
            "                    chatArea.scrollTop = chatArea.scrollHeight;\n" +
            "                })\n" +
            "                .catch(error => {\n" +
            "                    showNotification(`Error fetching messages: ${error.message}`, 'error');\n" +
            "                });\n" +
            "        }\n" +
            "        function fetchFiles() {\n" +
            "            fetch('/api/files')\n" +
            "                .then(response => response.json())\n" +
            "                .then(files => {\n" +
            "                    const fileList = document.getElementById('fileList');\n" +
            "                    if (files.length === 0) {\n" +
            "                        fileList.innerHTML = `\n" +
            "                            <div style=\"text-align: center; color: #718096; padding: 20px;\">\n" +
            "                                No files shared yet\n" +
            "                            </div>\n" +
            "                        `;\n" +
            "                        return;\n" +
            "                    }\n" +
            "                    fileList.innerHTML = files.map(file => `\n" +
            "                        <div class=\"file-item\">\n" +
            "                            <div class=\"file-info\">\n" +
            "                                <div class=\"file-name\">${file.name}</div>\n" +
            "                                <div class=\"file-meta\">\n" +
            "                                    ${formatFileSize(file.size)} • from ${file.sender} • ${file.timestamp}\n" +
            "                                </div>\n" +
            "                            </div>\n" +
            "                            <button class=\"download-btn\" onclick=\"downloadFile('${file.id}')\">\n" +
            "                                Download\n" +
            "                            </button>\n" +
            "                        </div>\n" +
            "                    `).join('');\n" +
            "                })\n" +
            "                .catch(error => {\n" +
            "                    showNotification(`Error fetching files: ${error.message}`, 'error');\n" +
            "                });\n" +
            "        }\n" +
            "        function downloadFile(fileId) {\n" +
            "            fetch(`/api/download/${fileId}`)\n" +
            "                .then(response => {\n" +
            "                    if (response.ok) {\n" +
            "                        return response.blob();\n" +
            "                    } else {\n" +
            "                        throw new Error('File not found');\n" +
            "                    }\n" +
            "                })\n" +
            "                .then(blob => {\n" +
            "                    const url = window.URL.createObjectURL(blob);\n" +
            "                    const a = document.createElement('a');\n" +
            "                    a.href = url;\n" +
            "                    a.download = fileId; // Use the actual filename if available\n" +
            "                    a.click();\n" +
            "                    window.URL.revokeObjectURL(url);\n" +
            "                    showNotification('File downloaded successfully!');\n" +
            "                })\n" +
            "                .catch(error => {\n" +
            "                    showNotification(`Error downloading file: ${error.message}`, 'error');\n" +
            "                });\n" +
            "        }\n" +
            "        function handleFileSelect() {\n" +
            "            const fileInput = document.getElementById('fileInput');\n" +
            "            const files = fileInput.files;\n" +
            "            if (files.length > 0) {\n" +
            "                for (let file of files) {\n" +
            "                    if (file.size > 5 * 1024 * 1024 * 1024) { // 5GB limit\n" +
            "                        showNotification('File size exceeds 5GB limit!', 'error');\n" +
            "                        continue;\n" +
            "                    }\n" +
            "                    uploadFile(file);\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        function uploadFile(file) {\n" +
            "            showProgress(true);\n" +
            "            const formData = new FormData();\n" +
            "            formData.append('file', file);\n" +
            "            fetch('/api/upload', {\n" +
            "                method: 'POST',\n" +
            "                body: formData\n" +
            "            })\n" +
            "                .then(response => {\n" +
            "                    if (response.ok) {\n" +
            "                        showNotification(`File \"${file.name}\" uploaded successfully!`);\n" +
            "                    } else {\n" +
            "                        throw new Error('File upload failed');\n" +
            "                    }\n" +
            "                })\n" +
            "                .catch(error => {\n" +
            "                    showNotification(`Error uploading file: ${error.message}`, 'error');\n" +
            "                })\n" +
            "                .finally(() => {\n" +
            "                    showProgress(false);\n" +
            "                });\n" +
            "        }\n" +
            "        function clearSession() {\n" +
            "            if (confirm('This will clear all session files and cannot be undone. Continue?')) {\n" +
            "                fetch('/api/clear-session', { method: 'POST' })\n" +
            "                    .then(response => {\n" +
            "                        if (response.ok) {\n" +
            "                            showNotification('Session cleared!');\n" +
            "                        } else {\n" +
            "                            throw new Error('Failed to clear session');\n" +
            "                        }\n" +
            "                    })\n" +
            "                    .catch(error => {\n" +
            "                        showNotification(`Error clearing session: ${error.message}`, 'error');\n" +
            "                    });\n" +
            "            }\n" +
            "        }\n" +
            "        function startPeriodicUpdates() {\n" +
            "            setInterval(() => {\n" +
            "                fetchMessages();\n" +
            "                fetchFiles();\n" +
            "            }, 5000); // Fetch updates every 5 seconds\n" +
            "        }\n" +
            "        function showProgress(show) {\n" +
            "            document.getElementById('uploadProgress').style.display = show ? 'block' : 'none';\n" +
            "        }\n" +
            "        function updateProgressBar(percent) {\n" +
            "            document.getElementById('progressFill').style.width = percent + '%';\n" +
            "        }\n" +
            "        function showNotification(message, type = 'success') {\n" +
            "            const notification = document.getElementById('notification');\n" +
            "            notification.textContent = message;\n" +
            "            notification.style.background = type === 'error' ? '#e53e3e' : '#48bb78';\n" +
            "            notification.classList.add('show');\n" +
            "            setTimeout(() => {\n" +
            "                notification.classList.remove('show');\n" +
            "            }, 3000);\n" +
            "        }\n" +
            "        function updateConnectionStatus() {\n" +
            "            const status = document.getElementById('connectionStatus');\n" +
            "            if (isConnected) {\n" +
            "                status.innerHTML = `\n" +
            "                    <div class=\"connection-status\">\n" +
            "                        <div class=\"status-dot\"></div>\n" +
            "                        <span>Connected as ${username}</span>\n" +
            "                    </div>\n" +
            "                `;\n" +
            "            } else {\n" +
            "                status.innerHTML = `\n" +
            "                    <div class=\"connection-status\">\n" +
            "                        <div class=\"status-dot\" style=\"background: #e53e3e;\"></div>\n" +
            "                        <span>Disconnected</span>\n" +
            "                    </div>\n" +
            "                `;\n" +
            "            }\n" +
            "        }\n" +
            "        function formatFileSize(bytes) {\n" +
            "            if (bytes < 1024) return bytes + ' B';\n" +
            "            if (bytes < 1024 * 1024) return Math.round(bytes / 1024) + ' KB';\n" +
            "            if (bytes < 1024 * 1024 * 1024) return Math.round(bytes / (1024 * 1024)) + ' MB';\n" +
            "            return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB';\n" +
            "        }\n" +
            "        window.addEventListener('load', init);\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;

        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String fullMessage = "[" + timestamp + "] " + username + ": " + message;
        String dataToSend = MESSAGE_PREFIX + fullMessage;

        try {
            byte[] data = dataToSend.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
            socket.send(packet);
            appendMessage(fullMessage);
            messageField.setText("");
        } catch (IOException e) {
            showError("Failed to send message: " + e.getMessage());
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("All Files", "*");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.length() > MAX_FILE_SIZE) {
                showError("File size exceeds 5GB limit!");
                return;
            }

            try {
                String fileId = UUID.randomUUID().toString();
                String filePath = SESSION_FILES_DIR + "/" + fileId + "_" + file.getName();
                Files.copy(file.toPath(), Paths.get(filePath));

                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                FileInfo fileInfo = new FileInfo(file.getName(), filePath, username, file.length(), timestamp);
                sessionFiles.put(fileInfo.fileId, fileInfo);

                String fileMessage = "[" + timestamp + "] " + username + " shared a file: " + file.getName();
                sendSystemMessage(fileMessage);

                // Notify others about the file
                String dataToSend = FILE_PREFIX + fileInfo.fileId + ":" + fileInfo.fileName + ":" + fileInfo.fileSize;
                byte[] data = dataToSend.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
                socket.send(packet);
            } catch (IOException e) {
                showError("Failed to send file: " + e.getMessage());
            }
        }
    }

    private void clearSession() {
        for (FileInfo fileInfo : sessionFiles.values()) {
            if (fileInfo.sender.equals(username)) {
                try {
                    Files.deleteIfExists(Paths.get(fileInfo.filePath));
                } catch (IOException e) {
                    System.err.println("Failed to delete file: " + e.getMessage());
                }
            }
        }
        sessionFiles.clear();
        sendSystemMessage(username + " cleared the session");
        appendMessage("Session cleared - all files removed");
    }
private void shutdown() {
    isConnected = false;
    try {
        if (socket != null && !socket.isClosed()) {
            socket.leaveGroup(new InetSocketAddress(group, PORT), networkInterface); // Fixed line
            socket.close();
        }
        if (fileServerSocket != null && !fileServerSocket.isClosed()) {
            fileServerSocket.close();
        }
        if (webServerSocket != null && !webServerSocket.isClosed()) {
            webServerSocket.close();
        }
        executorService.shutdownNow();
    } catch (IOException e) {
        System.err.println("Error during shutdown: " + e.getMessage());
    }
}

private void startMessageReceiver() {
    receiverThread = new Thread(() -> {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (isConnected) {
            try {
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());

                if (received.startsWith(MESSAGE_PREFIX)) {
                    String message = received.substring(MESSAGE_PREFIX.length());
                    // Extract the sender's username from the message
                    String sender = message.split(":")[0].trim();
                    if (!sender.equals(username)) { // Ignore messages sent by the same user
                        SwingUtilities.invokeLater(() -> appendMessage(message));
                    }
                } else if (received.startsWith(FILE_PREFIX)) {
                    String fileInfo = received.substring(FILE_PREFIX.length());
                    String[] parts = fileInfo.split(":");
                    String fileId = parts[0];
                    String fileName = parts[1];
                    long fileSize = Long.parseLong(parts[2]);
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                    FileInfo info = new FileInfo(fileName, "", "Remote User", fileSize, timestamp);
                    sessionFiles.put(fileId, info);

                    SwingUtilities.invokeLater(() -> appendMessage("File received: " + fileName));
                } else if (received.startsWith(SYSTEM_PREFIX)) {
                    String message = received.substring(SYSTEM_PREFIX.length());
                    SwingUtilities.invokeLater(() -> appendMessage(message));
                }
            } catch (IOException e) {
                if (isConnected) {
                    showError("Error receiving message: " + e.getMessage());
                }
            }
        }
    });
    receiverThread.setDaemon(true);
    receiverThread.start();
}

    private void sendSystemMessage(String message) {
        String dataToSend = SYSTEM_PREFIX + message;
        try {
            byte[] data = dataToSend.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, group, PORT);
            socket.send(packet);
        } catch (IOException e) {
            showError("Failed to send system message: " + e.getMessage());
        }
    }

    private void appendMessage(String message) {
        chatArea.append(message + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EnhancedLANChatApp app = new EnhancedLANChatApp();
            app.setVisible(true);
        });
    }
}


