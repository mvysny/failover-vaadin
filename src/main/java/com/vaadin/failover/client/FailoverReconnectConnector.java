package com.vaadin.failover.client;

import com.google.gwt.user.client.Timer;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.client.ui.label.LabelConnector;
import com.vaadin.failover.FailoverReconnectExtension;
import com.vaadin.shared.ui.Connect;

import java.util.*;

/**
 * Implements the fail-over logic. {@link FailoverReconnectDialog} expects this extension to be attached to the UI class.
 * @author mavi
 */
@Connect(FailoverReconnectExtension.class)
public class FailoverReconnectConnector extends AbstractExtensionConnector implements FailoverReconnectClientRpc {

    public interface StatusListener {
        /**
         * Called from {@link FailoverReconnectConnector#startFailOver()} to update the reconnection state.
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
     * When {@link #startFailOver()} is called, these listeners are notified for status updates.
     */
    public final LinkedList<StatusListener> statusListeners = new LinkedList<>();

    /**
     * Only applicable when {@link FailoverReconnectState#infinite} is true.
     * When none of the URLs are up, back off a bit before trying again.
     * This is especially useful after getting a rapid stream of "connection refused" from the browser.
     */
    private com.google.gwt.user.client.Timer startOverBackOffTimer = null;

    @SuppressWarnings("GwtInconsistentSerializableClass")
    private LiveUrlFinder liveUrlFinder = null;

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
        return liveUrlFinder != null;
    }

    @Override
    public void startFailOver() {
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
        // start the liveUrlFinder process
        final StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(String message) {
                for (StatusListener listener : statusListeners) {
                    listener.onStatus(message);
                }
            }

            @Override
            public void onGaveUp() {
                // we ran out of URLs.
                // null the liveUrlFinder so that we can eventually start again
                liveUrlFinder = null;
                if (getState().infinite) {
                    // repeat the whole process but back off a bit
                    // useful after getting a rapid stream of "connection refused" from the browser.
                    startOverBackOffTimer = new Timer() {
                        @Override
                        public void run() {
                            startFailOver();
                        }
                    };
                    startOverBackOffTimer.schedule(3000);
                    for (StatusListener listener : statusListeners) {
                        listener.onStatus("All servers appear to be down, retrying");
                    }
                } else {
                    for (StatusListener listener : statusListeners) {
                        listener.onGaveUp();
                    }
                }
            }
        };
        liveUrlFinder = new LiveUrlFinder(listener, getState().pingMillis, getState().pingImagePath);
        liveUrlFinder.start(urls);
    }

    @Override
    public void cancelFailOver() {
        if (liveUrlFinder != null) {
            liveUrlFinder.cancel();
            liveUrlFinder = null;
        }
        if (startOverBackOffTimer != null) {
            startOverBackOffTimer.cancel();
            startOverBackOffTimer = null;
        }
    }

    private static <T> void shuffle(List<T> list) {
        // GWT does not implement Collections.shuffle()
        final Random rnd = new Random();
        for (int i = list.size(); i > 1; i--)
            Collections.swap(list, i - 1, rnd.nextInt(i));
    }
}
