package com.flat.bluetooth.connection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jake on 9/14/13.
 */
public final class Message {
    public static final class MessageTooLongException extends Exception {}

    private final List<byte[]> mParts;
    private final String mFullMsg;

    public Message(String msg) throws MessageTooLongException {
        mFullMsg = msg;
        mParts = new ArrayList<byte[]>();
        int start, end;
        int count = 0;
        while (true) {
            start = count * DataPacket.MAX_PAYLOAD;
            end = start + DataPacket.MAX_PAYLOAD;
            if (msg.length() >= end) {
                mParts.add(msg.substring(start, end).getBytes());
            } else {
                mParts.add(msg.substring(start).getBytes());
                break;
            }
            count++;
        }
        if (mParts.size() > Byte.MAX_VALUE) {
            throw new MessageTooLongException();
        }
    }

    public byte[] get(int partIndex) {
        return mParts.get(partIndex);
    }

    public List<byte[]> getParts() {
        return mParts;
    }

    public int size() {
        return mParts.size();
    }

    @Override
    public String toString() {
        return mFullMsg;
    }
}
