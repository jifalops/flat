package com.essentiallocalization.connection;

/**
 * Created by Jake on 9/14/13.
 */
public interface Packet {
    int BUFFER_SIZE = 1024;

    byte TYPE_TEST = 1;
    byte TYPE_ACK = 2;
    byte TYPE_MSG = 3;

    int INDEX_OF_TYPE = 0;

    byte[] getBytes();
}
