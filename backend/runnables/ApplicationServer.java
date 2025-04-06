package backend.runnables;

// == IMPORTS ======================================
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import backend.user.User;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import org.java_websocket.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

// == APPLICATION SERVER ===============================
public class ApplicationServer extends WebSocketServer {

  final static Gson gson = new Gson();

  // == SERVER INSTANCE INFO ===========================

  int serverPort;
  WebSocket agentSocket;
  // Set<WebSocket> clients;
  HashMap<WebSocket, User> activeUsers;

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
    activeUsers = new HashMap<>();
  }

  // == WEBSOCKET SERVER METHODS ============================

  // What to do when someone connects.

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {


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
    } else {
      if (User.CAT_WATCHING.equals(activeUsers.get(conn).getCurrentlyDoing())) {
        agentSocket.send("REMOVE_WATCHER");
        System.out.println("removing watcher");
      }
      activeUsers.remove(conn);
      reportToConsole("A CLIENT DISCONNECTED", ERROR);

    }

  }

  // What to do when a message is recieved.

  @Override
  public void onMessage(WebSocket conn, String message) {

    JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
    String messageType = jsonObject.get("type").getAsString();

    switch (messageType) {
      case "FEEDER_DATA":
        // broadcast only to WATCHERS
        for (WebSocket userSocket : activeUsers.keySet()) {
          User user = activeUsers.get(userSocket);
          if (User.CAT_WATCHING.equals(user.getCurrentlyDoing())) {  // Send to Watchers only
              userSocket.send(message);  // Send the feed data
          }
      }
        

        break;

      case "FEED_REQUEST":
        System.out.print("ACTION | ");
        reportToConsole("FEEDING BEASTS", INTERESTING);
        agentSocket.send("FEED");
        break;

      case "ROLE_ASSIGNMENT":

        activeUsers.get(conn).setCurrentlyDoingTo(jsonObject.get("role").getAsString());
        agentSocket.send("ADD_WATCHER");
        System.out.println("adding watcher");
        break;

      default:
        break;
    }

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
