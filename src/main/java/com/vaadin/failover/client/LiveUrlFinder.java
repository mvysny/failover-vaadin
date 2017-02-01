package com.vaadin.failover.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;

import java.util.List;

/**
 * Performs one fail-over cycle over given list of URLs. Every URL is pinged first, whether it is actually live.
 * <p></p>
 * Cancelable. Can be started, then canceled. Once canceled, cannot be started again.
 * @author mavi
 */
final class LiveUrlFinder {
    /**
     * Notifies this listener of finder's current status.
     */
    private final FailoverReconnectConnector.StatusListener listener;
    /**
     * If the URL does not respond within this amount of millis, it is considered dead and the finder moves onto the next URL.
     */
    private final int pingMillis;
    /**
     * True if {@link #start(List)} has been called.
     */
    private boolean started = false;
    /**
     * True if {@link #cancel()} has been called.
     */
    private boolean canceled = false;
    /**
     * Currently ongoing probe. Used to cancel+cleanup the current request when the {@link #cancel()} is called.
     */
    private Request ongoingRequest;

    /**
     * Creates the finder.
     * @param listener
     * @param pingMillis
     */
    public LiveUrlFinder(FailoverReconnectConnector.StatusListener listener, int pingMillis) {
        this.listener = listener;
        if (listener == null) {
            throw new IllegalArgumentException("Parameter listener: invalid value " + listener + ": must not be null");
        }
        this.pingMillis = pingMillis;
        if (pingMillis < 0) {
            throw new IllegalArgumentException("Parameter pingMillis: invalid value " + pingMillis + ": must be 0 or greater");
        }
    }

    /**
     * Probes given list of URLs and redirects the browser automatically to the first live URL.
     * @param urls the URLs to probe, in this order.
     */
    public void start(final List<String> urls) {
        if (canceled) {
            throw new IllegalStateException("Invalid state: canceled");
        }
        if (started) {
            throw new IllegalStateException("Invalid state: already started");
        }
        started = true;
        redirectToNextWorkingUrl(urls);
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
        Utils.jslog("Trying to ping server at " + url);
        listener.onStatus("Trying " + url);
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
                Utils.jslog("Got response from " + url + ": " + response.getStatusCode() + " " + response.getStatusText() + ": " + response.getText());
                if (response.getStatusCode() == 0) {
                    // Chrome reports net::ERR_CONNECTION_REFUSED like this. This means that the server is down and we'll have to try the next one.
                    tryNext();
                    return;
                }
                listener.onStatus(url + " is up, redirecting");
                // any proper kind of response (e.g. 401 unauthorized) means that the server is alive. Redirect.
                redirectTo(url);
            }

            private void tryNext() {
                final List<String> next = remainingURLs.subList(1, remainingURLs.size());
                redirectToNextWorkingUrl(next);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                if (canceled) {
                    return;
                }
                ongoingRequest = null;
                Utils.jslog("Server failed to reply", exception);
                tryNext();
            }
        });
        builder.setTimeoutMillis(pingMillis);
        builder.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        try {
            ongoingRequest = builder.send();
        } catch (Exception e) {
            Utils.jslog("Failed to ping server, redirecting blindly to " + url, e);
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
