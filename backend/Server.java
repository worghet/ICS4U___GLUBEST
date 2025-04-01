package backend;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import com.sun.net.httpserver.*;

public class Server {


    int serverPort;
    HttpServer httpServer;


    private static String SEND_WEBCAM_API = "/send-cam-data";


    private static String GET_WEBCAM_DATA_API = "/get-cam-data";

    public static void main(String[] args) {
        
        createServer();

    }

    public static void createServer() {


        int requestedServerPort = 8000;

          boolean portAvailible = false;

        // Essentially trying to run a server on requested port... seeing what happens.

        // (This is fancy "try-with-resources" syntax; auto closes objects when complete.)
        try (ServerSocket socket = new ServerSocket(requestedServerPort)) {

            // If there was no error binding this socket, then it is available.
            portAvailible = true;

        } catch (Exception e) {

            // Exception was thrown - likely because requested port was busy.
            System.out.println("port " + requestedServerPort + " is... OCCUPIED!");

        }

        // Continue with server creation.

        if (portAvailible) {

            // If the port is open, open the server on that port.            
            Server server = new Server(requestedServerPort);

        }



    }

    public Server(int requestedServerPort) {

        serverPort = requestedServerPort;

        try {

            // create server
            httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);

            
            
            
            
            // start server
            httpServer.start();
            System.out.println("server started at port: " + serverPort);
        }
        catch (Exception e) {
            System.out.println("Something went wrong..");
        }


    }


    private void initializeAPIs() {
        // static handler with different inputs? html, css, image, etc
        httpServer.createContext(GET_WEBCAM_DATA_API, new StaticFileHandler("webcam"));



    }

    // ============= HANDLERS ===========================================================

    static class StaticFileHandler implements HttpHandler {
    
        private String fileSort;
    
        public StaticFileHandler(String fileSort) {
            this.fileSort = fileSort;
        }
    
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {

            }
        }
    }
    
}
