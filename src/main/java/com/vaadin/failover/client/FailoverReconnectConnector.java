package com.vaadin.failover.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.label.LabelConnector;
import com.vaadin.failover.FailoverReconnectExtension;
import com.vaadin.shared.ui.Connect;

import java.util.*;

/**
 * Implements the reconnection logic. {@link FailoverReconnectDialog} expects this extension to be attached to the UI class.
 * @author mavi
 */
@Connect(FailoverReconnectExtension.class)
public class FailoverReconnectConnector extends AbstractExtensionConnector implements FailoverReconnectClientRpc {

    public interface StatusListener {
        /**
         * Called from {@link FailoverReconnectConnector#startReconnecting()} to update the reconnection state.
         * @param message the reconnection state message, such as "Reconnecting to %0".
         */
        void onStatus(String message);

        /**
         * The reconnection process iterated all URLs and gave up since {@link FailoverReconnectState#infinite} is false.
         */
        void onGaveUp();
    }

    private class DebugLabelStatusListener implements StatusListener {
        @Override
        public void onStatus(String message) {
            final LabelConnector label = (LabelConnector) getState().statusLabel;
            if (label != null) {
                label.getWidget().setText(message);
            }
        }

        @Override
        public void onGaveUp() {
            final LabelConnector label = (LabelConnector) getState().statusLabel;
            if (label != null) {
                label.getWidget().setText("Failed to reconnect, all servers appear to have crashed");
            }
        }
    }

    public FailoverReconnectConnector() {
        registerRpc(FailoverReconnectClientRpc.class, this);
        statusListeners.add(new DebugLabelStatusListener());
    }

    /**
     * When {@link #startReconnecting()} is called, these listeners are notified for status updates.
     */
    public final LinkedList<StatusListener> statusListeners = new LinkedList<>();

    @SuppressWarnings("GwtInconsistentSerializableClass")
    private Reconnector reconnector = null;

    @Override
    protected void extend(ServerConnector serverConnector) {
        // this extension connector has no visual representation; it creates no divs nor other stuff.
        // Its state serves to transfer the reconnect process configuration to the client.
        // It also houses the reconnection logic, which is then used by FailoverReconnectDialog.
    }

    @Override
    public FailoverReconnectState getState() {
        return (FailoverReconnectState) super.getState();
    }

    /**
     * Checks whether there is a reconnection process ongoing.
     * @return true if we are currently reconnecting, false if not.
     */
    public boolean isReconnectionOngoing() {
        return reconnector != null;
    }

    public void startReconnecting() {
        if (isReconnectionOngoing()) {
            return;
        }
        // compute the list of reconnection URLs
        final List<String> urls = new ArrayList<>(getState().urls);
        if (getState().randomRobin) {
            shuffle(urls);
        }
        if (urls.isEmpty()) {
            for (StatusListener listener : statusListeners) {
                listener.onGaveUp();
            }
            return;
        }
        // start the reconnector process
        reconnector = new Reconnector(new StatusListener() {
            @Override
            public void onStatus(String message) {
                for (StatusListener listener : statusListeners) {
                    listener.onStatus(message);
                }
            }

            @Override
            public void onGaveUp() {
                // null the reconnector so that we can eventually start again
                reconnector = null;
                if (getState().infinite) {
                    startReconnecting();
                } else {
                    for (StatusListener listener : statusListeners) {
                        listener.onGaveUp();
                    }
                }
            }
        }, getState().pingMillis);
        reconnector.start(urls);
    }

    public void cancelReconnecting() {
        if (reconnector != null) {
            reconnector.cancel();
            reconnector = null;
        }
    }

    private static <T> void shuffle(List<T> list) {
        // GWT does not implement Collections.shuffle()
        final Random rnd = new Random();
        for (int i = list.size(); i > 1; i--)
            Collections.swap(list, i - 1, rnd.nextInt(i));
    }
}
