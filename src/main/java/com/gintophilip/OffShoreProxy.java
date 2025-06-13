package com.gintophilip;

import com.gintophilip.httprequest.requestparser.HttpRequest;
import com.gintophilip.httprequest.executor.HttpRequestExecutor;
import com.gintophilip.httprequest.requestparser.HttpRequestParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class OffShoreProxy {


    private int proxyServerPort;

    public void listenAndServe(int proxyServerPort) {
        this.proxyServerPort = proxyServerPort;

        Socket clientSocket = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try (ServerSocket serverSocket = new ServerSocket(this.proxyServerPort)){
            System.out.println("[off_shore_proxy]waiting for connection from client");
            clientSocket = serverSocket.accept();
            System.out.println("[off_shore_proxy]received connection from client");
            writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        start(reader,writer);
    }

    private void start(BufferedReader reader, BufferedWriter writer) {
        while (true) {
            try {
                String rawRequest = readHttpRequest(reader);
                if (rawRequest.isEmpty()) {
                    continue;
                }else{
                }

                HttpRequest httpRequest = parseHttpRequest(rawRequest);
                String response = new HttpRequestExecutor().executeRequest(httpRequest);

                sendResponse(writer, response);
            } catch (IOException | RuntimeException e) {
                System.err.println("[off_shore_proxy] Error: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
    }

    private String readHttpRequest(BufferedReader reader) throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            System.out.println("[off_shore_proxy]reading request line");
            requestBuilder.append(line).append("\r\n");
        }
        String request = requestBuilder.toString();
        System.out.println("[off_shore_proxy] Received from [ship_proxy]: " + request);
        return request;
    }

    private HttpRequest parseHttpRequest(String rawRequest) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
        return HttpRequestParser.parse(inputStream);
    }

    private void sendResponse(BufferedWriter writer, String response) throws IOException {
        writer.write(response);
        writer.flush();
    }
}

