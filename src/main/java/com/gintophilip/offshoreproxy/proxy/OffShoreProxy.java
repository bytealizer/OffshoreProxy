package com.gintophilip.offshoreproxy.proxy;

import com.gintophilip.offshoreproxy.httprequest.requestparser.HttpRequest;
import com.gintophilip.offshoreproxy.httprequest.executor.HttpRequestExecutor;
import com.gintophilip.offshoreproxy.httprequest.requestparser.HttpRequestParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class OffShoreProxy {


    private int proxyServerPort;

    public void listenAndServe(int proxyServerPort) {
        this.proxyServerPort = proxyServerPort;

        try (ServerSocket serverSocket = new ServerSocket(this.proxyServerPort)) {
            System.out.println("[off_shore_proxy] Waiting for connections on port " + proxyServerPort);

            while (true) {
                Socket clientSocket = serverSocket.accept(); // Wait for a client
                System.out.println("[off_shore_proxy] Received connection from client: " + clientSocket.getRemoteSocketAddress());
                new Thread(() -> {
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                        start(reader, writer, clientSocket);
                    } catch (IOException e) {
                        System.err.println("[off_shore_proxy] Error handling client: " + e.getMessage());
                    }
                }).start();
            }

        } catch (IOException e) {
            throw new RuntimeException("[off_shore_proxy] Server failed to start", e);
        }
    }

    private String readHttpRequest(BufferedReader reader) throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            System.out.println("[off_shore_proxy]reading request line");
            requestBuilder.append(line).append("\r\n");
        }
        if (line == null && requestBuilder.isEmpty()) {
            return null;
        }

        String request = requestBuilder.toString();
        System.out.println("[off_shore_proxy] Received from [ship_proxy]: " + request);
        return request;
    }
    private void start(BufferedReader reader, BufferedWriter writer,Socket shipProxySocket) {
        while (true) {
            try {
                String rawRequest = readHttpRequest(reader);
                if (rawRequest == null) {
                    System.out.println("[off_shore_proxy] Client closed the connection.");
                    break;
                }
                if (rawRequest.isEmpty()) {
                    continue;
                }else{
                }

                HttpRequest httpRequest = parseHttpRequest(rawRequest);
                if(httpRequest.method.toUpperCase().equals("CONNECT")){
                     new HttpRequestExecutor().executeHttpsRequest(httpRequest,shipProxySocket);
                }else {
                    new HttpRequestExecutor().executeRequest(httpRequest,shipProxySocket);
                }

            } catch (IOException | RuntimeException e) {
                System.err.println("[off_shore_proxy] Error: " + e.getMessage());
                break;
            }
        }
    }

    private HttpRequest parseHttpRequest(String rawRequest) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(rawRequest.getBytes(StandardCharsets.UTF_8));
        return HttpRequestParser.parse(inputStream);
    }
}

