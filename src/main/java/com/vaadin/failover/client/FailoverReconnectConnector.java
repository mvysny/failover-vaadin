package com.vaadin.failover.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.vaadin.client.ServerConnector;
import com.vaadin.client.extensions.AbstractExtensionConnector;
import com.vaadin.failover.FailoverReconnectExtension;
import com.vaadin.shared.ui.Connect;

import java.util.List;

/**
 * @author mavi
 */
@Connect(FailoverReconnectExtension.class)
public class FailoverReconnectConnector extends AbstractExtensionConnector {

    public FailoverReconnectConnector() {
        registerRpc(FailoverReconnectClientRpc.class, new FailoverReconnectClientRpc() {
            @Override
            public void startReconnecting() {
                FailoverReconnectConnector.this.startReconnecting();
            }
        });
    }

    @Override
    protected void extend(ServerConnector serverConnector) {
        // nothing to do, really. This connector just serves as a means to transfer state so that the
        // FailoverReconnectDialog can read the state and configure itself.
    }

    @Override
    public FailoverReconnectState getState() {
        return (FailoverReconnectState) super.getState();
    }

    public void startReconnecting() {
        redirectToNextWorkingUrl(getState().urls);
    }

    private void redirectToNextWorkingUrl(final List<String> urls) {
        // @todo mavi make the order configurable: either round robin or random round robin
        if (urls.isEmpty()) {
            throw new IllegalArgumentException("Parameter urls: invalid value " + urls + ": empty");
        }
        final String url = urls.get(0);
        GWT.log("Trying to connect to a backup server at " + url);
        final RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
        builder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                GWT.log("Got response from " + url + ": " + response.getStatusCode() + " " + response.getStatusText() + ": " + response.getText());
                // any kind of response (e.g. 401 unauthorized) means that the server is alive. Redirect.
                redirectTo(url);
            }

            @Override
            public void onError(Request request, Throwable exception) {
                GWT.log("Server failed to reply: " + exception, exception);
                final List<String> next = urls.subList(1, urls.size());
                if (next.isEmpty()) {
                    // @todo mavi add support for one-shot vs continuous checks
                    Window.alert("Unfortunately I was unable to connect to any backup servers. Is the network up?");
                } else {
                    redirectToNextWorkingUrl(next);
                }
            }
        });
        builder.setTimeoutMillis(10000); // 10 seconds, @todo mavi make this configurable
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
}
