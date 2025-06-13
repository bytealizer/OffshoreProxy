package com.gintophilip.httprequest.executor;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

public class HttpRequestExecutor {

    private final HttpClient client;

    public HttpRequestExecutor() {
        this.client = HttpClient.newHttpClient();
    }

    public String executeRequest(com.gintophilip.httprequest.requestparser.HttpRequest requestWrapper) {
        StringBuilder rawResponse = new StringBuilder();

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(requestWrapper.path));

            switch (requestWrapper.method.toUpperCase()) {
                case "GET":
                    builder.GET();
                    break;
                case "POST":
                    builder.POST(HttpRequest.BodyPublishers.ofString(requestWrapper.body));
                    break;
                case "PUT":
                    builder.PUT(HttpRequest.BodyPublishers.ofString(requestWrapper.body));
                    break;
                case "DELETE":
                    builder.DELETE();
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported method: " + requestWrapper.method);
            }

            HttpRequest request = builder.build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String protocol = "HTTP/1.1"; // Java HttpClient doesn’t expose version easily
            String statusMessage = ""; // Not available in Java HttpClient directly

            rawResponse.append(protocol)
                    .append(" ")
                    .append(statusCode)
                    .append(" ")
                    .append(statusMessage)
                    .append("\r\n");
            HttpHeaders headers = response.headers();
            for (Map.Entry<String, List<String>> entry : headers.map().entrySet()) {
                for (String value : entry.getValue()) {
                    rawResponse.append(entry.getKey()).append(": ").append(value).append("\r\n");
                }
            }
            rawResponse.append("\r\n");
            rawResponse.append(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            rawResponse.append("Error: ").append(e.getMessage());
        }

        return rawResponse.toString();
    }

    public void  executeHttpsRequest(com.gintophilip.httprequest.requestparser.HttpRequest httpRequest,Socket shipProxySocket) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("https://"+httpRequest.path));

        String[] hostParts = httpRequest.path.split(":");
        String host = hostParts[0];
        int port = Integer.parseInt(hostParts[1]);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(shipProxySocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(shipProxySocket.getOutputStream()));
            Socket targetSocket = new Socket(host, port);
            writer.write("HTTP/1.1 200 Connection Established\r\n\r\n");
            writer.flush();

            new Thread(() -> forwardData(shipProxySocket, targetSocket)).start();
            forwardData(targetSocket, shipProxySocket);

        }catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private void forwardData(Socket inSocket, Socket outSocket) {
        try (InputStream in = inSocket.getInputStream();
             OutputStream out = outSocket.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            // socket closed
        }
    }
}
