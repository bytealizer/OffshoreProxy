package com.gintophilip.offshoreproxy.httprequest.executor;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;

public class HttpRequestExecutor {

    public HttpRequestExecutor() {
    }

    public void executeRequest(com.gintophilip.offshoreproxy.httprequest.requestparser.HttpRequest requestWrapper, Socket clientSocket) {
        StringBuilder rawResponse = new StringBuilder();

        try {
            ProxyResponseHandler proxyHandler = new ProxyResponseHandler();
            proxyHandler.handleProxyResponse(clientSocket, requestWrapper);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            rawResponse.append("Error: ").append(e.getMessage());
        }

    }

    public void  executeHttpsRequest(com.gintophilip.offshoreproxy.httprequest.requestparser.HttpRequest httpRequest, Socket shipProxySocket) {
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
        try  {
            InputStream in = inSocket.getInputStream();
            OutputStream out = outSocket.getOutputStream();
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


 class ProxyResponseHandler {

    public void handleProxyResponse(Socket clientSocket, com.gintophilip.offshoreproxy.httprequest.requestparser.HttpRequest requestWrapper) throws IOException {
        try {
            Socket targetSocket = new Socket(requestWrapper.getHost(), 80);
            System.out.println("connected to target "+ targetSocket.getInetAddress().getHostName());
            InputStream targetIn = targetSocket.getInputStream();
            OutputStream targetOut = targetSocket.getOutputStream();
            OutputStream clientOut = clientSocket.getOutputStream();
            sendRequestToTarget(targetOut, requestWrapper);
            streamResponseDirectly(targetIn,clientOut,targetSocket,clientSocket);
            targetSocket.close();
        }catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();

        }
    }

    private void sendRequestToTarget(OutputStream targetOut, com.gintophilip.offshoreproxy.httprequest.requestparser.HttpRequest requestWrapper) throws IOException {
        System.out.println("sending request");
        byte[] requestBytes = requestWrapper.toHttpRequest().getBytes(StandardCharsets.UTF_8);
        targetOut.write(requestBytes);
        targetOut.flush();
        System.out.println("sending result completed");
    }
     private int indexOf(byte[] data, byte[] pattern) {
         outer:
         for (int i = 0; i <= data.length - pattern.length; i++) {
             for (int j = 0; j < pattern.length; j++) {
                 if (data[i + j] != pattern[j]) continue outer;
             }
             return i;
         }
         return -1;
     }

     private void streamResponseDirectly(InputStream targetIn, OutputStream clientOut, Socket targetSocket,Socket clientSocket) throws IOException {
         System.out.println("[off_shore_proxy]sending to client "+clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort());
         byte[] buffer = new byte[4096];
         int bytesRead=0;
         int totalBytes = 0;

         ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
         boolean headersComplete = false;
         byte[] delimiter = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);
         int headerEndIndex = -1;

         // Read until headers are complete
         while (!headersComplete && (bytesRead = targetIn.read(buffer)) != -1) {
             headerBuffer.write(buffer, 0, bytesRead);
             byte[] currentData = headerBuffer.toByteArray();
             headerEndIndex = indexOf(currentData, delimiter);
             if (headerEndIndex != -1) {
                 headersComplete = true;
                 break;
             }
         }

         if (!headersComplete) {
             System.err.println("[off_shore_proxy] Failed to read full headers");
             return;
         }

         // Extract and send headers
         byte[] headerBytes = headerBuffer.toByteArray();
         clientOut.write(headerBytes, 0, headerEndIndex + 4);

         String headerStr = new String(headerBytes, 0, headerEndIndex + 4, StandardCharsets.ISO_8859_1);
         System.out.println("[off_shore_proxy] Response headers:\n" + headerStr);

         boolean isChunked = headerStr.toLowerCase().contains("transfer-encoding: chunked");
         boolean isGzip = headerStr.toLowerCase().contains("content-encoding: gzip");

         // Prepare InputStream for body
         int bodyStart = headerEndIndex + 4;
         int remainingLength = headerBytes.length - bodyStart;
         InputStream bodyStream;

         if (remainingLength > 0) {
             InputStream remainingBody = new ByteArrayInputStream(headerBytes, bodyStart, remainingLength);
             bodyStream = new SequenceInputStream(remainingBody, targetIn);
         } else {
             bodyStream = targetIn;
         }

         if (isChunked) {
             targetSocket.setSoTimeout(10000); // 10 sec timeout for chunked encoding
             try {
                 System.out.println("Chunked encoding detected, streaming with timeout...");
                 while ((bytesRead = bodyStream.read(buffer)) != -1) {
                     clientOut.write(buffer, 0, bytesRead);
                     clientOut.flush();
                     totalBytes += bytesRead;
                 }
             } catch (SocketTimeoutException e) {
                 System.out.println("Read timed out, no data received for 10 seconds. Closing socket.");
                 try {
                     String END_MARKER = "\r\nEND_OF_RESPONSE_FROM_OFF_SHORE_PROXY\r\n";
                     clientOut.write(END_MARKER.getBytes(StandardCharsets.UTF_8));
                     clientOut.flush();
                     targetSocket.close();
                 } catch (IOException ex) {
                     System.err.println("Error closing socket after timeout: " + ex.getMessage());
                 }
             }
         } else {
             while ((bytesRead = bodyStream.read(buffer)) != -1) {
                 clientOut.write(buffer, 0, bytesRead);
                 clientOut.flush(); // <-- flush here too
                 totalBytes += bytesRead;
             }
         }
         System.out.println("[off_shore_proxy]sending to client "+clientSocket.getInetAddress().getHostAddress()+":"+clientSocket.getPort());
         System.out.println("[proxy] Finished streaming response. Total body bytes: " + totalBytes);

     }

     private byte[] readFullResponse(InputStream targetIn) throws IOException {
        System.out.println("[proxy] Start reading response from target server...");

        ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        int totalBytesRead = 0;
        // Step 1: Read until we get full headers (ending in \r\n\r\n)
        StringBuilder headerBuilder = new StringBuilder();
        boolean headersComplete = false;

        while (!headersComplete && (bytesRead = targetIn.read(buffer)) != -1) {
            responseBuffer.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;

            headerBuilder.append(new String(buffer, 0, bytesRead));
            if (headerBuilder.indexOf("\r\n\r\n") != -1) {
                headersComplete = true;
                break;
            }
        }

        if (!headersComplete) {
            System.err.println("[proxy] Failed to read full headers");
            return responseBuffer.toByteArray(); // return what we got so far
        }

        String headersPart = headerBuilder.substring(0, headerBuilder.indexOf("\r\n\r\n"));
        System.out.println("[proxy] Response headers:\n" + headersPart);

        boolean isChunked = headersPart.toLowerCase().contains("transfer-encoding: chunked");
        boolean isGzip = headersPart.toLowerCase().contains("content-encoding: gzip");

        if (isChunked) {
            System.out.println("[proxy] Detected chunked transfer encoding.");
            // You may choose to implement actual chunk decoding here

        }
        while ((bytesRead = targetIn.read(buffer)) != -1) {
            responseBuffer.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
        }

        System.out.println("[proxy] Finished reading response. Total bytes read: " + totalBytesRead);
        return responseBuffer.toByteArray();
    }

    private byte[] modifyResponseContentLength(byte[] fullResponse) {
        String responseString = new String(fullResponse, StandardCharsets.UTF_8);
        int headerEndIndex = responseString.indexOf("\r\n\r\n");

        if (headerEndIndex == -1) {
            throw new IllegalArgumentException("Invalid HTTP response (no header/body separation)");
        }

        String headersPart = responseString.substring(0, headerEndIndex);
        String bodyPart = responseString.substring(headerEndIndex + 4); // skip \r\n\r\n
        int bodyLength = bodyPart.getBytes(StandardCharsets.UTF_8).length;

        String[] headerLines = headersPart.split("\r\n");
        StringBuilder modifiedHeaders = new StringBuilder();

        boolean hasContentLength = false;
        for (String line : headerLines) {
            if (line.toLowerCase().startsWith("content-length:")) {
                hasContentLength = true;
                modifiedHeaders.append("Content-Length: ").append(bodyLength).append("\r\n");
            } else {
                modifiedHeaders.append(line).append("\r\n");
            }
        }

        if (!hasContentLength) {
            modifiedHeaders.append("Content-Length: ").append(bodyLength).append("\r\n");
        }

        // Combine headers and body
        return (modifiedHeaders + "\r\n" + bodyPart).getBytes(StandardCharsets.UTF_8);
    }
}

