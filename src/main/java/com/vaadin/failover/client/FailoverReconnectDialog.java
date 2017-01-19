package com.vaadin.failover.client;

import com.google.gwt.user.client.Window;
import com.vaadin.client.communication.DefaultReconnectDialog;

/**
 * @author mavi
 */
public class FailoverReconnectDialog extends DefaultReconnectDialog {
    @Override
    public void setReconnecting(boolean reconnecting) {
        super.setReconnecting(reconnecting);
        if (reconnecting) {
            final String url = ac.getUIConnector().getResourceUrl("failoverUrl");
            // We do not want the user to be able to navigate back - if the server would come up alive and the user back-navigated to it,
            // the session in the new server would not be transferred back and thus is perceived as lost.
            // Thus, Use GWT replace instead of assign - replace modifies the history and thus the user is not able to navigate back to the old server.
            Window.Location.replace(url);
        }
    }
}
