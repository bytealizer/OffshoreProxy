package com.gintophilip;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class OffShoreProxy {
    private int port = 8081;

    public OffShoreProxy(int port) {
        this.port = port;
    }
    public void start() throws IOException {

        try (ServerSocket offShoreSocket = new ServerSocket(port)) {
            System.out.println("[off_shore_proxy] started...");
            while (true) {
                Socket clientSocket = offShoreSocket.accept();
                BufferedReader clientInputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter clientOutPutWriter = new PrintWriter(clientSocket.getOutputStream());
                String line;
                while ((line = clientInputReader.readLine()) != null) {
                    System.out.println("[off_shore_proxy]Received: " + line);
                    clientOutPutWriter.println("[off_shore_proxy]Echo: " + line);
                    clientOutPutWriter.flush();
                }

            }
        }


    }
}
