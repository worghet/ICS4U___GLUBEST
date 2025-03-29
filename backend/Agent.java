package backend;


import com.fazecast.jSerialComm.SerialPort;
import java.util.Scanner;

public class Agent {    

    private static final String FEED_KEYWORD = "FEED";
    private static final String PORT_NAME = "/dev/ttyACM0"; // linux-specific
    final static Scanner input = new Scanner(System.in);
    
    final SerialPort serialPort = SerialPort.getCommPort(PORT_NAME); // indicate com port

    public static void main(String[] args) {
        // start agent
        System.out.println("creating agent..");
        Agent agent = new Agent();
        if (agent.initializedSerialPort()) {

            String requestArduino;
            while (true) {
                requestArduino = input.nextLine();
                if (requestArduino.equals(FEED_KEYWORD)) {
                    try {
                        agent.performArduinoRotation();
                    }
                    catch (Exception e) {
                        System.out.println("sum went wrong");
                    }
                }
    
            }
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

    // Webcam + Arduino handling here
    public void performArduinoRotation() throws Exception {
  
        serialPort.getOutputStream().write(FEED_KEYWORD.getBytes());
        serialPort.getOutputStream().flush();

    }

}