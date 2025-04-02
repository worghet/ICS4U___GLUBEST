package backend;

import com.fazecast.jSerialComm.SerialPort;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import com.github.sarxos.webcam.*;

import java.net.URI;
import java.util.Base64;
import java.util.Scanner;
import javax.imageio.ImageIO;


import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class Agent {

    private static final String FEED_KEYWORD = "FEED";
    private static final String SERIAL_PORT_NAME = "/dev/ttyACM0"; // linux-specific
    private static final int WEBCAM_SENDING_FPS = 30;

    final static Scanner input = new Scanner(System.in);

    SerialPort serialPort; // indicate com port
    Webcam webcam;

    WebSocketClient webSocketClient;
    Thread webcamSendingThread;

    // This main will be run on the agent computer
    public static void main(String[] args) {

        Agent agent = new Agent();
        if (agent.initializedSerialPort() && agent.initializedWebcam() && agent.initializeWebSocketClient()) {
            System.out.println("setup all good!");

            agent.webcamSendingThread = new Thread() {
                public void run() {
                    try {
                        while (true) {
                            
                            System.out.println("getting image..");
                            BufferedImage image = agent.webcam.getImage();
                            System.out.println("got image");

                            // Convert the image to Base64
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            ImageIO.write(image, "PNG", byteArrayOutputStream);
                            byte[] imageBytes = byteArrayOutputStream.toByteArray();
                            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                            System.out.println("encoded!");

                            // Send the Base64 string over WebSocket
                            agent.webSocketClient.send(base64Image);
                            System.out.println("Image sent to server in Base64 format");
                            
                            System.out.println("sent webcam data!");
                            Thread.sleep(1000 / WEBCAM_SENDING_FPS);
                        }
                    } catch (Exception exception) {
                        System.out.println("SOMETHING IN WRONG WHEN SENDING WEBCAM!!!" + exception.getMessage());
                    }
                }
            };

            agent.webSocketClient.connect(); // connect to server
            agent.webcamSendingThread.start();
        } else {
            System.err.println("Something went wrong within the initialization process.");
        }
    }

    private boolean initializedSerialPort() {

        // parameters: int baud rate, data size in bits, num stop bits, parity bits
        // serialPort.setComPortParameters(9600, 8, 1, 0);

        // // check port availibility
        // if (!serialPort.openPort()) {
        // System.out.println("Oops! Port not availible..");
        // return false;
        // }

        // System.out.println("port is open!!");
        return true;
    }

    private boolean initializedWebcam() {

        try {
            webcam = Webcam.getDefault();
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcam.open();
        } catch (Exception exception) {
            System.out.println("Something went wrong while initializing camera..");
            return false;
        }

        return true;

    }

    // Webcam + Arduino handling here
    public void performArduinoRotation() throws Exception {

        serialPort.getOutputStream().write(FEED_KEYWORD.getBytes());
        serialPort.getOutputStream().flush();

    }

    public boolean initializeWebSocketClient() {
        try {

            URI serverUri = URI.create("ws://127.0.0.1:8000");
            webSocketClient = new WebSocketClient(serverUri) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("CONNECTION SUCCESSFUL");
                    this.send("I am the agent.. super sneaky!");

                }

                @Override
                public void onMessage(String message) {
                    // System.out.println("Received from server: " + message);
                    // You can handle server messages here (e.g., trigger feed rotation)
                    if (message.equals(FEED_KEYWORD)) {
                        try {
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

        } catch (Exception e) {
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