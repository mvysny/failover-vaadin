package com.vaadin.failover;

import com.vaadin.failover.client.FailoverReconnectClientRpc;
import com.vaadin.failover.client.FailoverReconnectState;
import com.vaadin.server.AbstractExtension;
import com.vaadin.ui.UI;

import java.util.List;

/**
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
     * Sets the list of URLs to reconnect to.
     * @param urls the list of URLs, not null, may be empty.
     */
    public void setUrls(List<String> urls) {
        getState().urls.clear();
        getState().urls.addAll(urls);
    }

    public void startReconnecting() {
        getRpcProxy(FailoverReconnectClientRpc.class).startReconnecting();
    }
}
