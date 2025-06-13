package com.gintophilip.offshoreproxy;

import com.gintophilip.offshoreproxy.proxy.OffShoreProxy;


public class Main {
    public static void main(String[] args) {
        Integer listenPort = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    if (i + 1 < args.length) {
                        listenPort = Integer.parseInt(args[++i]);
                    } else {
                        System.err.println("Missing value for --port");
                        System.exit(1);
                    }
                    break;

                default:
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
            }
        }

        // Validate required argument
        if (listenPort == null) {
            System.err.println("Missing required argument: --port <port>");
            System.exit(1);
        }

        OffShoreProxy offShoreProxy = new OffShoreProxy();
        offShoreProxy.listenAndServe(listenPort);
    }

}