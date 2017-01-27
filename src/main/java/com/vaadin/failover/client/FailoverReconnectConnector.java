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
public class FailoverReconnectConnector extends AbstractExtensionConnector {

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
        registerRpc(FailoverReconnectClientRpc.class, new FailoverReconnectClientRpc() {
            @Override
            public void startReconnecting() {
                FailoverReconnectConnector.this.startReconnecting();
            }
        });
        statusListeners.add(new DebugLabelStatusListener());
    }

    /**
     * When {@link #startReconnecting()} is called, these listeners are notified for status updates.
     */
    public final LinkedList<StatusListener> statusListeners = new LinkedList<>();

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
     * Begins the reconnection process to another server. As the reconnection process progresses, {@link #statusListeners} are notified.
     */
    public void startReconnecting() {
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
        redirectToNextWorkingUrl(urls);
    }

    private void notifyStatus(String message) {
        for (StatusListener listener : statusListeners) {
            listener.onStatus(message);
        }
    }

    /**
     * Tries to connect to the first URL in the list. If that fails, calls itself recursively, with the first URL omitted.
     * Fires notification messages on {@link #statusListeners}.
     * @param remainingURLs the URLs to probe, not null, may be empty.
     */
    private void redirectToNextWorkingUrl(final List<String> remainingURLs) {
        if (remainingURLs.isEmpty()) {
            if (getState().infinite) {
                startReconnecting();
            } else {
                for (StatusListener listener : statusListeners) {
                    listener.onGaveUp();
                }
            }
            return;
        }
        final String url = remainingURLs.get(0);
        GWT.log("Trying to connect to a backup server at " + url);
        notifyStatus("Reconnecting to " + url);
        final RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
        builder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("Got response from " + url + ": " + response.getStatusCode() + " " + response.getStatusText() + ": " + response.getText());
                notifyStatus(url + " is up, redirecting");
                // any kind of response (e.g. 401 unauthorized) means that the server is alive. Redirect.
                redirectTo(url);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                GWT.log("Server failed to reply: " + exception, exception);
                final List<String> next = remainingURLs.subList(1, remainingURLs.size());
                redirectToNextWorkingUrl(next);
            }
        });
        builder.setTimeoutMillis(Math.max(0, getState().pingMillis));
        builder.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        try {
            GWT.log("Trying to ping backup server " + url);
            builder.send();
        } catch (Exception e) {
            GWT.log("Failed to ping server, redirecting blindly to " + url + ": " + e, e);
            redirectTo(url);
        }
    }

    private void redirectTo(String url) {
        // We do not want the user to be able to navigate back - if the server would come up alive and the user back-navigated to it,
        // the session in the new server would not be transferred back and thus is perceived as lost.
        // Thus, Use GWT replace instead of assign - replace modifies the history and thus the user is not able to navigate back to the old server.
        Window.Location.replace(url);
    }

    private static <T> void shuffle(List<T> list) {
        final Random rnd = new Random();
        for (int i = list.size(); i > 1; i--)
            Collections.swap(list, i - 1, rnd.nextInt(i));
    }
}
