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
    private final String pingImagePath;
    /**
     * If the URL does not respond within this amount of millis, it is considered dead and the finder moves onto the next URL.
     */
    private final int pingMillis;
    /**
     * Currently ongoing probe. Used to cancel+cleanup the current request when the {@link #cancel()} is called.
     * <p></p>
     * Not null if {@link #start(List,boolean)} has been called.
     */
    private PingStrategy ongoingPing;

    public LiveUrlFinder(FailoverReconnectConnector.StatusListener listener, int pingMillis, String pingImagePath) {
        this.listener = listener;
        this.pingImagePath = pingImagePath;
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
    public void start(final List<String> urls, boolean automatic) {
        if (ongoingPing != null) {
            throw new IllegalStateException("Invalid state: already started");
        }
        redirectToNextWorkingUrl(urls, automatic);
    }

    /**
     * Cancels the redirection process. No further listener methods will be called.
     * Idempotent - second and further calls to this method are ignored.
     */
    public void cancel() {
        if (ongoingPing != null) {
            ongoingPing.cancel();
        }
    }

    /**
     * Tries to connect to the first URL in the list. If that fails, calls itself recursively, with the first URL omitted.
     * Fires notification messages on {@link #listener}.
     * @param remainingURLs the URLs to probe, not null, may be empty.
     */
    private void redirectToNextWorkingUrl(final List<String> remainingURLs, final boolean automatic) {
        if (remainingURLs.isEmpty()) {
            // no more URLs to reconnect. Maybe start anew? Let the owner decide.
            listener.onGaveUp();
            return;
        }

        // try to reconnect to the first URL from the list.
        final String url = remainingURLs.get(0);
        Utils.jslog("Trying to ping server at " + url);
        listener.onStatus("Trying " + url);

        // We don't want to simply redirect the browser to the URL straight away - what if the fallback server is down as well?
        // First, ping the URL whether it is alive. If it is, only then do the browser redirect.

        // Do automatic redirect if requested
        if (automatic) {
            redirectTo(url);
        }


        // There are couple of options to use when trying to ping a server, see PingStrategy for details.
        if (pingImagePath != null) {
            ongoingPing = new PingStrategy.ImageStrategy(pingImagePath);
        } else {
            ongoingPing = new PingStrategy.AjaxStrategy();
        }

        ongoingPing.ping(url, pingMillis, new PingStrategy.Callback() {
            @Override
            public void onSuccess() {
                listener.onStatus(url + " is up, redirecting");
                redirectTo(url);
            }

            @Override
            public void onFailed() {
                // try next URL
                final List<String> next = remainingURLs.subList(1, remainingURLs.size());
                redirectToNextWorkingUrl(next, automatic);
            }
        });
    }

    private void redirectTo(String url) {
        // We do not want the user to be able to navigate back - if the server would come up alive and the user back-navigated to it,
        // the session in the new server would not be transferred back and thus is perceived as lost.
        // Thus, Use GWT replace instead of assign - replace modifies the history and thus the user is not able to navigate back to the old server.
        Window.Location.replace(url);
    }
}
