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

    @Override
    public Component getTestComponent() {
        final List<String> urls = Arrays.asList("http://197.100.100.100/company", "http://197.100.100.101/company", "http://197.100.100.102/company", "https://vaadin.com/company");
        final FailoverReconnectExtension reconnectExtension = FailoverReconnectExtension.addTo(UI.getCurrent());
        reconnectExtension.setUrls(urls);
        getReconnectDialogConfiguration().setDialogText("Can't connect to the server. The network may be down, or the server has crashed. Press the 'Reconnect' button to try to connect to fallback server.");
        final VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Label("Kill the server and click the button: the browser should automatically redirect to " + urls));
        vl.addComponent(new Button("Click me"));
        vl.addComponent(new Label("The button below will invoke the reconnect functionality directly, no need to kill the server."));
        vl.addComponent(new Button("Click me", new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                reconnectExtension.startReconnecting();
            }
        }));
        return vl;
    }
}
