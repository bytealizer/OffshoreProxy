package com.gintophilip.httprequest.executor;

import com.gintophilip.HttpRequest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class HttpRequestExecutor {

    public String executeRequest(HttpRequest request) {
        StringBuilder rawResponse = new StringBuilder();

        try {
            URL url = new URL(request.path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(request.method.toUpperCase());

            // ---- Status Line ----
            int statusCode = connection.getResponseCode();
            String statusMessage = connection.getResponseMessage();
            String protocol = "HTTP/1.1";

            rawResponse.append(protocol)
                    .append(" ")
                    .append(statusCode)
                    .append(" ")
                    .append(statusMessage)
                    .append("\r\n");

            Map<String, List<String>> headers = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue()) {
                    if (key != null) {
                        rawResponse.append(key).append(": ").append(value).append("\r\n");
                    } else {

                    }
                }
            }

            rawResponse.append("\r\n");
            InputStream inputStream;
            if (statusCode >= 400) {
                inputStream = connection.getErrorStream();
            } else {
                inputStream = connection.getInputStream();
            }

            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    rawResponse.append(line).append("\n");
                }
                reader.close();
            }
            connection.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
            rawResponse.append("Error: ").append(e.getMessage());
        }
        return rawResponse.toString();

    }
}
