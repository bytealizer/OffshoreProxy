package com.gintophilip.httprequest.requestparser;

import com.gintophilip.HttpRequest;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * This class will parse the raw http requests which are received as String
 */
public class HttpRequestParser {

    /**
     *
     * @param inputStream the raw http request
     * @return a custom representation of http request
     * @throws IOException
     */
    public static HttpRequest parse(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        HttpRequest request = new HttpRequest();

        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Invalid HTTP request line");
        }

        // parse the request line
        String[] requestLineParts = requestLine.split(" ");
        request.method = requestLineParts[0];
        String fullPath = requestLineParts[1];
        request.version = requestLineParts[2];

        // Split path and query parameters
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex != -1) {
            request.path = fullPath.substring(0, queryIndex);
            parseQueryParams(fullPath.substring(queryIndex + 1), request.queryParams);
        } else {
            request.path = fullPath;
        }

        // Parse Headers and stores in a map
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(':');
            if (colonIndex != -1) {
                String name = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                request.headers.put(name, value);
            }
        }

        // Parse Body if the method expects a body
        if (request.method.equalsIgnoreCase("POST") ||
                request.method.equalsIgnoreCase("PUT") ||
                request.method.equalsIgnoreCase("PATCH") ||
                request.method.equalsIgnoreCase("DELETE")) {

            int contentLength = 0;
            //extract content length header
            if (request.headers.containsKey("Content-Length")) {
                contentLength = Integer.parseInt(request.headers.get("Content-Length"));
            }

            char[] bodyChars = new char[contentLength];
            reader.read(bodyChars);
            //extract request body
            request.body = new String(bodyChars);
        }

        return request;
    }


    private static void parseQueryParams(String query, Map<String, String> queryParams) {
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = decodeURL(keyValue[0]);
            String value = keyValue.length > 1 ? decodeURL(keyValue[1]) : "";
            queryParams.put(key, value);
        }
    }

    private static String decodeURL(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
