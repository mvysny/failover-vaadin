package com.vaadin.failover.uiserver;

import org.vaadin.addonhelpers.TServer;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class Server2 extends TServer {
    
    public static void main(String... args) throws Exception {
        Server.preparePingImage();
        new Server2().startServer(9992);
    }
}
