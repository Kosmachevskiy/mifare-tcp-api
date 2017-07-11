package app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Server implements Runnable {

    private ExecutorService service;
    private ServerSocket server;
    private int port;
    @Autowired
    private SmartCartService cartService;

    public Server(int port) {
        this.port = port;
    }

    @PostConstruct
    public void init() throws InterruptedException, IOException {
        System.out.println("TCP server startup.");
        server = new ServerSocket(port);
        service = Executors.newSingleThreadExecutor();
        service.submit(this);
    }

    @PreDestroy
    public void destroy() {
        if (service != null) {
            service.shutdownNow();
            System.out.println("TCP server shutting down.");
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket socket = server.accept();
                byte[] buffer = new byte["WRITE:123456789012345".length()];
                socket.getInputStream().read(buffer);
                String request = new String(buffer);
                String response;

                if (request.startsWith("READ;")) {
                    try {
                        response = "SUCCESS:" + cartService.read() + ";";
                    } catch (Exception e) {
                        response = "ERROR:" + e.getMessage() + ";";
                    }
                } else if (request.startsWith("ERASE;")) {
                    try {
                        cartService.erase();
                        response = "SUCCESS;";
                    } catch (Exception e) {
                        response = "ERROR:" + e.getMessage() + ";";
                    }
                } else if (request.startsWith("WRITE:")) {
                    String data = request.substring(request.indexOf(":") + 1, request.lastIndexOf(";"));
                    try {
                        cartService.write(data);
                        response = "SUCCESS;";
                    } catch (Exception e) {
                        response = "ERROR:" + e.getMessage() + ";";
                    }
                } else {
                    response = "UNKNOWN_REQUEST:" + request + ";";
                }
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeBytes(response);
                outputStream.flush();
                outputStream.close();
                socket.close();
            } catch (Exception e) {
                System.out.println("Server error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
