package com.vaadin.failover.client;

import com.google.gwt.core.client.GWT;

/**
 * @author mavi
 */
public class Utils {
    public static void jslog(String message, Throwable ex) {
        GWT.log(message, ex);
        jslog(message + ": " + ex);
    }
    public static native void jslog(String message) /*-{
        console.log(message);
    }-*/;
}
