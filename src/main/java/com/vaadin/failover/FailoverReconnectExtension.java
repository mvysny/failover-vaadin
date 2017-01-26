package com.vaadin.failover;

import com.vaadin.failover.client.FailoverReconnectClientRpc;
import com.vaadin.failover.client.FailoverReconnectState;
import com.vaadin.server.AbstractExtension;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import java.util.ArrayList;
import java.util.List;

/**
 * The extension which handles the reconnection. To use this extension, perform the following steps:
 * <ul>
 *     <li>in your {@link UI#init(VaadinRequest)} call {@link #addTo(UI)}</li>
 *     <li>set the redirect URLs via {@link #setUrls(List)}</li>
 * </ul>
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
     * @param urls the list of URLs, not null, may be empty.
     */
    public void setUrls(List<String> urls) {
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
     * Sets the "status" label. When {@link #startReconnecting()} is called, this label will show the
     * @param statusLabel
     */
    void setStatusLabel(Label statusLabel) {
        getState().statusLabel = statusLabel;
    }

    void startReconnecting() {
        getRpcProxy(FailoverReconnectClientRpc.class).startReconnecting();
    }
}
