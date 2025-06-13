package com.gintophilip.offshoreproxy.httprequest.requestparser;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class HttpRequest {
    public String method;
    public String path;
    public String version;
    public Map<String, String> headers = new LinkedHashMap<>();
    public Map<String, String> queryParams = new LinkedHashMap<>();
    public String body = "";

    @Override
    public String toString() {
        return "Method: " + method + "\n" +
                "Path: " + path + "\n" +
                "Version: " + version + "\n" +
                "Headers: " + headers + "\n" +
                "QueryParams: " + queryParams + "\n" +
                "Body: " + body;
    }
    public String toHttpRequest() {
        StringBuilder request = new StringBuilder();

        // Build query string
        StringBuilder queryString = new StringBuilder();
        if (!queryParams.isEmpty()) {
            queryString.append("?");
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (queryString.length() > 1) queryString.append("&");
                queryString.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());
            }
        }

        // Request line
        request.append(method).append(" ")
                .append(path).append(queryString)
                .append(" ")
                .append(version).append("\r\n");

        // Headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append("\r\n");
        }

        // Separate headers from body
        request.append("\r\n");

        // Body
        if (body != null && !body.isEmpty()) {
            request.append(body);
        }

        return request.toString();
    }

    public String getHost() {
        try {
            URI uri = URI.create(path);
            URL url = uri.toURL();
            return url.getHost();
        } catch (MalformedURLException e) {
            System.err.println("Invalid URL: " + path);
            return null;
        }
    }
}