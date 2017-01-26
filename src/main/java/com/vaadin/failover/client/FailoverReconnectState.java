package com.vaadin.failover.client;

import com.vaadin.shared.communication.SharedState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mavi
 */
public class FailoverReconnectState extends SharedState {
    public List<String> urls = new ArrayList<>();
}
