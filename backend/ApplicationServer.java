package backend;

// == IMPORTS ======================================
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.java_websocket.*;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

// == APPLICATION SERVER ===============================
public class ApplicationServer extends WebSocketServer {

  // == SERVER INSTANCE INFO ===========================

  int serverPort;
  WebSocket agentSocket;
  Set<WebSocket> clients;

    // == CONSOLE CONSTANTS [FOR READABILITY] ==================


    static public final int ERROR = 0;
    static public final int OKAY = 1;
    static public final int MESSAGE = 2;
    static public final int INTERESTING = 3;


  // == CONSTANT ENDPOINT APIS =========================

  private static String SEND_WEBCAM_API = "/send-cam-data";
  private static String GET_WEBCAM_DATA_API = "/get-cam-data";

  // == MAIN (TO BE RUN ON AWS) ========================

  public static void main(String[] args) {
    System.out.print("WHAT PORT? ");
    inititializeServer(new Scanner(System.in).nextInt());
  }

  // == FACTORY METHOD =================================
  // technically doesnt return object.. might change that later?

  public static void inititializeServer(int requestedPort) {

    // Check if the server can be run of a specified port.

    boolean portAvailible = false;

    try (ServerSocket socket = new ServerSocket(requestedPort)) {

      portAvailible = true;

    } catch (Exception e) {

      System.out.println("port " + requestedPort + " is... OCCUPIED!");

    }

    if (portAvailible) {

      // Build the server object.
      ApplicationServer applicationServer = new ApplicationServer(requestedPort);

      // Start te server.

      applicationServer.start();
    }
  }

  // == CONSTRUCTOR ========================================

  public ApplicationServer(int serverPort) {
    super(new InetSocketAddress(serverPort));
    this.serverPort = serverPort;
    clients = new HashSet<>();
  }

  // == WEBSOCKET SERVER METHODS ============================

  // What to do when someone connects.

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {

    // Identify agent based on first message (or other logic)
    if (handshake.getResourceDescriptor().contains("agent")) { // Optional condition
      agentSocket = conn;
      reportToConsole("AGENT CONNECTED", INTERESTING);
    } 
    else {
      clients.add(conn);
      reportToConsole("NEW CLIENT CONNECTED", INTERESTING);
    }

  }

  public static void reportToConsole(String message, int STATUS) {

    String colour;

    // Determine color based on message status.

    if (ERROR == STATUS) {
        colour = "\u001B[31m";
    } else if (OKAY == STATUS) { // OKAY
        colour = "\u001B[32m";
    } else if (INTERESTING == STATUS) {
        colour = "\u001B[34m";
    } else {
        colour = "\u001B[37m";
    }

    // Print formatted message (reset code at the end).

    System.out.println(colour + message + "\u001B[0m");
}

  // What to do on disconnection.

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    if (conn.equals(agentSocket)) {
      agentSocket = null;
      reportToConsole("AGENT DISCONNECTED", ERROR);
    } 
    else {
      clients.remove(conn);
      reportToConsole("A CLIENT DISCONNECTED", ERROR);

    }

  }

  // What to do when a message is recieved.

  @Override
  public void onMessage(WebSocket conn, String message) {

    if (isBase64EncodedImage(message)) {
      // System.out.println("Base64 image received, broadcasting to all clients...");
      broadcast(message);

    } else if ("FEED".equals(message)) {
      System.out.print("ACTION | ");
      reportToConsole("FEEDING BEASTS", INTERESTING);
      agentSocket.send(message);
    }

  }

  private boolean isBase64EncodedImage(String message) {

    if (message.startsWith("data:image/")) {
      message = message.split(",")[1]; // Get the Base64 part after the comma
    }

    // Regex pattern to match valid Base64-encoded data
    String base64Pattern = "^[A-Za-z0-9+/=]+$";

    // Return true if the message matches the Base64 image pattern
    return Pattern.matches(base64Pattern, message);
  }

  @Override
  public void onStart() {
    System.out.print("SERVER OPEN | ");
    reportToConsole("SUCCESS", OKAY);
    System.out.println("============================");
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    // todo later?
  }
}
