package com.vaadin.failover.client;

import com.vaadin.shared.communication.ClientRpc;

/**
 * @author mavi
 */
public interface FailoverReconnectClientRpc extends ClientRpc {
    void startReconnecting();
}
