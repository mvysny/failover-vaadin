package com.vaadin.failover.client;

import com.google.gwt.core.client.GWT;
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
        } else {
            // cancel the reconnection process if ongoing! It seems we are back online.
            getFailoverConnector().cancelReconnecting();
        }
    }

    @Override
    public void setText(String text) {
        if (!getFailoverConnector().isReconnectionOngoing()) {
            super.setText(text);
        } else {
            // the reconnection logic is running and the label is showing reconnection status.
            // suppress any attempts to overwrite the reconnection status but log them.
            GWT.log("FailoverReconnectDialog: Suppressed message: " + text);
        }
    }

    private void startReconnecting() {
        reconnect.setVisible(false);
        final FailoverReconnectConnector reconnectConnector = getFailoverConnector();
        if (!registeredAsListener) {
            reconnectConnector.statusListeners.add(new FailoverReconnectConnector.StatusListener() {
                @Override
                public void onStatus(String message) {
                    label.setText(message);
                }

                @Override
                public void onGaveUp() {
                    label.setText("Failed to reconnect, all servers appear to have crashed");
                    reconnect.setVisible(true);
                }
            });
            registeredAsListener = true;
        }
        reconnectConnector.startReconnecting();
    }

    private FailoverReconnectConnector getFailoverConnector() {
        for (ServerConnector connector : ac.getUIConnector().getChildren()) {
            if (connector instanceof FailoverReconnectConnector) {
                return ((FailoverReconnectConnector) connector);
            }
        }
        throw new IllegalStateException("The reconnect is not configured. Have you attached the FailoverReconnectExtension to your UI?");
    }
}
