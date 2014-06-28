package com.essentiallocalization.bluetooth.connection;

import java.nio.ByteBuffer;

/**
 * Created by Jake on 9/14/13.
 */
public final class AckPacket extends Packet {

    public long hciDestReceived;
    public long javaDestReceived;
    public long javaDestSent;

    private static final int HEADER_SIZE = (8 * 3) + (4 * 0) + (2 * 0) + (1 * 0); // see properties above

    AckPacket(byte[] ackPacket) {
        super(ackPacket);
        type = Packet.TYPE_ACK;

        ByteBuffer bb = ByteBuffer.wrap(ackPacket);
        bb.position(Packet.HEADER_SIZE + PREPEND.length());
        hciDestReceived = bb.getLong();
        javaDestReceived = bb.getLong();
        javaDestSent = bb.getLong();
    }

    @Override
    public byte[] getBytes(boolean sending) {
        return getBuffer(false)
                .putLong(hciDestReceived)
                .putLong(javaDestReceived)
                .putLong(sending ? javaDestSent = System.nanoTime()
                                 : javaDestSent)
                .array();
    }

    AckPacket() {
        type = Packet.TYPE_ACK;
    }

    AckPacket(Packet p) {
        super(p);
        type = Packet.TYPE_ACK;
    }

    @Override
    public int marginalHeaderSize() {
        return HEADER_SIZE;
    }

    @Override
    public int payloadSize() {
        return 0;
    }
}
