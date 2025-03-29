package backend;

import com.github.sarxos.webcam.*;
import com.fazecast.jSerialComm.SerialPort;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

public class Agent {    

    private static final String FEED_KEYWORD = "FEED";
    private static final String PORT_NAME = "/dev/ttyACM0"; // linux-specific
    final static Scanner input = new Scanner(System.in);
    
    SerialPort serialPort; // indicate com port
    Webcam webcam;

    public static void main(String[] args) {


        // Webcam webcam = Webcam.getDefault();
		// if (webcam != null) {
		// 	System.out.println("Webcam: " + webcam.getName());
		// } else {
		// 	System.out.println("No webcam detected");
		// }

        // start agent
        System.out.println("creating agent..");
        Agent agent = new Agent();

        try {
            System.out.println("setting up cam");
            agent.webcam = Webcam.getDefault();
            System.out.println("opened cam");
            agent.webcam.open();
            
            ImageIO.write(agent.webcam.getImage(), "PNG", new File("hello-world.png"));
            System.out.println("did it!");
        }
        catch (Exception e) {
            System.out.println("cam went wrong");
        }


        // agent.serialPort = SerialPort.getCommPort(PORT_NAME);
        // if (agent.initializedSerialPort()) {

        //     String requestArduino;
        //     while (true) {
        //         requestArduino = input.nextLine();
        //         if (requestArduino.equals(FEED_KEYWORD)) {
        //             try {
        //                 agent.performArduinoRotation();
        //             }
        //             catch (Exception e) {
        //                 System.out.println("sum went wrong");
        //             }
        //         }
    
        //     }
        // }

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

    // Webcam + Arduino handling here
    public void performArduinoRotation() throws Exception {
  
        serialPort.getOutputStream().write(FEED_KEYWORD.getBytes());
        serialPort.getOutputStream().flush();

    }

}