package backend;

// == IMPORTS ======================================
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.java_websocket.*;
import java.util.Scanner;
import java.util.regex.Pattern;

// == APPLICATION SERVER ===============================
public class ApplicationServer extends WebSocketServer {

  // == SERVER INSTANCE INFO ===========================

  int serverPort;

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
  }

  // == WEBSOCKET SERVER METHODS ============================

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    System.out.println("SOMEONE CONNECTED");

  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {

  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    if (isBase64EncodedImage(message)) {
      System.out.println("Base64 image received, broadcasting to all clients...");
      broadcast(message); // Send the Base64 image to all connected clients
    } else {
      System.out.println("Invalid Base64 image data.");
      System.out.println(message.substring(0, 20));
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
    System.out.println("SERVER IS NOW STARTED");
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    // todo later?
  }
}
