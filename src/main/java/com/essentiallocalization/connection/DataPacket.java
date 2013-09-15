package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public class DataPacket implements Packet {
    public static final int HEADER_SIZE = 36;
    public static final int MAX_PAYLOAD = BUFFER_SIZE - HEADER_SIZE;

    public final byte type;
    public final int packetIndex;
    public final int msgIndex;
    public final byte msgAttempt;
    public final byte msgPart;
    public final byte msgParts;
    public final long sent;
    public long received;
    public long confirmed;
    public final byte[] payload;

    public DataPacket (byte type, int packetIndex, int msgIndex, byte msgAttempt, byte msgPart, byte msgParts, byte[] payload) {
        this.type = type;
        this.packetIndex = packetIndex;
        this.msgIndex = msgIndex;
        this.msgAttempt = msgAttempt;
        this.msgPart = msgPart;
        this.msgParts = msgParts;
        this.sent = System.nanoTime();
        this.received = 0;
        this.confirmed = 0;
        this.payload = payload;
    }

    public DataPacket(byte[] fromDataPacket) {
        ByteBuffer bb = ByteBuffer.wrap(fromDataPacket);
        type = bb.get();
        packetIndex = bb.getInt();
        msgIndex = bb.getInt();
        msgAttempt = bb.get();
        msgPart = bb.get();
        msgParts = bb.get();
        sent = bb.getLong();
        received = bb.getLong();
        confirmed = bb.getLong();
        payload = new byte[bb.limit() - bb.position()];
        bb.get(payload);
    }

    @Override
    public byte[] getBytes() {
        return ByteBuffer.allocate(HEADER_SIZE + payload.length)
                .put(type)
                .putInt(packetIndex)
                .putInt(msgIndex)
                .put(msgAttempt)
                .put(msgPart)
                .put(msgParts)
                .putLong(sent)
                .putLong(received)
                .putLong(confirmed)
                .put(payload)
                .array();
    }
}
