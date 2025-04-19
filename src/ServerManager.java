// == IMPORTS ======================================
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.ServerSocket;
import java.io.OutputStream;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.io.File;

// == SERVER MANAGER =======
public class ServerManager {
    
    // Actual Servers and their respective ports.

    private static HttpServer httpServer;
    private static WebSocketServer webSocketServer;
    private static int HTTPSERVER_PORT = 8000;
    private static int WEBSOCKET_PORT = 8090;

    // Some useful stuff.

    final static Gson gson = new Gson(); // To interpret stuff.
    private static ArrayList<WebSocket> activeWatchers = new ArrayList<>(); // Client connections.
    private static WebSocket agentSocket; // Connection to the agent.

    // Endpoints for HTTP server

    private static final String MAIN_PAGE_API = "/glubest";
    private static final String FEEDER_PAGE_API = "/feeder";
    private static final String FEED_REQUEST_API = "/feed-request";

    // Logging codes.

    public static final String HTTPSERVER = "HTTPSERVER";
    public static final String WEBSOCKET = "WEBSOCKET ";
    public static final String COLOUR_RESET_CONSTANT = "\u001B[0m";
    public static final String RED = "\033[1;31m";
    public static final String GREEN = "\033[1;32m";
    public static final String YELLOW = "\033[1;33m";
    public static final String BLUE = "\033[1;34m";
    public static final String PURPLE = "\033[1;35m";
    public static final String CYAN = "\033[1;36m";
    public static final String WHITE = "\033[1;37m";

    // Main to be run on the remote server (AWS).
    public static void main(String[] args) {

        // Log setup.
        System.out.println("|========================================================|");
        System.out.println("|===== SERVER =====|=============== SETUP ===============|");

        // Start servers.
        initializeServers();

        // Log "log"s.
        System.out.println("|===== SERVER =====|================ LOG ================|");

    }

    public static void consolePrint(String LOGGED_SERVER_CONSTANT, String message, String COLOUR_CONSTANT) {

        // Print the message in the requested color with a preset.
        System.out.println("|    " + WHITE + LOGGED_SERVER_CONSTANT + "   " + COLOUR_RESET_CONSTANT + " | "
                + COLOUR_CONSTANT + message + COLOUR_RESET_CONSTANT);

    }

    private static boolean isPortAvailable(int requestedPort) {

        boolean portAvailible;

        // Essentially try to run a server on that port first and see if anything goes wrong.
        try (ServerSocket socket = new ServerSocket(requestedPort)) {

            portAvailible = true;

        } 

        // If anything goes wrong, assume the port is taken.
        catch (Exception e) {

            portAvailible = false;

        }

        return portAvailible;
    }

    private static void initializeServers() {

        // Initialize the HTTP server.
        initializeHttpServer();

        // Initialize the WebSocket server.
        initializeWebSocketServer();

    }

    private static void initializeHttpServer() {

        // Ensure that the port is availible.
        if (isPortAvailable(HTTPSERVER_PORT)) {

            // Start attempt to setup the server.
            try {

                // Create the server.
                httpServer = HttpServer.create(new InetSocketAddress(HTTPSERVER_PORT), 0);

                // Add endpoints.

                httpServer.createContext("/", new StaticFileHandler());

                httpServer.createContext(FEED_REQUEST_API, new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        
                        // Notify agent of request.
                        agentSocket.send(Agent.FEED_KEYWORD);

                        // Can simplify by just sending 200 code.
                        String response = "Feed endpoint reached!";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                    }
                });

                // Not too sure what this is, keeping it here from last project.
                httpServer.setExecutor(null);

                // Start the server.
                httpServer.start();

                // Log the initialization; address is currently hardcoded.
                consolePrint(HTTPSERVER, "OK (" + "18.218.44.44:" + HTTPSERVER_PORT + ")", GREEN);

            }
             
            // If anything goes wrong, let console know.
            catch (Exception exception) {
                consolePrint(HTTPSERVER, "FAILED (" + exception.getMessage() + ")", GREEN);
            }

        } 
        
        // If server port is unavailible, let console know.
        else {
            consolePrint(HTTPSERVER, "FAILED (PORT UNAVAILABLE)", RED);
        }
    }

    static class StaticFileHandler implements HttpHandler {

        // *** CHATGPT WROTE THIS HANDLE SEGMENT ***

        @Override
        public void handle(HttpExchange exchange) throws IOException {
    
            // Get the requested path from the URL.
            String requestedPath = exchange.getRequestURI().getPath();
    
            // Route remapping.
            switch (requestedPath) {
                case "/":
                case MAIN_PAGE_API:
                    requestedPath = "index.html";
                    break;
                case FEEDER_PAGE_API:
                    requestedPath = "feeder/feeder.html";
                    break;
                default:
                    if (requestedPath.startsWith("/resources/")) {
                        requestedPath = "resources/" + requestedPath.substring("/resources/".length());
                    }
                    break;
            }
    
            // Locate the file.
            String projectRoot = System.getProperty("user.dir");
            File file = new File(projectRoot, "frontend/" + requestedPath);
            System.out.println("User requested: " + file.getAbsolutePath());
    
            // Ensure the file exists.
            if (file.exists() && !file.isDirectory()) {
    
                // Guess MIME type.
                String contentType = guessContentType(file.getName());
                exchange.getResponseHeaders().add("Content-Type", contentType);
    
                // Enable byte-range support (important for media seeking)
                exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
    
                // Send 200 OK and the file
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(file.toPath(), os);
                }
    
            } else {
                // Handle 404 not found
                String response = "404 - File Not Found";
                exchange.sendResponseHeaders(404, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    
        // Guess the correct MIME type for the given file extension
        private String guessContentType(String fileName) {
            fileName = fileName.toLowerCase();
            if (fileName.endsWith(".html")) return "text/html";
            if (fileName.endsWith(".css")) return "text/css";
            if (fileName.endsWith(".js")) return "application/javascript";
            if (fileName.endsWith(".json")) return "application/json";
            if (fileName.endsWith(".png")) return "image/png";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            if (fileName.endsWith(".gif")) return "image/gif";
            if (fileName.endsWith(".mp3")) return "audio/mpeg";       // 🔥 Key addition
            if (fileName.endsWith(".wav")) return "audio/wav";
            if (fileName.endsWith(".mp4")) return "video/mp4";
            return "application/octet-stream"; // Default fallback
        }
    }

    private static void initializeWebSocketServer() {

        // Ensure the port is availible.
        if (isPortAvailable(WEBSOCKET_PORT)) {

            // Attempt initialization.
            try {

                // Initialize server.
                webSocketServer = new WebSocketServer(new InetSocketAddress(WEBSOCKET_PORT)) {

                    // What to do when someone connects.

                    @Override
                    public void onOpen(WebSocket conn, ClientHandshake handshake) {

                        // Identify if the logged in entity is the agent | yes, this is a vulnerability.
                        if (handshake.getResourceDescriptor().contains("agent")) { 
                            consolePrint(WEBSOCKET, "AGENT CONNECTED", BLUE);
                            agentSocket = conn;
                        } 
                        
                        // If its just a plain user.
                        else {

                            // Add them to the list of online users.
                            activeWatchers.add(conn);

                            // Notify console of new connection.
                            consolePrint(WEBSOCKET, "CLIENT CONNECTED", CYAN);
                            
                            // Update the feeder data to increment viewers.
                            agentSocket.send("ADD_WATCHER");
                        }

                    }

                    // What to do if a connection is closed.

                    @Override
                    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

                        // If the disconnected entity is the agent socket.
                        if (agentSocket.equals(conn)) {                            
                            agentSocket = null;
                            consolePrint(WEBSOCKET, "AGENT DISCONNECTED", YELLOW);
                        } 
                        
                        // If it is just a user. 
                        else {

                            // Notify console.
                            consolePrint(WEBSOCKET, "CLIENT DISCONNECTED", CYAN);
                            
                            // Reduce viewer count in feeder data.
                            agentSocket.send("REMOVE_WATCHER");

                            // Remove address from the list of active users.
                            activeWatchers.remove(conn);
                        }

                    }

                    // What to do when a message is recieved.

                    @Override
                    public void onMessage(WebSocket conn, String message) {

                        // Get the message type by deserializing it (I know, I know, could have optimized it here).
                        JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
                        String messageType = jsonObject.get("type").getAsString();

                        // Interpret what kind of message was recieved.
                        switch (messageType) {

                            // If it is just the feeder data.
                            case "FEEDER_DATA":
                                
                                // Send only to the active users; ignore the agent connection.
                                for (WebSocket watcher : activeWatchers) {
                                    if (!watcher.equals(agentSocket)) {
                                        watcher.send(message);
                                    }
                                }
                        }

                    }

                    @Override
                    public void onError(WebSocket conn, Exception ex) {}

                    @Override
                    public void onStart() {}

                };

                // Start the server.
                webSocketServer.start();

                // Notify that the server is up.
                consolePrint(WEBSOCKET, "OK", GREEN);

            } 
            
            // If anything goes wrong in the setup process, notify why.
            catch (Exception exception) {
                consolePrint(WEBSOCKET, "FAILED (" + exception.getMessage() + ")", GREEN);
            }

        } 
        
        // If port is unavailable, let console know.
        else {
            consolePrint(WEBSOCKET, "FAILED (PORT UNAVAILABLE)", RED);
        }

    }

}
