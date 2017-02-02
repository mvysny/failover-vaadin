package com.vaadin.failover;

import com.vaadin.failover.client.FailoverReconnectClientRpc;
import com.vaadin.failover.client.FailoverReconnectState;
import com.vaadin.server.AbstractExtension;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The extension which handles the failover. To use this extension, perform the following steps:
 * <ul>
 *     <li>in your {@link UI#init(VaadinRequest)} call {@link #addTo(UI)}</li>
 *     <li>set the redirect URLs via {@link #setUrls(List)}</li>
 *     <li>Optionally configure other aspects of the fail-over process.</li>
 *     <li>Most important, include this widgetset in your widgetset</li>
 * </ul>
 * Once you include this widgetset, the standard Vaadin Reconnect dialog will be extended to also perform the FailOver logic.
 * Once the server crashes, the user will be able to press the "Try Spare Servers" button which will initiate the failover.
 * @author mavi
 */
public class FailoverReconnectExtension extends AbstractExtension {
    public void extend(UI ui) {
        super.extend(ui);
    }

    public static FailoverReconnectExtension addTo(UI ui) {
        final FailoverReconnectExtension extension = new FailoverReconnectExtension();
        extension.extend(ui);
        return extension;
    }

    @Override
    protected FailoverReconnectState getState() {
        return (FailoverReconnectState) super.getState();
    }

    @Override
    protected FailoverReconnectState getState(boolean markAsDirty) {
        return (FailoverReconnectState) super.getState(markAsDirty);
    }

    /**
     * Sets the list of URLs to reconnect to. You can add all URLs here (including the main server URL). By default this list is empty.
     * @param urls the list of URLs, not null, may be empty. All URLs must start with http:// or https://
     */
    public void setUrls(List<String> urls) {
        for (String url : urls) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                // Chrome will complain regarding same origin policy if the URL has no proper prefix.
                throw new IllegalArgumentException("Parameter urls: invalid value " + url + ": the URL must start either with http:// or https://");
            }
        }
        getState().urls.clear();
        getState().urls.addAll(urls);
    }

    /**
     * Returns the current list of URLs we will reconnect to.
     * @return the list of URLs, not null, initially empty.
     */
    public List<String> getUrls() {
        return new ArrayList<>(getState(false).urls);
    }

    /**
     * Sets the "status" label. When {@link #startFailOver()} is called, this label will gradually show the status.
     * Only used for testing/development purposes.
     * @param statusLabel the label to show the progress, may be null.
     */
    void setStatusLabel(Label statusLabel) {
        getState().statusLabel = statusLabel;
    }

    /**
     * Begins the failover process. Only used for testing/development purposes.
     */
    void startFailOver() {
        getRpcProxy(FailoverReconnectClientRpc.class).startFailOver();
    }

    /**
     * Cancels any ongoing failover process. Only used for testing/development purposes.
     */
    void cancelFailOver() {
        getRpcProxy(FailoverReconnectClientRpc.class).cancelFailOver();
    }

    /**
     * If true, then during the reconnecting phase, {@link #getUrls()} are pulled in random order. If false, {@link #getUrls()} are pulled
     * in exactly the same order as they appear in the {@link #getUrls()} list.
     * @return true if the reconnect should follow the random robin algorithm, false if round robin
     */
    public boolean isRandomRobin() {
       return getState(false).randomRobin;
    }

    /**
     * If true, then during the reconnecting phase, {@link #getUrls()} are pulled in random order. If false, {@link #getUrls()} are pulled
     * in exactly the same order as they appear in the {@link #getUrls()} list.
     * @param randomRobin if the reconnect should follow the random robin algorithm, false if round robin
     */
    public void setRandomRobin(boolean randomRobin) {
        getState().randomRobin = randomRobin;
    }

    /**
     * If true (the default), the reconnection process is endless - it will forever try to connect to {@link #getUrls()}. If false,
     * each URL from {@link #getUrls()} is tried only once. After that, the reconnection dialog gives up (and shows "Failed to reconnect, all servers appear to have crashed").
     * @return true if the reconnect process should be repeated indefinitely, false if not.
     */
    public boolean isInfinite() {
        return getState(false).infinite;
    }

    /**
     * If true (the default), the reconnection process is endless - it will forever try to connect to {@link #getUrls()}. If false,
     * each URL from {@link #getUrls()} is tried only once. After that, the reconnection dialog gives up (and shows "Failed to reconnect, all servers appear to have crashed").
     * @param infinite true if the reconnect process should be repeated indefinitely, false if not.
     */
    public void setInfinite(boolean infinite) {
        getState().infinite = infinite;
    }

    /**
     * When reconnecting, the URL is probed first, to check whether it is actually alive. If the server does not respond
     * within the defined period, it is considered dead and the reconnect logic moves to the next URL.
     * @return The value in milliseconds; the default is 10 seconds.
     */
    public int getPingMillis() {
        return getState(false).pingMillis;
    }

    /**
     * When reconnecting, the URL is probed first, to check whether it is actually alive. If the server does not respond
     * within the defined period, it is considered dead and the reconnect logic moves to the next URL.
     * @param pingMillis The value in milliseconds; the default is 10 seconds. Must not be negative.
     */
    public void setPingMillis(int pingMillis) {
        if (pingMillis < 0) {
            throw new IllegalArgumentException("Parameter pingMillis: invalid value " + pingMillis + ": must be 0 or greater");
        }
        getState().pingMillis = pingMillis;
    }

    /**
     * The configurable caption of the "Try Spare Servers" button. Defaults to "Try Spare Servers".
     * @return the caption, not null.
     */
    public String getTrySpareServersButtonCaption() {
        return getState(false).trySpareServersButtonCaption;
    }

    /**
     * The configurable caption of the "Try Spare Servers" button. Defaults to "Try Spare Servers".
     * @param trySpareServersButtonCaption
     */
    public void setTrySpareServersButtonCaption(String trySpareServersButtonCaption) {
        Objects.requireNonNull(trySpareServersButtonCaption);
        getState().trySpareServersButtonCaption = trySpareServersButtonCaption;
    }

    /**
     * If true, the user is able to cancel the process of finding spare servers (fail-over). Defaults to true.
     * @return false if there will not be the Cancel button visible.
     */
    public boolean isAllowCancel() {
        return getState(false).allowCancel;
    }

    /**
     * If true, the user is able to cancel the process of finding spare servers (fail-over). Defaults to true.
     * @param allowCancel false if there will not be the Cancel button visible.
     */
    public void setAllowCancel(boolean allowCancel) {
        getState().allowCancel = allowCancel;
    }

    /**
     * If not null, the Image Ping strategy will be used to ping for a live server. This string is then simply added to every URL
     * in {@link #getUrls()}. You can use e.g. "/favicon.ico" or "/VAADIN/themes/mytheme/img/app-icon.png".
     * @return path to image, null by default.
     */
    public String getPingImagePath() {
        return getState(false).pingImagePath;
    }

    /**
     * If not null, the Image Ping strategy will be used to ping for a live server. This string is then simply added to every URL
     * in {@link #getUrls()}. You can use e.g. "/favicon.ico" or "/VAADIN/themes/mytheme/img/app-icon.png".
     * @param pingImagePath path to image, null by default.
     */
    public void setPingImagePath(String pingImagePath) {
        getState().pingImagePath = pingImagePath;
    }
}
