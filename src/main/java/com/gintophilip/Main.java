package com.gintophilip;

import com.gintophilip.proxy.OffShoreProxy;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        OffShoreProxy offShoreProxy = new OffShoreProxy();
        offShoreProxy.listenAndServe(8081);
    }

}