package com.vaadin.failover.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import java.util.List;

/**
 * Performs one reconnection cycle. Cancelable. Can be started, then canceled. Once canceled, cannot be started again.
 * @author mavi
 */
final class Reconnector {

    private final FailoverReconnectConnector.StatusListener listener;
    private final int pingMillis;
    private boolean started = false;
    private boolean canceled = false;
    private Request ongoingRequest;

    public Reconnector(FailoverReconnectConnector.StatusListener listener, int pingMillis) {
        this.listener = listener;
        if (listener == null) {
            throw new IllegalArgumentException("Parameter listener: invalid value " + listener + ": must not be null");
        }
        this.pingMillis = pingMillis;
        if (pingMillis < 0) {
            throw new IllegalArgumentException("Parameter pingMillis: invalid value " + pingMillis + ": must be 0 or greater");
        }
    }

    public void start(final List<String> remainingURLs) {
        if (canceled) {
            throw new IllegalStateException("Invalid state: canceled");
        }
        if (started) {
            throw new IllegalStateException("Invalid state: already started");
        }
        started = true;
        redirectToNextWorkingUrl(remainingURLs);
    }

    /**
     * Cancels the redirection process. No further listener methods will be called.
     * Idempotent - second and further calls to this method are ignored.
     */
    public void cancel() {
        if (!canceled) {
            canceled = true;
            if (ongoingRequest != null) {
                ongoingRequest.cancel();
                ongoingRequest = null;
            }
        }
    }

    /**
     * Tries to connect to the first URL in the list. If that fails, calls itself recursively, with the first URL omitted.
     * Fires notification messages on {@link #listener}.
     * @param remainingURLs the URLs to probe, not null, may be empty.
     */
    private void redirectToNextWorkingUrl(final List<String> remainingURLs) {
        if (canceled) {
            return;
        }
        if (remainingURLs.isEmpty()) {
            // no more URLs to reconnect. Maybe start anew? Let the owner decide.
            listener.onGaveUp();
            return;
        }

        // try to reconnect to the first URL from the list.
        final String url = remainingURLs.get(0);
        GWT.log("Trying to connect to a backup server at " + url);
        listener.onStatus("Reconnecting to " + url);
        // We don't want to simply redirect the browser to the URL straight away - that would kill us.
        // First, ping the URL whether it is alive. If it is, only then do the browser redirect.
        final RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
        builder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if (canceled) {
                    return;
                }
                ongoingRequest = null;
                GWT.log("Got response from " + url + ": " + response.getStatusCode() + " " + response.getStatusText() + ": " + response.getText());
                listener.onStatus(url + " is up, redirecting");
                // any kind of response (e.g. 401 unauthorized) means that the server is alive. Redirect.
                redirectTo(url);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                if (canceled) {
                    return;
                }
                ongoingRequest = null;
                GWT.log("Server failed to reply: " + exception, exception);
                final List<String> next = remainingURLs.subList(1, remainingURLs.size());
                redirectToNextWorkingUrl(next);
            }
        });
        builder.setTimeoutMillis(pingMillis);
        builder.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        try {
            GWT.log("Trying to ping backup server " + url);
            ongoingRequest = builder.send();
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
}
