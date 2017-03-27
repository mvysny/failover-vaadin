package com.vaadin.failover.client;

import com.vaadin.shared.Connector;
import com.vaadin.shared.communication.SharedState;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures the FailOver dialog. Every field is set to a most reasonable defaults.
 * <p>
 * Warning: by default the {@link PingStrategy.AjaxStrategy} is used which requires CORS to be handled correctly on all servers.
 * Set {@link #pingImagePath} to a non-null value to activate the Image Ping strategy.
 * 
 * TODO mavi more information about ping strategies
 * @author mavi
 */
public class FailoverReconnectState extends SharedState {
    /**
     * The list of URLs to reconnect to, not null, may be empty.
     */
    public List<String> urls = new ArrayList<>();
    /**
     * Only for debugging/development purposes - when reconnecting, overwrites this Label's text with the status message.
     */
    public Connector statusLabel;
    /**
     * If true (the default), then during the reconnecting phase, {@link #urls} are pulled in random order. If false, {@link #urls} are pulled
     * in exactly the same order as they appear in the {@link #urls} list.
     */
    public boolean randomRobin = true;
    /**
     * If true (the default), the reconnection process is endless - it will forever try to connect to {@link #urls}. If false,
     * each URL from {@link #urls} is tried only once. After that, the reconnection dialog gives up (and calls
     * {@link FailoverReconnectConnector.StatusListener#onGaveUp()}
     */
    public boolean infinite = true;

    /**
     * When reconnecting, the URL is probed first, to check whether it is actually alive. If the server does not respond
     * within the defined period, it is considered dead and the reconnect logic moves to the next URL.
     * <p>
     * The value is in milliseconds; the default is 10 seconds.
     */
    public int pingMillis = 10000;

    /**
     * The configurable caption of the "Try Spare Servers" button. Defaults to "Try Spare Servers".
     */
    public String trySpareServersButtonCaption = "Try Spare Servers";

    /**
     * If true, the user is able to cancel the process of finding spare servers (fail-over). Defaults to true.
     */
    public boolean allowCancel = true;

    /**
     * If not null, the Image Ping strategy will be used to ping for a live server. This string is then simply added to every URL
     * in {@link #urls}. You can use e.g. "/favicon.ico" or "/VAADIN/themes/mytheme/img/app-icon.png".
     */
    public String pingImagePath = null;
}
