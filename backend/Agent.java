package backend;

import com.fazecast.jSerialComm.SerialPort;
import com.github.sarxos.webcam.*;
import java.util.Scanner;

public class Agent {    

    private static final String FEED_KEYWORD = "FEED";
    private static final String PORT_NAME = "/dev/ttyACM0"; // linux-specific
    final static Scanner input = new Scanner(System.in);
    final static int WEBCAM_POSTING_FPS = 20;

    SerialPort serialPort; // indicate com port
    Webcam webcam;


    // This main will be run on the agent computer
    public static void main(String[] args) {

        Agent agent = new Agent();
        if (agent.initializedSerialPort() && agent.initializedWebcam()) {
            System.out.println("all good!");
        }
        else {
            System.err.println("Something went wrong within the initialization process.");
        }
    }

    private boolean initializedSerialPort() {


        // parameters: int baud rate, data size in bits, num stop bits, parity bits
        serialPort.setComPortParameters(9600, 8, 1, 0);

        // check port availibility
        if (!serialPort.openPort()) {
            System.out.println("Oops! Port not availible..");
            return false;
        }

        System.out.println("port is open!!");
        return true;
    }

    private boolean initializedWebcam() {
        return true;
    }

    // Webcam + Arduino handling here
    public void performArduinoRotation() throws Exception {
  
        serialPort.getOutputStream().write(FEED_KEYWORD.getBytes());
        serialPort.getOutputStream().flush();

    }

}