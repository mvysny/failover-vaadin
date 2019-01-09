package com.vaadin.failover.uiserver;

import org.vaadin.addonhelpers.TServer;

public class Server2 extends TServer {
    
    public static void main(String... args) throws Exception {
        new Server2().startServer(9992);
    }
}
