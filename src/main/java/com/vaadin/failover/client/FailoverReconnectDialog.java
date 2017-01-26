package com.vaadin.failover.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.communication.DefaultReconnectDialog;

/**
 * @author mavi
 */
public class FailoverReconnectDialog extends DefaultReconnectDialog {
    private Button reconnect;
    private boolean registeredAsListener = false;

    private FlowPanel getRoot() {
        return (FlowPanel) getWidget();
    }

    @Override
    public void setReconnecting(boolean reconnecting) {
        super.setReconnecting(reconnecting);
        if (reconnecting) {
            if (reconnect == null) {
                reconnect = new Button("Reconnect", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        startReconnecting();
                    }
                });
                reconnect.getElement().setAttribute("style", "margin-left: 10px");
                getRoot().add(reconnect);
            }
        }
    }

    private void startReconnecting() {
        final FailoverReconnectConnector reconnectConnector = getFailoverConnector();
        if (reconnectConnector == null) {
            throw new IllegalStateException("The reconnect is not configured. Have you attached the FailoverReconnectExtension to your UI?");
        }
        if (!registeredAsListener) {
            reconnectConnector.statusListeners.add(new FailoverReconnectConnector.StatusListener() {
                @Override
                public void onStatus(String message) {
                    label.setText(message);
                }
            });
        }
        reconnectConnector.startReconnecting();
    }

    private FailoverReconnectConnector getFailoverConnector() {
        for (ServerConnector connector : ac.getUIConnector().getChildren()) {
            if (connector instanceof FailoverReconnectConnector) {
                return ((FailoverReconnectConnector) connector);
            }
        }
        return null;
    }
}
