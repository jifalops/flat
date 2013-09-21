package com.essentiallocalization.connection;

/**
 * Created by Jake on 9/20/13.
 */
public interface ConnectionFilter {
    boolean isAllowed(String address);
}
