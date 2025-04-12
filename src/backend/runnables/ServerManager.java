package backend.runnables;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import backend.user.User;

public class ServerManager {

    final static Gson gson = new Gson();
    private static ArrayList<WebSocket> activeWatchers = new ArrayList<>();
    private static WebSocket agentSocket;

    // Actual Servers.

    private static HttpServer httpServer;
    private static WebSocketServer webSocketServer;

    private static int HTTPSERVER_PORT = 8000;
    private static int WEBSOCKET_PORT = 8090;

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

    public static void main(String[] args) {

        System.out.println("|========================================================|");
        System.out.println("|===== SERVER =====|=============== SETUP ===============|");

        initializeServers();

        System.out.println("|===== SERVER =====|================ LOG ================|");

        // start servers here

    }

    public static void consolePrint(String LOGGED_SERVER_CONSTANT, String message, String COLOUR_CONSTANT) {

        System.out.println("|    " + WHITE + LOGGED_SERVER_CONSTANT + "   " + COLOUR_RESET_CONSTANT + " | "
                + COLOUR_CONSTANT + message + COLOUR_RESET_CONSTANT);

    }

    private static boolean isPortAvailable(int requestedPort) {

        boolean portAvailible;

        try (ServerSocket socket = new ServerSocket(requestedPort)) {

            portAvailible = true;

        } catch (Exception e) {

            portAvailible = false;

        }

        return portAvailible;
    }

    private static void initializeServers() {

        initializeHttpServer();

        initializeWebSocketServer();

    }

    private static final String MAIN_PAGE_API = "/glubest";
    private static final String FEEDER_PAGE_API = "/feeder";

    private static final String FEED_REQUEST_API = "/feed-request";

    private static void initializeHttpServer() {

        if (isPortAvailable(HTTPSERVER_PORT)) {

            try {

                httpServer = HttpServer.create(new InetSocketAddress(HTTPSERVER_PORT), 0);

                // add apis:

                httpServer.createContext("/", new StaticFileHandler());

                httpServer.createContext("/feed-request", new HttpHandler() {
                    @Override
                    public void handle(HttpExchange exchange) throws IOException {
                        String response = "Feed endpoint reached!";

                        agentSocket.send(Agent.FEED_KEYWORD);

                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                    }
                });

                httpServer.setExecutor(null);

                httpServer.start();
                consolePrint(HTTPSERVER, "OK (AVAILABLE AT [LINK GOES HERE])", GREEN);

            } catch (Exception exception) {
                consolePrint(HTTPSERVER, "FAILED (" + exception.getMessage() + ")", GREEN);
            }

        } else {
            consolePrint(HTTPSERVER, "FAILED (PORT UNAVAILABLE)", RED);
        }
    }

    static class StaticFileHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            // GET REQUESTED PATH
            String requestedPath = exchange.getRequestURI().getPath();

            switch (requestedPath) {
                case "/":
                case "/glubest":
                    requestedPath = "index.html";
                    break;
                case "/feeder":
                    requestedPath = "feeder/feeder.html";
                    break;
                case "/chatroom":
                    requestedPath = "chatroom/chatroom.html";
                    break;
                default:

                    if (requestedPath.startsWith("/resources/")) {
                        // Extract image name (e.g., "/dev-photos/3.jpg")
                        requestedPath = "resources/" + requestedPath.substring(11);
                    }
                    // Leave as is; assume it's a direct file reference
                    break;
            }

            String projectRoot = System.getProperty("user.dir");
            System.out.println("user requested: " + projectRoot + "/frontend/" + requestedPath);

            File file = new File(projectRoot, "/frontend/" + requestedPath);

            if (file.exists() && !file.isDirectory()) {

                // Determine content type
                String contentType = guessContentType(file.getName());
                exchange.getResponseHeaders().add("Content-Type", contentType);

                exchange.sendResponseHeaders(200, file.length());

                try (OutputStream os = exchange.getResponseBody()) {
                    Files.copy(file.toPath(), os);
                }

            } else {
                String response = "404 - REQUESTED FILE NOT FOUND";
                exchange.sendResponseHeaders(404, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }

        // Utility method for content-type guessing
        private String guessContentType(String fileName) {
            if (fileName.endsWith(".html"))
                return "text/html";
            if (fileName.endsWith(".css"))
                return "text/css";
            if (fileName.endsWith(".js"))
                return "application/javascript";
            if (fileName.endsWith(".json"))
                return "application/json";
            if (fileName.endsWith(".png"))
                return "image/png";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg"))
                return "image/jpeg";
            if (fileName.endsWith(".gif"))
                return "image/gif";
            return "application/octet-stream";
        }

    }

    private static void initializeWebSocketServer() {

        if (isPortAvailable(WEBSOCKET_PORT)) {

            try {

                webSocketServer = new WebSocketServer(new InetSocketAddress(WEBSOCKET_PORT)) {

                    @Override
                    public void onOpen(WebSocket conn, ClientHandshake handshake) {

                        // Identify agent based on first message (or other logic)
                        if (handshake.getResourceDescriptor().contains("agent")) { // Optional condition
                            consolePrint(WEBSOCKET, "AGENT CONNECTED", BLUE);
                            agentSocket = conn;
                        } else {

                            // prolly use login + session storage to recognize users on load

                            activeWatchers.add(conn);

                            consolePrint(WEBSOCKET, "CLIENT CONNECTED", CYAN);
                            agentSocket.send("ADD_WATCHER");
                        }

                    }

                    @Override
                    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

                        // System.out.println("onclose called");

                        if (agentSocket.equals(conn)) {
                            agentSocket = null;
                            consolePrint(WEBSOCKET, "AGENT DISCONNECTED", YELLOW);
                        } else {
                            consolePrint(WEBSOCKET, "CLIENT DISCONNECTED", CYAN);
                            agentSocket.send("REMOVE_WATCHER");
                            activeWatchers.remove(conn);
                        }

                    }

                    @Override
                    public void onMessage(WebSocket conn, String message) {

                        JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
                        String messageType = jsonObject.get("type").getAsString();

                        switch (messageType) {
                            case "FEEDER_DATA":

                                System.out.println("feeder message size: " + message.length());
                                for (WebSocket watcher : activeWatchers) {
                                    if (!watcher.equals(agentSocket)) {
                                        watcher.send(message);
                                    }
                                }
                                // broadcast only to WATCHERS (memory :::)
                                // for (WebSocket userSocket : activeUsers.keySet()) {
                                // User user = activeUsers.get(userSocket);
                                // if (User.CAT_WATCHING.equals(user.getCurrentlyDoing())) { // Send to Watchers
                                // only
                                // userSocket.send(message); // Send the feed data
                                // }
                                // }
                        }

                    }

                    @Override
                    public void onError(WebSocket conn, Exception ex) {
                    }

                    @Override
                    public void onStart() {
                    }

                };

                webSocketServer.start();
                consolePrint(WEBSOCKET, "OK", GREEN);

            } catch (Exception exception) {
                consolePrint(WEBSOCKET, "FAILED (" + exception.getMessage() + ")", GREEN);
            }

        } else {
            consolePrint(WEBSOCKET, "FAILED (PORT UNAVAILABLE)", RED);
        }

    }

}
