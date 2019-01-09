package com.vaadin.failover;

import com.vaadin.ui.*;
import org.vaadin.addonhelpers.AbstractTest;

import java.util.Arrays;
import java.util.List;

/**
 * Add many of these with different configurations,
 * combine with different components, for regressions
 * and also make them dynamic if needed.
 */
public class BasicFailoverUsageUI extends AbstractTest {

    public BasicFailoverUsageUI() {
        setDescription("A basic failover to another server. Just kill the server and click the button.");
    }

    private final Label status = new Label();

    @Override
    public Component getTestComponent() {
        final List<String> urls = Arrays.asList("http://197.100.100.100", "http://197.100.100.101", "http://197.100.100.102", "https://vaadin.com");
        final FailoverReconnectExtension failoverExtension = FailoverReconnectExtension.addTo(UI.getCurrent());
        failoverExtension.setUrls(urls);
        failoverExtension.setStatusLabel(status);
        failoverExtension.setInfinite(false);
        failoverExtension.setRandomRobin(false);
        failoverExtension.setPingMillis(5000);
        // must exist on vaadin.com otherwise the failover will think vaadin.com is down. Read "The Image Ping" at https://github.com/mvysny/failover-vaadin
        // for more information
        failoverExtension.setPingImagePath("/images/hero-reindeer.svg");

        getReconnectDialogConfiguration().setDialogText("Can't connect to the server. The network may be down, or the server has crashed. Press the 'Try Spare Servers' button to try to connect to fallback server.");

        final VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Label("1. Kill the server and click the button: the browser should automatically redirect to " + urls + ". Note that we're using PING "));
        vl.addComponent(new Button("Click me"));
        vl.addComponent(new Label("OR 2. The button below will invoke the reconnect functionality directly, no need to kill the server."));
        vl.addComponent(status);
        vl.addComponent(new Button("Start FailOver", new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                failoverExtension.startFailOver();
            }
        }));
        vl.addComponent(new Button("Cancel FailOver", new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                failoverExtension.cancelFailOver();
            }
        }));
        return vl;
    }
}
