package com.vaadin.failover.client;

import com.vaadin.shared.communication.ClientRpc;

/**
 * @author mavi
 */
public interface FailoverReconnectClientRpc extends ClientRpc {
    /**
     * Begins the reconnection process to another server. As the reconnection process progresses, status listeners are notified.
     * <p></p>
     * If the reconnecting process is currently ongoing, this call does nothing.
     */
    void startReconnecting();
    /**
     * Cancels the currently ongoing reconnection process. Does nothing if there is no reconnection process ongoing.
     */
    void cancelReconnecting();
}
