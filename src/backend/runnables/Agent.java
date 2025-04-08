package backend.runnables;

// == IMPORTS ======================================
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.client.WebSocketClient;
import com.fazecast.jSerialComm.SerialPort;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import com.github.sarxos.webcam.*;
import javax.imageio.ImageIO;
import java.util.Scanner;
import com.google.gson.*;
import java.util.Base64;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// == AGENT ========
public class Agent {

    // == CLASS CONSTANTS ========================================

    private static final String AGENT = "   AGENT  ";
    private static final String SERIAL_PORT_NAME = "/dev/ttyACM0"; // linux-specific.
    public static final String FEED_KEYWORD = "FEED";
    private static final int WEBCAM_SENDING_FPS = 5;

    // == FUNCTIONAL CONSTANTS ===================================

    final static Scanner input = new Scanner(System.in);

    // == INSTANCE VARIABLES =====================================

    WebSocketClient webSocketClient; // Websocket client itself.
    Thread webcamSendingThread; // Separate thread meant to send webcam data.
    SerialPort serialPort; // Port access to write to Arduino.
    Webcam webcam; // Webcam object.
    FeederData feederData;

    // == MAIN ====================================================

    // This main will be run on the agent computer.
    public static void main(String[] args) {

        
        // Setup agent object
        System.out.println("|========================================================|");
        System.out.println("|===== #AGENT =====|=============== SETUP ===============|");
        
        Agent agent = createAgent();



        if (agent != null) {
            agent.startOperating();
            System.out.println("|===== #AGENT =====|================ LOG ================|");
        } else {
            ServerManager.consolePrint(AGENT, "AGENT IS NULL", ServerManager.RED);
        }

    }

    public void startOperating() {

        // Connect to the server.
        webSocketClient.connect();

        // Start sending the webcam data.
        webcamSendingThread.start();

    }

    // == OBJECT FACTORY ===========================================

    public static Agent createAgent() {

        // Create default agent object.
        Agent agent = new Agent();

        // Initialize all components: serialPort, webcam, and the websocket connection;
        // only proceed if all are initialized.
        if (agent.initializedSerialPort() && agent.initializedWebcam() && agent.initializeWebSocketClient()) {

            agent.feederData = new FeederData();

            
            // Setup the thread for sending webcam data.
            agent.webcamSendingThread = new Thread() {
                public void run() {
                    try {

                        // Perform the following all the time.

                        while (true) {

                            // Read image from webcam.
                            BufferedImage image = agent.webcam.getImage();

                            // Take the image, and rewrite it in Base64; as we can't simply send png over
                            // network.
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            ImageIO.write(image, "PNG", byteArrayOutputStream);
                            byte[] imageBytes = byteArrayOutputStream.toByteArray();
                            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                            agent.feederData.encodedImage = base64Image;

                            // Send the encoded image to server.
                            agent.webSocketClient.send(agent.feederData.toJsonString());

                            // Pause sending (to prevent overflow) momentarily.
                            Thread.sleep(1000 / WEBCAM_SENDING_FPS);
                        }
                    } catch (Exception exception) {
                        ServerManager.consolePrint(AGENT, "CAMERA SENDING FAILED", ServerManager.RED);
                    }
                }
            };

            ServerManager.consolePrint(AGENT, "CONSTRUCTION OK", ServerManager.GREEN);

            // If all initiallization was good, return the agent as a new object.
            return agent;

        } 
        else {

            // Otherwise, print error message.
            ServerManager.consolePrint(AGENT, "CONSTRUCTION FAILED", ServerManager.RED);

        }

        return null;

    }

    // == INITIALIZOR METHODS ======================================

    private boolean initializedSerialPort() {

        try {

            serialPort = SerialPort.getCommPort(SERIAL_PORT_NAME);

            // Setup serial port (parameters: int baud rate, data size in bits, num stop
            // bits, parity bits).
            serialPort.setComPortParameters(9600, 8, 1, 0);
            serialPort.openPort();

        } 
        catch (Exception exception) {
            ServerManager.consolePrint(AGENT, "SERIAL PORT FAILED", ServerManager.RED);    
            // return false;
        }

        ServerManager.consolePrint(AGENT, "SERIAL PORT OK", ServerManager.GREEN);    
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

        } catch (Exception exception) {
            ServerManager.consolePrint(AGENT, "WEBCAM FAILED", ServerManager.RED);    
            return false;
        }

        // Return true to indicate that the initialization was successful.
        ServerManager.consolePrint(AGENT, "WEBCAM OK", ServerManager.GREEN);    
        return true;

    }

    public void performArduinoRotation() throws Exception {

        // Write the keyword into the the serial port.
        serialPort.getOutputStream().write(FEED_KEYWORD.getBytes());

        // Force the output to be written.
        serialPort.getOutputStream().flush();

    }

    public boolean initializeWebSocketClient() {
        try {

            // Recognize the server location (via URI) + Identify that this is the agent
            // (more efficient).
            String global = "ws://18.218.44.44:8090/agent";
            String local = "ws://10.0.0.198:8090/agent";
            URI serverUri = URI.create(local); // "ws://18.218.44.44:8090/agent"

            // Initialize the websocket client.
            webSocketClient = new WebSocketClient(serverUri) {

                // Method for what to do when initially when connected to server.

                @Override
                public void onOpen(ServerHandshake handshake) {
                    ServerManager.consolePrint(AGENT, "WEBSOCKET CONNECTION OK", ServerManager.GREEN);
                }

                // Method for what to do when recieve message.

                @Override
                public void onMessage(String message) {

                    // Check that the broadcast message was "FEED".

                    switch (message) {

                        case FEED_KEYWORD:
                            break;
                        case "ADD_WATCHER":
                            feederData.viewerCount++;
                            break;

                        case "REMOVE_WATCHER":
                            feederData.viewerCount--;
                            break;
                    }

                    if (message.equals(FEED_KEYWORD)) {

                        // Try to perform the arduino action.
                        try {
                            
                            
                            feederData.formattedLastTimeFed = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss (dd / MM / yyyy)"));
                            
                                    ServerManager.consolePrint(AGENT, "FEEDING COMPLETE @ " + feederData.formattedLastTimeFed, ServerManager.BLUE);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                }

                @Override
                public void onError(Exception ex) {
                }
            };

        }

        // If anything goes wrong, print issues with server connection.
        catch (Exception e) {
            ServerManager.consolePrint(AGENT, "FAILED TO CONNECT TO SERVER", ServerManager.RED);
            return false;
        }

        return true;

    }

    static class FeederData {

        String type = "FEEDER_DATA";
        int viewerCount;
        String encodedImage;
        String formattedLastTimeFed;

        public FeederData() {
            viewerCount = 0;
            encodedImage = "";
            formattedLastTimeFed = "HAS NOT BEEN FED YET";
        }

        public String toJsonString() {
            return ApplicationServer.gson.toJson(this);
        }

    }

}