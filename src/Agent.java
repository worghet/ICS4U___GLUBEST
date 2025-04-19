// == IMPORTS ======================================
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.client.WebSocketClient;
import com.fazecast.jSerialComm.SerialPort;
import java.time.format.DateTimeFormatter;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import com.github.sarxos.webcam.*;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;
import java.util.Scanner;
import java.util.Base64;
import java.net.URI;

// == AGENT ========
public class Agent {


    // == CLASS CONSTANTS ========================================


    private static final String AGENT = "   AGENT  ";
    private static final String SERIAL_PORT_NAME = "/dev/ttyACM0"; // linux-specific.
    public static final String FEED_KEYWORD = "FEED";
    private static final int WEBCAM_SENDING_FPS = 15;


    // == FUNCTIONAL CONSTANTS ===================================


    final static Scanner input = new Scanner(System.in);


    // == INSTANCE VARIABLES =====================================


    WebSocketClient webSocketClient; // Websocket client itself.
    Thread webcamSendingThread; // Separate thread meant to send webcam data.
    SerialPort serialPort; // Port access to write to Arduino.
    FeederData feederData; // The data which is sent to server.
    Webcam webcam; // Webcam object.


    // == MAIN ====================================================


    // This main will be run on the agent computer.
    public static void main(String[] args) {

        // Setup agent object.
        System.out.println("|========================================================|");
        System.out.println("|===== #AGENT =====|=============== SETUP ===============|");

        Agent agent = createAgent();

        // If the initialization was successful.
        if (agent != null) {

            // Start a thread which checks connection, and
            // reconnects if detects that its lost connection.
            new Thread(() -> {

                // Variable for readability.
                int reconnectAttempts = 0;
    
                // Initial pause (to allow for initial connection attempt).
                try { Thread.sleep(5000); } catch (InterruptedException e) {}

                // This will always run.
                while (true) {
    
                    // If the connection is closed.
                    // (i.e. Needs to reconnect.)
                    if (!agent.webSocketClient.isOpen()) {
                        
                        // Uptick the number of reconnection ticks.
                        reconnectAttempts++;

                        // Re-initialize the client.
                        agent.initializeWebSocketClient();

                        // Notify console of reconnection attempt.
                        ServerManager.consolePrint(AGENT, "[" + reconnectAttempts + "] ATTEMPTING RECONNECTION", ServerManager.YELLOW);
                        
                        // Attempt re-connection.
                        agent.startOperating();
                    } 
                    
                    // If is connected.
                    else {

                        // Reset attempts.
                        reconnectAttempts = 0;

                    }
    
                    // Pause for 30 seconds (30 000 milliseconds).
                    try { Thread.sleep(30000); } catch (Exception e) {}
                }
            }).start(); // Start this thread.

            // Start the agent.
            agent.startOperating();

            // Begin logging details.
            System.out.println("|===== #AGENT =====|================ LOG ================|");
        } 
        
        // If it is null; report it, dont do anything.
        else {
            ServerManager.consolePrint(AGENT, "AGENT IS NULL", ServerManager.RED);
        }

    }

    public void startOperating() {

        // Connect to the server.
        webSocketClient.connect();

        // Start sending the webcam data (if connection successful).
        if (webSocketClient.isOpen()) {
            webcamSendingThread.start();
        }

    }


    // == OBJECT FACTORY ===========================================


    public static Agent createAgent() {

        // Create default agent object.
        Agent agent = new Agent();

        // Ensure that all have been initialized properly: Serial Port (Arduino), Webcam, and WebsocketClient.
        if (agent.initializedSerialPort() && agent.initializedWebcam() && agent.initializeWebSocketClient()) {

            // Initialize feederdata.
            agent.feederData = new FeederData();

            // Report that the construction (initialization) of the agent was successful.
            ServerManager.consolePrint(AGENT, "CONSTRUCTION OK", ServerManager.GREEN);

            // If all initiallization was good, return the agent as a new object.
            return agent;

        } else {

            // Otherwise, print error message.
            ServerManager.consolePrint(AGENT, "CONSTRUCTION FAILED", ServerManager.RED);

        }

        return null;

    }


    // == INITIALIZOR METHODS ======================================


    private boolean initializedSerialPort() {

        try {

            // Save access of serial port.
            serialPort = SerialPort.getCommPort(SERIAL_PORT_NAME);

            // Setup serial port (parameters: int baud rate, data size in bits, num stop bits, parity bits).
            serialPort.setComPortParameters(9600, 8, 1, 0);
            
            // Open the port.
            serialPort.openPort();
            
            // Let console know that all went well.
            ServerManager.consolePrint(AGENT, "SERIAL PORT OK", ServerManager.GREEN);

        }
        
        // If there's any error
        catch (Exception exception) {
            ServerManager.consolePrint(AGENT, "SERIAL PORT FAILED", ServerManager.RED);
            return false;
        }

        return true;
    }

    private boolean initializedWebcam() {

        try {

            // Get the default camera (whichever one can be found).
            webcam = Webcam.getDefault();

            // Set resoulution to highest possible one (for mine it was like 600x400).
            webcam.setViewSize(WebcamResolution.VGA.getSize());

            // Open (start) the webcam.
            webcam.open();

        } 
        
        // If anything goes wrong, let console know.
        catch (Exception exception) {
            ServerManager.consolePrint(AGENT, "WEBCAM FAILED", ServerManager.RED);
            return false;
        }

        // Return true to indicate that the initialization was successful.
        ServerManager.consolePrint(AGENT, "WEBCAM OK", ServerManager.GREEN);
        return true;

    }

    public void performArduinoRotation() {

        try {

            // Write the keyword into the the serial port.
            serialPort.getOutputStream().write(FEED_KEYWORD.getBytes());

            // Force the output to be written.
            serialPort.getOutputStream().flush();

        } catch (Exception exception) {
            System.out.println("sum send went wrong");
        }
    }

    public boolean initializeWebSocketClient() {
        try {

            // Recognize the server location (via URI) + Identify that this is the agent (more efficient).
            String global = "ws://18.218.44.44:8090/agent";
            String local = "ws://10.0.0.198:8090/agent";
            URI serverUri = URI.create(global); // "ws://18.218.44.44:8090/agent"

            // Initialize the websocket client.
            webSocketClient = new WebSocketClient(serverUri) {

                // Method for what to do when initially when connected to server.

                @Override
                public void onOpen(ServerHandshake handshake) {
                    
                    // Let console know that connection was successful.
                    ServerManager.consolePrint(AGENT, "WEBSOCKET CONNECTION OK", ServerManager.GREEN);
                
                    // If a previous webcam thread was running, stop it.
                    if (webcamSendingThread != null && webcamSendingThread.isAlive()) {
                        webcamSendingThread.interrupt();
                        ServerManager.consolePrint(AGENT, "PREVIOUS CAMERA THREAD INTERRUPTED", ServerManager.YELLOW);
                    }
                
                    // Create and start a new webcam sending thread.
                    webcamSendingThread = new Thread(() -> {
                
                        try {
                
                            // While this thread is allowed to run (is not interrupted)
                            while (!Thread.currentThread().isInterrupted()) {
                
                                // Take a picture with the webcam.
                                BufferedImage image = webcam.getImage();

                                // Write the image in a base64 format.
                                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                ImageIO.write(image, "JPEG", byteArrayOutputStream);
                                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                                String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                                feederData.encodedImage = base64Image;
                
                                // Actually send the image.
                                webSocketClient.send(feederData.toJsonString());
                                
                                // Pause the sending (to accomodate for desired FPS)
                                Thread.sleep(1000 / WEBCAM_SENDING_FPS);
                            }
                        } 
                        
                        // If anything goes wrong, let console know.
                        catch (Exception e) {
                            ServerManager.consolePrint(AGENT, "CAMERA SENDING FAILED: " + e.getMessage(), ServerManager.RED);
                        }
                    });
                
                    // Start the thread we just made.
                    webcamSendingThread.start();
                }
                

                // Method for what to do when a message is recieved.

                @Override
                public void onMessage(String message) {

                    // Understand what the message was.

                    switch (message) {

                        // If the message was "FEED", then tell the arduino to rotate.
                        case FEED_KEYWORD:

                            // Rotate the motor.
                            performArduinoRotation();

                            // Update the "last fed" variable in the feederData
                            feederData.formattedLastTimeFed = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss (dd / MM / yyyy)"));

                            // Let console know that feeding was complete.
                            ServerManager.consolePrint(AGENT, "FEEDING COMPLETE @ " + feederData.formattedLastTimeFed, ServerManager.BLUE);

                            break;

                        // If a new user tuned in, increment the viewer count.    
                        case "ADD_WATCHER":
                            feederData.viewerCount++;
                            break;

                        // If a user logged off, reduce the viewer count.    
                        case "REMOVE_WATCHER":
                            feederData.viewerCount--;
                            break;
                    }
                }

                // Method for what to do on disconnection.

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    ServerManager.consolePrint(AGENT, "WEBSOCKET DISCONNECTED", ServerManager.RED);
                }


                // Method for what to do on error.

                @Override
                public void onError(Exception ex) {}
            };

        }

        // If anything goes wrong, print issues with server connection.
        catch (Exception e) {
            ServerManager.consolePrint(AGENT, "FAILED TO CONNECT TO SERVER", ServerManager.RED);
            return false;
        }

        return true;

    }


    // == FEEDERDATA CLASS (DATA) ===================================

    static class FeederData {

        String type = "FEEDER_DATA"; // For ID-ing a serialized version of this.
        int viewerCount;
        String encodedImage; // In base64.
        String formattedLastTimeFed;

        public FeederData() {
            viewerCount = 0;
            encodedImage = "";
            formattedLastTimeFed = "HAS NOT BEEN FED YET";
        }

        public String toJsonString() {
            return ServerManager.gson.toJson(this);
        }

    }

}