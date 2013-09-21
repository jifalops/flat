package com.essentiallocalization.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public final class DataPacket extends Packet {
    public static final int HEADER_SIZE = Packet.HEADER_SIZE + 24;
    public static final int MAX_PAYLOAD = BUFFER_SIZE - HEADER_SIZE;

    public final long sent;
    public volatile long received;
    public volatile long confirmed;
    public final byte[] payload;

    public DataPacket (byte type, int packetIndex, int msgIndex, byte msgAttempt, byte msgPart, byte msgParts, byte[] payload) {
        super(type, packetIndex, msgIndex, msgAttempt, msgPart, msgParts);
        sent = System.nanoTime();
        received = 0;
        confirmed = 0;
        this.payload = payload;
    }

    public DataPacket(byte[] fromDataPacket) {
        super(fromDataPacket);
        ByteBuffer bb = ByteBuffer.wrap(fromDataPacket);
        bb.position(Packet.HEADER_SIZE);
        sent = bb.getLong();
        received = bb.getLong();
        confirmed = bb.getLong();
        payload = new byte[bb.limit() - bb.position()];
        bb.get(payload);
    }

    @Override
    public byte[] getBytes() {
        return getBuffer()
                .putLong(sent)
                .putLong(received)
                .putLong(confirmed)
                .put(payload)
                .array();
    }

    @Override
    public int size() {
        return HEADER_SIZE + payload.length;
    }
}
