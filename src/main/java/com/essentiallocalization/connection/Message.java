package com.essentiallocalization.connection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jake on 9/14/13.
 */
public class Message {
    public static final class MessageTooLongException extends Exception {}

    public final List<byte[]> parts;
    private final String msg;

    public Message(String msg) throws MessageTooLongException {
        this.msg = msg;
        parts = new ArrayList<byte[]>();
        int start, end;
        int count = 0;
        while (true) {
            start = count * DataPacket.MAX_PAYLOAD;
            end = start + DataPacket.MAX_PAYLOAD;
            if (msg.length() >= end) {
                parts.add(msg.substring(start, end).getBytes());
            } else {
                parts.add(msg.substring(start).getBytes());
                break;
            }
            count++;
        }
        if (parts.size() > Byte.MAX_VALUE) {
            throw new MessageTooLongException();
        }
    }

    @Override
    public String toString() {
        return msg;
    }
}
