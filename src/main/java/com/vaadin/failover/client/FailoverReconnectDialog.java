package com.vaadin.failover.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.communication.DefaultReconnectDialog;

/**
 * Replaces the standard reconnect dialog with a new one. The new one will, in addition to the reconnection itself,
 * ping given spare/fail-over URLs. If a live fail-over URL is find, the browser is redirected there.
 * @author mavi
 */
public class FailoverReconnectDialog extends DefaultReconnectDialog {
    /**
     * The reconnect button. When pushed, the failover process will start.
     */
    private Button reconnect;
    /**
     * Stops the failover process and returns to the original mode of wait-until-this-server-becomes-up.
     */
    private Button cancelReconnect;
    /**
     * During the failover process the status message from {@link com.vaadin.failover.client.FailoverReconnectConnector.StatusListener#onStatus(String)}
     * is displayed instead of the original dialog text.
     * When the failover is canceled, I need to restore the original dialog text immediately. In order to do that, I'll remember the text here.
     */
    private String dialogText = null;

    private FlowPanel getRoot() {
        return (FlowPanel) getWidget();
    }

    @Override
    public void setReconnecting(boolean reconnecting) {
        super.setReconnecting(reconnecting);
        if (reconnecting) {
            if (reconnect == null) {
                // create the UI buttons
                reconnect = new Button(getFailoverConnector().getState().trySpareServersButtonCaption, new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        startReconnecting();
                    }
                });
                reconnect.getElement().setAttribute("style", "margin-left: 10px");
                getRoot().add(reconnect);
                cancelReconnect = new Button("Cancel", new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        getFailoverConnector().cancelFailOver();
                        setReconnectButtonVisible(true);
                    }
                });
                cancelReconnect.getElement().setAttribute("style", "margin-left: 10px");
                getRoot().add(cancelReconnect);
                setReconnectButtonVisible(true);

                // register itself as a listener for FailoverReconnectConnector status
                getFailoverConnector().statusListeners.add(new FailoverReconnectConnector.StatusListener() {
                    @Override
                    public void onStatus(String message) {
                        label.setText(message);
                    }

                    @Override
                    public void onGaveUp() {
                        label.setText("Failed to reconnect, all servers appear to have crashed");
                        setReconnectButtonVisible(true);
                    }
                });
            }
        } else {
            // cancel the reconnection process if ongoing! It seems we are back online.
            getFailoverConnector().cancelFailOver();
        }
    }

    private void setReconnectButtonVisible(boolean reconnectVisible) {
        reconnect.setVisible(reconnectVisible);
        cancelReconnect.setVisible(!reconnectVisible && getFailoverConnector().getState().allowCancel);
        if (reconnectVisible && dialogText != null) {
            super.setText(dialogText);
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
            dialogText = text;
        }
    }

    private void startReconnecting() {
        setReconnectButtonVisible(false);
        getFailoverConnector().startFailOver();
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
