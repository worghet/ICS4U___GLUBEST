package backend;


import com.fazecast.jSerialComm.SerialPort;
import java.util.Scanner;

public class Agent {

    private static final String PORT_NAME = "/dev/ttyACM0"; // linux-specific
    private static final Scanner input = new Scanner(System.in);
    private static final SerialPort serialPort = SerialPort.getCommPort(PORT_NAME); // indicate com port


    public static void main(String[] args) {
        // start agent
        System.out.println("creating agent..");
        Agent agent = new Agent();
        agent.initializeSerialPort();
    }

    private void initializeSerialPort() {


        // parameters: int baud rate, data size in bits, num stop bits, parity bits
        serialPort.setComPortParameters(9600, 8, 1, 0);

        // check port availibility
        if (!serialPort.openPort()) {
            System.out.println("Oops! Port not availible..");
            return;
        }

        System.out.println("port is open!!");
      
        try {
            performProcess();
        }
        catch (Exception e) {
            System.out.println("couldnt perform process");
        }

    }

    private void performProcess() throws Exception {

        while (true) {
            System.out.print("how many times to blink? ");
            Integer numBlinks = input.nextInt();
            if (numBlinks == 0) {
                return;
            }

            Thread.sleep(1500);

            serialPort.getOutputStream().write(numBlinks.byteValue());
        }
    }


    // Webcam + Arduino handling here
    public void pingArduino() {
  


    }

}