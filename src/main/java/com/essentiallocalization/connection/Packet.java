package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public abstract class Packet {
    public static final int BUFFER_SIZE = 1024;

    protected static final int HEADER_SIZE = 12;

    public static final byte TYPE_TEST = 1;
    public static final byte TYPE_ACK = 2;
    public static final byte TYPE_MSG = 3;

    public byte type;
    public int packetIndex;
    public int msgIndex;
    public byte msgAttempt;
    public byte msgPart;
    public byte msgParts;

    public static byte getType(byte[] packet) {
        return packet[0];
    }

    public static String trim(byte[] payload) {
        int i = 0;
        for (byte b : payload) {
            if (b == 0) {
                break;
            }
            i++;
        }
        return new String(ByteBuffer.wrap(payload, 0, i).array());
    }

    Packet(byte type, int packetIndex, int msgIndex, byte msgAttempt, byte msgPart, byte msgParts) {
        this.type = type;
        this.packetIndex = packetIndex;
        this.msgIndex = msgIndex;
        this.msgAttempt = msgAttempt;
        this.msgPart = msgPart;
        this.msgParts = msgParts;
    }

    Packet(byte[] fromPacket) {
        ByteBuffer bb = ByteBuffer.wrap(fromPacket);
        type = bb.get();
        packetIndex = bb.getInt();
        msgIndex = bb.getInt();
        msgAttempt = bb.get();
        msgPart = bb.get();
        msgParts = bb.get();
    }

    public abstract int size();

    public abstract byte[] onSend();

    protected final ByteBuffer getBuffer() {
        return ByteBuffer.allocate(size())
                .put(type)
                .putInt(packetIndex)
                .putInt(msgIndex)
                .put(msgAttempt)
                .put(msgPart)
                .put(msgParts);
    }
}
