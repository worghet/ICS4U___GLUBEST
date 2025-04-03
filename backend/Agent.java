package backend;

// == IMPORTS ======================================
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.client.WebSocketClient;
import com.fazecast.jSerialComm.SerialPort;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import com.github.sarxos.webcam.*;
import javax.imageio.ImageIO;
import java.util.Scanner;
import java.util.Base64;
import java.net.URI;

// == AGENT ========
public class Agent {

    // == CLASS CONSTANTS ========================================

    private static final String SERIAL_PORT_NAME = "/dev/ttyACM0"; // linux-specific.
    private static final String FEED_KEYWORD = "FEED";
    private static final int WEBCAM_SENDING_FPS = 30;

    // == FUNCTIONAL CONSTANTS ===================================

    final static Scanner input = new Scanner(System.in);

    // == INSTANCE VARIABLES =====================================

    WebSocketClient webSocketClient; // Websocket client itself.
    Thread webcamSendingThread; // Separate thread meant to send webcam data.
    SerialPort serialPort; // Port access to write to Arduino.
    Webcam webcam; // Webcam object.

    // == MAIN ====================================================

    // This main will be run on the agent computer.
    public static void main(String[] args) {

        // Setup agent object
        Agent agent = createAgent();

        // Start processes.
        if (agent != null) {
            agent.startOperating();
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

            System.out.println("SETUP | OK");

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

                            // Send the encoded image to server.
                            agent.webSocketClient.send(base64Image);

                            // Pause sending (to prevent overflow) momentarily.
                            Thread.sleep(1000 / WEBCAM_SENDING_FPS);
                        }
                    } catch (Exception exception) {
                        System.out.println("SOMETHING IN WRONG WHEN SENDING WEBCAM!!!" + exception.getMessage());
                    }
                }
            };

            // If all initiallization was good, return the agent as a new object.
            return agent;

        } else {

            // Otherwise, print error message.
            System.err.println("Something went wrong within the initialization process.");

        }

        return null;

    }

    // == INITIALIZOR METHODS ======================================

    private boolean initializedSerialPort() {


        serialPort = SerialPort.getCommPort(SERIAL_PORT_NAME);

        // Setup serial port (parameters: int baud rate, data size in bits, num stop
        // bits, parity bits).
        serialPort.setComPortParameters(9600, 8, 1, 0);

        // Check port availibility.
        if (!serialPort.openPort()) {
            System.out.println("Oops! Port not availible..");
            return false;
        }

        System.out.println("port is open!!");
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

            // Print a message, and return false to prevent condition in factory from being
            // true.
            System.out.println("Something went wrong while initializing camera..");
            return false;
        }

        // Return true to indicate that the initialization was successful.
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

            // Recognize the server location (via URI).
            URI serverUri = URI.create("ws://127.0.0.1:8000");

            // Initialize the websocket client.
            webSocketClient = new WebSocketClient(serverUri) {

                // Method for what to do when initially when connected to server.

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("CONNECTION SUCCESSFUL");
                    this.send("I am the agent.. super sneaky!");

                }

                // Method for what to do when recieve message.

                @Override
                public void onMessage(String message) {

                    // Check that the broadcast message was "FEED".

                    if (message.equals(FEED_KEYWORD)) {

                        // Try to perform the arduino action.
                        try {
                            System.out.println("CAT IS BEING FED NOW..");
                            performArduinoRotation();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

        }

        // If anything goes wrong, print issues with server connection.
        catch (Exception e) {
            System.out.println("Had issues connecting to server..");
            return false;
        }

        return true;

    }

    static class FeederData {

        int peopleViewing;
        String formattedLastTimeFed;

    }

}