package com.vaadin.failover.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Pings given http or https URL. Notifies the caller whether the ping was successful or not. There are multiple strategies
 * with varying requirements, summed here: http://stackoverflow.com/questions/4282151/is-it-possible-to-ping-a-server-from-javascript
 * <h3>Non-viable strategies</h3>
 * <ul>
 *     <li>Pinging a non-websocket endpoint with WebSocket fails; this type of failure cannot be differentiated from net::ERR_CONNECTION_REFUSED.</li>
 * </ul>
 * @author mavi
 */
public interface PingStrategy {
    /**
     * Pings given absolute URL. May be called only once per object instance.
     *
     * @param url      the absolute URL, must be http or https.
     * @param timeoutMillis max amount of millis to wait for the server response. If the time is exceeded, {@link Callback#onFailed()} is reported
     *                      and the ping operation is canceled. No further {@link Callback#onSuccess()} are reported even if the server responds.
     * @param callback the callback to call.
     */
    void ping(String url, int timeoutMillis, Callback callback);

    /**
     * Cancels ongoing ping. Causes ongoing ping to not invoke any callbacks. No-op if there is no ping ongoing.
     */
    void cancel();

    interface Callback {
        /**
         * Invoked when the URL is alive. The definition of alive is that there seems to be something running and
         * responding to a TCP-IP connection probe. It doesn't matter whether it replies with 404 or 200 or in any other way:
         * the only requirement is that a few bytes are sent as a response. It may be a SSH service for all we know.
         * This even includes a SSL handshake failure errors, since those indicate that there's something running.
         */
        void onSuccess();

        /**
         * The URL is dead, there's nothing running, I have failed to connect to that endpoint.
         * The browser probably got net::ERR_CONNECTION_REFUSED or net::ERR_NAME_NOT_RESOLVED
         * (if the DNS name is not resolvable). Called even in case of timeout.
         */
        void onFailed();
    }

    /**
     * Performs a HTTP(s) GET via XMLHttpRequest.
     * <h3>Prerequisites</h3>
     * Calling GET/HEAD/OPTIONS via XMLHttpRequest fails because of CORS; this type of failure cannot be differentiated from net::ERR_CONNECTION_REFUSED.
     * The target site must therefore have CORS enabled and properly configured, otherwise all I receive is a generic error and this strategy will fail
     * to ping even a live site and will incorrectly report a failure.
     * 
     * TODO mavi introduce a Servlet which properly configures CORS; maybe based on https://vaadin.com/blog/-/blogs/using-cors-with-vaadin
     */
    class AjaxStrategy implements PingStrategy {

        /**
         * Currently ongoing probe. Used to cancel+cleanup the current request when the {@link #cancel()} is called.
         */
        private Request ongoingRequest;

        /**
         * True if {@link #cancel()} has been called.
         */
        private boolean canceled = false;

        @Override
        public void ping(final String url, final int timeoutMillis, final Callback callback) {
            if (ongoingRequest != null) {
                throw new IllegalStateException("Invalid state: another ping is ongoing");
            }
            // unfortunately, using the preflights OPTIONS method won't fool the browser - it still shows that
            // XMLHttpRequest cannot load http://xyz/. Response to preflight request doesn't pass access control check: No 'Access-Control-Allow-Origin' header is present on the requested resource. Origin 'http://localhost:9998' is therefore not allowed access. The response had HTTP status code 405.
            // so we can't use this method to differentiate between net:: issue and CORS issue.
//        final RequestBuilder builder = new RequestBuilder("OPTIONS", url) {};

            // HEAD doesn't work either.
//        final RequestBuilder builder = new RequestBuilder("HEAD", url) {};

            final RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
            builder.setCallback(new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (canceled) {
                        return;
                    }
                    ongoingRequest = null;
                    Utils.jslog("Got response from " + url + ": " + response.getStatusCode() + " " + response.getStatusText() + ": " + response.getText() + ", headers=" + response.getHeadersAsString());
                    if (response.getStatusCode() == 0) {
                        // Chrome reports all net:: issues like net::ERR_CONNECTION_REFUSED or net::ERR_NAME_NOT_RESOLVED like this.
                        // This usually means that the server is down and we'll have to try the next one.
                        // The trouble with this approach is that the CORS error is reported this way as well.
                        // However, the server should have been correctly configured with CORS in mind. Therefore, report a failure.
                        callback.onFailed();
                        return;
                    }
                    // any proper kind of response (e.g. 401 unauthorized) means that the server is alive. Redirect.
                    callback.onSuccess();
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    if (canceled) {
                        return;
                    }
                    ongoingRequest = null;
                    Utils.jslog("Server failed to reply", exception);
                    callback.onFailed();
                }
            });
            builder.setTimeoutMillis(timeoutMillis);
//        builder.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            try {
                ongoingRequest = builder.send();
            } catch (Exception e) {
                Utils.jslog("Failed to ping server, redirecting blindly to " + url, e);
                callback.onSuccess();
            }
        }

        @Override
        public void cancel() {
            if (ongoingRequest != null) {
                canceled = true;
                ongoingRequest.cancel();
                ongoingRequest = null;
            }
        }
    }

    /**
     * Uses the JavaScript Image onload/onerror as desribed here: http://stackoverflow.com/a/11941783/377320
     * The strategy can be tested here: http://jsfiddle.net/GSSCD/203/
     * <h3>Prerequisites</h3>
     * The new Image() trick works only with images - onerror is simply reported even for valid web pages, probably because a html page is provided instead of an image.
     * This strategy bypasses CORS but requires the endpoint to be an image, otherwise the strategy will fail and will incorrectly
     * report a failure even on a live site.
     */
    class ImageStrategy implements PingStrategy {
        /**
         * Appended to the URL being pinged, must point to an image.
         */
        private final String pathToImage;

        private Image image;
        private Timer timeout;

        /**
         * @param pathToImage Appended to the URL being pinged, must point to an image.
         */
        public ImageStrategy(String pathToImage) {
            this.pathToImage = pathToImage;
        }

        @Override
        public void ping(final String url, int timeoutMillis, final Callback callback) {
            if (image != null) {
                throw new IllegalStateException("Invalid state: a ping is ongoing");
            }
            // make sure that we don't get some cached image - pollute the cache by adding some random (or changing) number to the URL.
            // the ?forcenocache has no meaning really.
            final String imageUrl = url + pathToImage + "?forcenocache=" + System.currentTimeMillis();
            Utils.jslog("Trying to download an image from " + imageUrl + " to verify whether the server is alive");
            image = GWT.create(Image.class);
            image.setVisible(false);
            // must be attached to the DOM tree otherwise GWT will not fire any events :(
            RootPanel.get().add(image);
            image.addLoadHandler(new LoadHandler() {
                @Override
                public void onLoad(LoadEvent event) {
                    if (image == null) {
                        Utils.jslog("ONLOAD CANCELED");
                        // canceled
                        return;
                    }
                    timeout.cancel();
                    Utils.jslog(url + " is live! " + event);
                    callback.onSuccess();
                }
            });
            image.addErrorHandler(new ErrorHandler() {
                @Override
                public void onError(ErrorEvent event) {
                    if (image == null) {
                        Utils.jslog("onError CANCELED");
                        // canceled
                        return;
                    }
                    cancel();
                    Utils.jslog("Failed to obtain image from " + imageUrl + ": " + event);
                    callback.onFailed();
                }
            });
            // emulate timeout with a timer
            timeout = new Timer() {
                @Override
                public void run() {
                    Utils.jslog("Timeout obtaining image from " + imageUrl);
                    cancel();
                    callback.onFailed();
                }
            };
            timeout.schedule(timeoutMillis);
            image.setUrl(imageUrl);
        }

        @Override
        public void cancel() {
            if (image != null) {
                RootPanel.get().remove(image);
                image = null;
                timeout.cancel();
                timeout = null;
            }
        }
    }
}
