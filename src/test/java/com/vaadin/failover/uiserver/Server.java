package com.vaadin.failover.uiserver;

import org.vaadin.addonhelpers.TServer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * The main method in this helper class opens a web server to 
 * http://localhost:9991/ to serve all your test UIs for development and
 * integration testing.
 */
public class Server extends TServer {
    
    public static void main(String... args) throws Exception {
        preparePingImage();
        new Server().startServer(9991);
    }

    /**
     * This will make sure that there is an image located at <a href="http://localhost:9998/VAADIN/themes/valo/app-icon.png">http://localhost:9998/VAADIN/themes/valo/app-icon.png</a>
     * when the server starts. This is vital for image-based pinging. See https://github.com/mvysny/failover-vaadin "The Image Ping" for more details.
     */
    public static void preparePingImage() throws IOException {
        new File("target/testwebapp/VAADIN/themes/valo").getAbsoluteFile().mkdirs();
        try(InputStream in = Server.class.getResourceAsStream("/vaadinlogo.png")) {
            Files.copy(in, new File("target/testwebapp/VAADIN/themes/valo/app-icon.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
