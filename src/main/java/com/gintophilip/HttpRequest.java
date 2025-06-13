package com.gintophilip;

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
}
