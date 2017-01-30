package com.vaadin.failover.client;

import com.vaadin.shared.communication.ClientRpc;

/**
 * @author mavi
 */
public interface FailoverReconnectClientRpc extends ClientRpc {
    /**
     * Begins the fail-over process. As the fail-over process progresses, status listeners are notified.
     * <p></p>
     * If the failover process is currently ongoing, this call does nothing.
     */
    void startFailOver();
    /**
     * Cancels the currently ongoing failover process. Does nothing if there is no failover process ongoing.
     */
    void cancelFailOver();
}
