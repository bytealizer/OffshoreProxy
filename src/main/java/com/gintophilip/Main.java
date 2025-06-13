package com.gintophilip;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        OffShoreProxy offShoreProxy = new OffShoreProxy(8081);

        try {
            offShoreProxy.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}