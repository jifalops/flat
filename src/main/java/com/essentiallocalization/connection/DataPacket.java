package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public final class DataPacket extends Packet {
    public static final int HEADER_SIZE = Packet.HEADER_SIZE + 32;
    public static final int MAX_PAYLOAD = BUFFER_SIZE - HEADER_SIZE;

    public long sent;
    public volatile long received;
    public volatile long resent;
    public volatile long confirmed;
    public final byte[] payload;

    public DataPacket (byte type, int packetIndex, int msgIndex, byte msgAttempt, byte msgPart, byte msgParts, byte[] payload) {
        super(type, packetIndex, msgIndex, msgAttempt, msgPart, msgParts);
        this.payload = payload;
    }

    public DataPacket(byte[] fromDataPacket) {
        super(fromDataPacket);
        ByteBuffer bb = ByteBuffer.wrap(fromDataPacket);
        bb.position(Packet.HEADER_SIZE);
        sent = bb.getLong();
        received = bb.getLong();
        resent = bb.getLong();
        confirmed = bb.getLong();
        payload = new byte[bb.limit() - bb.position()];
        bb.get(payload);
    }

    @Override
    public byte[] onSend() {
        return getBuffer()
                .putLong(sent = System.nanoTime())
                .putLong(received)
                .putLong(resent)
                .putLong(confirmed)
                .put(payload)
                .array();
    }

    @Override
    public int size() {
        return HEADER_SIZE + payload.length;
    }
}
