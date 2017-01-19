package com.vaadin.failover;

import com.vaadin.annotations.Push;
import com.vaadin.server.ExternalResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import org.vaadin.addonhelpers.AbstractTest;

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
        final String failoverUrl = "http://vaadin.com/company";
        setResource("failoverUrl", new ExternalResource(failoverUrl));
        getReconnectDialogConfiguration().setDialogText("Main server down, reconnecting to another node");
        final VerticalLayout vl = new VerticalLayout();
        vl.addComponent(new Label("Kill the server and click the button: the browser should automatically redirect to " + failoverUrl));
        vl.addComponent(new Button("Click me"));
        return vl;
    }
}
